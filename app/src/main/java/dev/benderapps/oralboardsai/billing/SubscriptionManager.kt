package dev.benderapps.oralboardsai.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

private const val TAG = "SubscriptionManager"

/** A purchasable subscription, flattened from Play's ProductDetails. */
data class SubscriptionProduct(
    val productId: String,
    val displayPrice: String,
    val isAnnual: Boolean,
    internal val details: ProductDetails,
    internal val offerToken: String,
)

data class SubscriptionState(
    val isPro: Boolean = false,
    val products: List<SubscriptionProduct> = emptyList(),
    val isLoadingProducts: Boolean = false,
    val isPurchasing: Boolean = false,
    val isRestoringPurchases: Boolean = false,
    val errorMessage: String? = null,
)

/** Google Play Billing equivalent of the iOS StoreKit-backed SubscriptionManager. */
class SubscriptionManager(context: Context) {
    val proProductIds: List<String> = listOf(
        "dev.benderapps.oralboards.pro.monthly",
        "dev.benderapps.oralboards.pro.annual",
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    private var hasStarted = false

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        scope.launch { handlePurchasesUpdated(result, purchases.orEmpty()) }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    val isPro: Boolean
        get() = _state.value.isPro

    fun start() {
        if (hasStarted) return
        hasStarted = true
        scope.launch { refreshStoreState() }
    }

    suspend fun refreshStoreState() {
        if (!ensureConnected()) return
        loadProducts()
        refreshEntitlements()
    }

    suspend fun loadProducts() {
        if (_state.value.products.isNotEmpty()) return
        if (!ensureConnected()) return

        _state.value = _state.value.copy(isLoadingProducts = true)

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                proProductIds.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(ProductType.SUBS)
                        .build()
                }
            )
            .build()

        val result = billingClient.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingResponseCode.OK) {
            val products = result.productDetailsList.orEmpty().mapNotNull(::toSubscriptionProduct)
            _state.value = _state.value.copy(
                products = products,
                isLoadingProducts = false,
                errorMessage = null,
            )
        } else {
            Log.w(TAG, "queryProductDetails failed: ${result.billingResult.debugMessage}")
            _state.value = _state.value.copy(
                isLoadingProducts = false,
                errorMessage = result.billingResult.debugMessage.ifEmpty { null },
            )
        }
    }

    /** Starts Google Play's purchase sheet. The outcome arrives via the purchases listener. */
    fun purchase(activity: Activity, product: SubscriptionProduct) {
        if (_state.value.isPurchasing) return
        _state.value = _state.value.copy(isPurchasing = true, errorMessage = null)

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(product.details)
                        .setOfferToken(product.offerToken)
                        .build()
                )
            )
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingResponseCode.OK) {
            _state.value = _state.value.copy(
                isPurchasing = false,
                errorMessage = "Unable to start the purchase right now.",
            )
        }
    }

    suspend fun restorePurchases() {
        if (_state.value.isRestoringPurchases) return
        _state.value = _state.value.copy(isRestoringPurchases = true)

        refreshEntitlements()

        _state.value = _state.value.copy(
            isRestoringPurchases = false,
            errorMessage = if (_state.value.isPro) null else "No active subscription was found to restore.",
        )
    }

    suspend fun refreshEntitlements() {
        if (!ensureConnected()) return

        val result = billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build()
        )
        if (result.billingResult.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "queryPurchases failed: ${result.billingResult.debugMessage}")
            return
        }

        val activePurchases = result.purchasesList.filter(::isActiveProEntitlement)
        activePurchases.forEach { acknowledgeIfNeeded(it) }

        _state.value = _state.value.copy(
            isPro = activePurchases.isNotEmpty(),
            errorMessage = if (activePurchases.isNotEmpty()) null else _state.value.errorMessage,
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private suspend fun handlePurchasesUpdated(result: BillingResult, purchases: List<Purchase>) {
        when (result.responseCode) {
            BillingResponseCode.OK -> {
                purchases.forEach { purchase ->
                    if (purchase.purchaseState == Purchase.PurchaseState.PENDING) {
                        _state.value = _state.value.copy(errorMessage = "Your purchase is pending approval.")
                    } else if (isActiveProEntitlement(purchase)) {
                        acknowledgeIfNeeded(purchase)
                    }
                }
                refreshEntitlements()
                if (!_state.value.isPro && _state.value.errorMessage == null) {
                    _state.value = _state.value.copy(
                        errorMessage = "Subscription is not active yet. Please try Restore Purchases.",
                    )
                }
            }
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> refreshEntitlements()
            else -> _state.value = _state.value.copy(
                errorMessage = result.debugMessage.ifEmpty { "Unable to complete the purchase right now." },
            )
        }
        _state.value = _state.value.copy(isPurchasing = false)
    }

    private suspend fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val result = billingClient.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        )
        if (result.responseCode != BillingResponseCode.OK) {
            Log.w(TAG, "acknowledgePurchase failed: ${result.debugMessage}")
        }
    }

    private fun isActiveProEntitlement(purchase: Purchase): Boolean =
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
            purchase.products.any { it in proProductIds }

    private fun toSubscriptionProduct(details: ProductDetails): SubscriptionProduct? {
        val offer = details.subscriptionOfferDetails?.firstOrNull() ?: return null
        // The last pricing phase is the recurring price (earlier phases are trials/intro offers).
        val price = offer.pricingPhases.pricingPhaseList.lastOrNull()?.formattedPrice ?: return null
        val text = "${details.productId} ${details.name}".lowercase()
        return SubscriptionProduct(
            productId = details.productId,
            displayPrice = price,
            isAnnual = text.contains("annual") || text.contains("year"),
            details = details,
            offerToken = offer.offerToken,
        )
    }

    private suspend fun ensureConnected(): Boolean {
        if (billingClient.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingResponseCode.OK) {
                        Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                        _state.value = _state.value.copy(
                            errorMessage = "Google Play Billing is unavailable on this device.",
                        )
                    }
                    if (continuation.isActive) continuation.resume(result.responseCode == BillingResponseCode.OK)
                }

                override fun onBillingServiceDisconnected() {
                    // Auto service reconnection is enabled; the next call reconnects.
                }
            })
        }
    }
}

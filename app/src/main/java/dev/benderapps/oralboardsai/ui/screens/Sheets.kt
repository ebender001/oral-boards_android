package dev.benderapps.oralboardsai.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.billing.PaywallTrigger
import dev.benderapps.oralboardsai.billing.SubscriptionProduct
import dev.benderapps.oralboardsai.ui.components.OBCard
import dev.benderapps.oralboardsai.ui.components.StatusPill
import dev.benderapps.oralboardsai.ui.theme.BrandBlue
import dev.benderapps.oralboardsai.ui.theme.ConceptPurple
import dev.benderapps.oralboardsai.ui.theme.CriticalRed
import dev.benderapps.oralboardsai.ui.theme.DepthIndigo
import dev.benderapps.oralboardsai.ui.theme.SuccessGreen
import dev.benderapps.oralboardsai.ui.theme.WarningOrange
import dev.benderapps.oralboardsai.ui.theme.oralBoards
import kotlinx.coroutines.launch

private const val PRIVACY_URL = "https://oboardsai.com/privacy/"
private const val TERMS_URL = "https://oboardsai.com/terms/"
private const val DISCLAIMER_URL = "https://oboardsai.com/disclaimer/"
private const val SUPPORT_URL = "https://oboardsai.com/support/"
private const val PLAY_REDEEM_URL = "https://play.google.com/redeem"
private const val PLAY_SUBSCRIPTIONS_URL = "https://play.google.com/store/account/subscriptions"

internal fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

// MARK: - Settings

@Composable
fun SettingsSheet() {
    val context = LocalContext.current
    val subscriptionManager = context.appContainer.subscriptionManager
    val state by subscriptionManager.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier.size(50.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(24.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Manage your subscription and view legal information",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        OBCard(cornerRadius = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Star, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Subscription", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Oral Exam Prep Pro",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    StatusPill(
                        text = if (state.isPro) "Pro" else "Free",
                        tint = if (state.isPro) SuccessGreen else WarningOrange,
                    )
                }

                if (state.isPro) {
                    SettingsActionRow(
                        title = "Manage Subscription",
                        subtitle = "Change or cancel in Google Play",
                        icon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { uriHandler.openUri(PLAY_SUBSCRIPTIONS_URL) },
                    )
                }
                SettingsActionRow(
                    title = if (state.isRestoringPurchases) "Restoring…" else "Restore Purchases",
                    subtitle = "Re-check your Google Play subscription",
                    icon = Icons.Rounded.Sync,
                    onClick = { scope.launch { subscriptionManager.restorePurchases() } },
                )
            }
        }

        OBCard(cornerRadius = 20.dp) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                LinkRow("Privacy Policy", PRIVACY_URL)
                HorizontalDivider()
                LinkRow("Terms of Use", TERMS_URL)
                HorizontalDivider()
                LinkRow("Educational Use Disclaimer", DISCLAIMER_URL)
                HorizontalDivider()
                LinkRow("Support", SUPPORT_URL)
            }
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(15.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(34.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LinkRow(title: String, url: String) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { uriHandler.openUri(url) }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

// MARK: - Paywall

@Composable
fun PaywallSheet(reason: PaywallTrigger, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val subscriptionManager = context.appContainer.subscriptionManager
    val state by subscriptionManager.state.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { subscriptionManager.refreshStoreState() }
    LaunchedEffect(state.isPro) { if (state.isPro) onDismiss() }

    val sortedProducts = state.products.sortedBy { if (it.isAnnual) 0 else 1 }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(top = 4.dp, bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                reason.title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "Practice realistic oral board cases with full feedback, unlimited access, and voice-based simulation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.oralBoards.card)
                .padding(vertical = 4.dp),
        ) {
            BenefitRow(Icons.AutoMirrored.Rounded.VolumeUp, "Voice Examiner", "Hear cases and follow-up questions like a real oral exam.")
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            BenefitRow(Icons.Rounded.Mic, "Dictated Responses", "Answer out loud and practice speaking under pressure.")
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            BenefitRow(Icons.Rounded.AllInclusive, "Unlimited Practice", "Continue cases after your free case limit is reached.")
            HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
            BenefitRow(Icons.Rounded.Assessment, "Detailed Feedback", "Review performance, missed points, and critical errors.")
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when {
                state.isPro -> StatusCard {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen)
                    Text(
                        "Premium access is already active on this device.",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                state.isLoadingProducts -> StatusCard {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Text(
                        "Loading subscription options...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                sortedProducts.isEmpty() -> StatusCard {
                    Text(
                        "Subscription options are unavailable. Please try again in a moment.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> sortedProducts.forEach { product ->
                    ProductButton(
                        product = product,
                        enabled = !state.isPurchasing,
                        onClick = {
                            context.findActivity()?.let { subscriptionManager.purchase(it, product) }
                        },
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            val errorMessage = state.errorMessage
            if (!errorMessage.isNullOrEmpty()) {
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = CriticalRed,
                    textAlign = TextAlign.Center,
                )
            }

            PaywallSecondaryButton(
                text = if (state.isRestoringPurchases) "Restoring..." else "Restore Purchases",
                enabled = !state.isPurchasing && !state.isRestoringPurchases,
                onClick = { scope.launch { subscriptionManager.restorePurchases() } },
            )
            PaywallSecondaryButton(
                text = "Redeem Promo Code",
                enabled = !state.isPurchasing,
                onClick = { uriHandler.openUri(PLAY_REDEEM_URL) },
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Cancel anytime",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Subscriptions renew automatically until canceled in Google Play.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { uriHandler.openUri(PRIVACY_URL) }) { Text("Privacy Policy") }
                    Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { uriHandler.openUri(TERMS_URL) }) { Text("Terms of Use") }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(content: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.oralBoards.card)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        content()
    }
}

@Composable
private fun BenefitRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(42.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(22.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductButton(product: SubscriptionProduct, enabled: Boolean, onClick: () -> Unit) {
    val isAnnual = product.isAnnual
    val shape = RoundedCornerShape(20.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isAnnual) BrandBlue.copy(alpha = 0.08f) else MaterialTheme.oralBoards.cardInset)
            .border(
                BorderStroke(
                    if (isAnnual) 2.dp else 1.dp,
                    if (isAnnual) BrandBlue.copy(alpha = 0.95f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
                ),
                shape,
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (isAnnual) 18.dp else 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (isAnnual) "Annual Pro" else "Monthly Pro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (isAnnual) BestValueBadge()
            }
            Text(
                if (isAnnual) "Unlock unlimited access for a full year." else "Unlock unlimited access for a full month.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (isAnnual) {
                Text(
                    "Best for serious prep",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandBlue.copy(alpha = 0.9f),
                )
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                product.displayPrice,
                style = if (isAnnual) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                fontWeight = if (isAnnual) FontWeight.Bold else FontWeight.SemiBold,
                color = if (isAnnual) BrandBlue else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                if (isAnnual) "/ year" else "/ month",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BestValueBadge() {
    Text(
        "BEST VALUE",
        modifier = Modifier
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFFFCE073), Color(0xFFF2C238))))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF6B470A),
    )
}

@Composable
private fun PaywallSecondaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - Exam parameters

@Composable
fun ExamParametersSheet(
    requiredMustCoverPoints: Int,
    allowedMajorErrors: Int,
    allowedMinorErrors: Int,
    maxTurns: Int,
    onRequiredMustCoverPointsChange: (Int) -> Unit,
    onAllowedMajorErrorsChange: (Int) -> Unit,
    onAllowedMinorErrorsChange: (Int) -> Unit,
    onMaxTurnsChange: (Int) -> Unit,
    onRestoreStandard: () -> Unit,
    onDone: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Exam Parameters",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onDone) { Text("Done", fontWeight = FontWeight.SemiBold) }
        }

        ParameterControlRow(
            title = "Critical Concepts",
            subtitle = "Essential management steps you must address",
            tint = BrandBlue,
            icon = Icons.Rounded.CheckCircle,
            value = requiredMustCoverPoints,
            range = 0..20,
            onValueChange = onRequiredMustCoverPointsChange,
        )
        ParameterControlRow(
            title = "Critical Failures",
            subtitle = "Unsafe decisions that may result in failure",
            tint = CriticalRed,
            icon = Icons.Rounded.Warning,
            value = allowedMajorErrors,
            range = 0..10,
            onValueChange = onAllowedMajorErrorsChange,
        )
        ParameterControlRow(
            title = "Recoverable Errors",
            subtitle = "Suboptimal decisions that lower performance but may not cause failure",
            tint = WarningOrange,
            icon = Icons.Rounded.ErrorOutline,
            value = allowedMinorErrors,
            range = 0..20,
            onValueChange = onAllowedMinorErrorsChange,
        )
        ParameterControlRow(
            title = "Case Depth",
            subtitle = "Maximum candidate response turns",
            tint = DepthIndigo,
            icon = Icons.Rounded.Sync,
            value = maxTurns,
            range = 1..12,
            onValueChange = onMaxTurnsChange,
        )

        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(MaterialTheme.oralBoards.cardInset)
                .clickable(onClick = onRestoreStandard)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.RestartAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                "Restore Standard Settings",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ParameterControlRow(
    title: String,
    subtitle: String,
    tint: Color,
    icon: ImageVector,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.oralBoards.card)
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.08f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepperButton(Icons.Rounded.Remove, "Decrease $title", tint, value > range.first) {
                onValueChange(value - 1)
            }
            Text(
                "$value",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            StepperButton(Icons.Rounded.Add, "Increase $title", tint, value < range.last) {
                onValueChange(value + 1)
            }
        }
    }
}

@Composable
private fun StepperButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = if (enabled) 0.08f else 0.03f))
            .border(BorderStroke(1.dp, tint.copy(alpha = if (enabled) 0.35f else 0.14f)), CircleShape),
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = if (enabled) tint.copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp),
        )
    }
}

// MARK: - How scoring works

@Composable
fun ParametersLearnMoreSheet(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "How Scoring Works",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            TextButton(onClick = onDismiss) { Text("Close") }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(SuccessGreen.copy(alpha = 0.055f))
                .border(BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.14f)), RoundedCornerShape(18.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                }
                Text("What Examiners Want", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                listOf(
                    "State a clear diagnosis early when possible",
                    "Transition to operative management once indicated",
                    "Be structured, decisive, and avoid unsafe actions",
                ).forEach { line ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", style = MaterialTheme.typography.titleMedium)
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }

        HorizontalDivider()

        Text("Scoring Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        ScoringCategory(
            tint = SuccessGreen,
            title = "Critical Concepts",
            description = "Essential management steps you must address during the case.",
        )
        ScoringCategory(
            tint = CriticalRed,
            title = "Critical Failures",
            description = "Unsafe decisions that may result in failure.",
        )
        ScoringCategory(
            tint = WarningOrange,
            title = "Recoverable Errors",
            description = "Suboptimal decisions that lower performance but may not cause failure.",
        )
        ScoringCategory(
            tint = ConceptPurple,
            title = "Case Depth",
            description = "Maximum candidate response turns. The case may end earlier if performance is clearly sufficient or insufficient.",
        )
        Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun ScoringCategory(tint: Color, title: String, description: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(tint))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
        }
    }
}

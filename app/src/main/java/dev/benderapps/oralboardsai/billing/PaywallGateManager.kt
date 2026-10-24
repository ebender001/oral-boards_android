package dev.benderapps.oralboardsai.billing

import dev.benderapps.oralboardsai.data.AppPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class PaywallTrigger(val title: String) {
    SpeechMode("Speech Is Pro"),
    CaseCompletionLimit("Free Case Limit Reached"),
}

/**
 * Decides when to show the paywall. Free users get [FREE_CASE_LIMIT] completed
 * cases and no speech modes.
 */
class PaywallGateManager(
    private val preferences: AppPreferences,
    private val subscriptionManager: SubscriptionManager,
) {
    private val _activeReason = MutableStateFlow<PaywallTrigger?>(null)
    val activeReason: StateFlow<PaywallTrigger?> = _activeReason.asStateFlow()

    val hasPremiumAccess: Boolean
        get() = subscriptionManager.isPro

    val hasReachedCompletionLimit: Boolean
        get() = !hasPremiumAccess && preferences.completedCaseCount >= FREE_CASE_LIMIT

    fun requestSpeechPaywall() {
        if (hasPremiumAccess) return
        _activeReason.value = PaywallTrigger.SpeechMode
    }

    fun recordCompletedCase() {
        preferences.completedCaseCount += 1
    }

    fun requestCompletionLimitPaywall() {
        if (hasPremiumAccess) return
        _activeReason.value = PaywallTrigger.CaseCompletionLimit
    }

    fun dismissPaywall() {
        _activeReason.value = null
    }

    private companion object {
        const val FREE_CASE_LIMIT = 2
    }
}

package dev.benderapps.oralboardsai

import android.app.Application
import dev.benderapps.oralboardsai.billing.PaywallGateManager
import dev.benderapps.oralboardsai.billing.SubscriptionManager
import dev.benderapps.oralboardsai.speech.EarconMuter
import dev.benderapps.oralboardsai.data.AppPreferences
import dev.benderapps.oralboardsai.data.ExaminerTTSService
import dev.benderapps.oralboardsai.data.OralExamService
import dev.benderapps.oralboardsai.data.ParseClient
import dev.benderapps.oralboardsai.data.PerformanceStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Hand-rolled dependency container; the app is small enough not to need a DI framework. */
class AppContainer(application: Application) {
    val preferences = AppPreferences(application)
    val examService = OralExamService(preferences)
    val ttsService = ExaminerTTSService()
    val performanceStore = PerformanceStore(application)
    val subscriptionManager = SubscriptionManager(application)
    val paywallGateManager = PaywallGateManager(preferences, subscriptionManager)
}

class OralBoardsApplication : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()

        // Parse configuration is injected from local.properties at build time, so
        // fail fast if that wiring is missing.
        check(ParseClient.isConfigured) {
            "Missing Parse configuration. Add PARSE_APP_ID, PARSE_CLIENT_KEY and " +
                "PARSE_SERVER_URL to local.properties (see local.properties.template)."
        }

        // Undo any dictation mute left behind if the app was killed mid-dictation.
        EarconMuter(this).restore()

        container = AppContainer(this)
        container.subscriptionManager.start()

        appScope.launch {
            container.performanceStore.load()
        }
        // Pre-warm Render for the slow start of the first case.
        appScope.launch(Dispatchers.IO) {
            container.ttsService.warmUp()
        }
    }
}

val android.content.Context.appContainer: AppContainer
    get() = (applicationContext as OralBoardsApplication).container

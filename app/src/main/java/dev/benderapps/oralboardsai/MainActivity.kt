package dev.benderapps.oralboardsai

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.benderapps.oralboardsai.data.Specialty
import dev.benderapps.oralboardsai.ui.OralCaseViewModel
import dev.benderapps.oralboardsai.ui.screens.CaseScreen
import dev.benderapps.oralboardsai.ui.screens.DebriefScreen
import dev.benderapps.oralboardsai.ui.screens.HistoricalCaseReviewScreen
import dev.benderapps.oralboardsai.ui.screens.IntroScreen
import dev.benderapps.oralboardsai.ui.screens.PerformanceDashboardScreen
import dev.benderapps.oralboardsai.ui.theme.OralBoardsAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OralBoardsAITheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    OralBoardsNavHost()
                }
            }
        }
    }
}

private object Routes {
    const val Intro = "intro"
    const val CasePattern = "case/{specialty}"
    const val Debrief = "debrief"
    const val Progress = "progress"
    const val ReviewPattern = "review/{sessionId}"

    fun case(specialty: Specialty) = "case/${specialty.raw}"
    fun review(sessionId: String) = "review/$sessionId"
}

@Composable
private fun OralBoardsNavHost() {
    val navController = rememberNavController()
    val application = LocalContext.current.applicationContext as Application

    NavHost(navController = navController, startDestination = Routes.Intro) {
        composable(Routes.Intro) {
            IntroScreen(
                onStartCase = { specialty -> navController.navigate(Routes.case(specialty)) },
                onOpenProgress = { navController.navigate(Routes.Progress) },
            )
        }

        composable(Routes.CasePattern) { entry ->
            val viewModel = caseViewModel(entry, application)
            CaseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onShowDebrief = { navController.navigate(Routes.Debrief) },
            )
        }

        composable(Routes.Debrief) { entry ->
            // The debrief shares the case's view model: it needs the live turns and error lists.
            val caseEntry = remember(entry) { navController.getBackStackEntry(Routes.CasePattern) }
            val viewModel = caseViewModel(caseEntry, application)
            DebriefScreen(
                viewModel = viewModel,
                onClose = {
                    viewModel.onDebriefDismissed()
                    navController.popBackStack()
                },
                onOpenProgress = { navController.navigate(Routes.Progress) },
            )
        }

        composable(Routes.Progress) {
            PerformanceDashboardScreen(
                onBack = { navController.popBackStack() },
                onOpenCase = { sessionId -> navController.navigate(Routes.review(sessionId)) },
            )
        }

        composable(Routes.ReviewPattern) { entry ->
            HistoricalCaseReviewScreen(
                sessionId = entry.arguments?.getString("sessionId").orEmpty(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}

@Composable
private fun caseViewModel(entry: NavBackStackEntry, application: Application): OralCaseViewModel {
    val specialty = Specialty.fromRaw(entry.arguments?.getString("specialty"))
        ?: Specialty.CardiothoracicSurgery
    return viewModel(
        viewModelStoreOwner = entry,
        factory = OralCaseViewModel.Factory(application, specialty),
    )
}

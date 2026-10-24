package dev.benderapps.oralboardsai.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.data.InteractionMode
import dev.benderapps.oralboardsai.ui.OralCaseViewModel
import dev.benderapps.oralboardsai.ui.reportAiContent
import dev.benderapps.oralboardsai.ui.components.BeginCaseButton
import dev.benderapps.oralboardsai.ui.components.DebriefPreparationCard
import dev.benderapps.oralboardsai.ui.components.ExamSetupCard
import dev.benderapps.oralboardsai.ui.components.ExaminerCard
import dev.benderapps.oralboardsai.ui.components.ResponseCard
import dev.benderapps.oralboardsai.ui.components.ResponseRecordedCard
import dev.benderapps.oralboardsai.ui.components.TurnProgressSection
import dev.benderapps.oralboardsai.ui.components.WaitingForExaminerCard
import dev.benderapps.oralboardsai.ui.components.turnProgressTint
import dev.benderapps.oralboardsai.ui.theme.oralBoards
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaseScreen(
    viewModel: OralCaseViewModel,
    onBack: () -> Unit,
    onShowDebrief: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val paywallReason by container.paywallGateManager.activeReason.collectAsState()
    val playback = viewModel.playback
    val speech = viewModel.speechRecognition

    var showExamParametersSheet by remember { mutableStateOf(false) }
    var showParametersLearnMore by remember { mutableStateOf(false) }
    var speechAlertMessage by remember { mutableStateOf<String?>(null) }

    // Case-flow events (e.g. the debrief is ready) come from the view model.
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OralCaseViewModel.Event.ShowDebrief -> onShowDebrief()
            }
        }
    }

    // Playback and dictation never continue in the background.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                playback.stop()
                speech.stop()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    fun exitCase() {
        viewModel.resetViewState()
        scope.launch {
            viewModel.abortCase()
            onBack()
        }
    }

    fun abortAndResetCase() {
        viewModel.resetViewState()
        scope.launch {
            viewModel.abortCase()
            scrollState.animateScrollTo(0)
        }
    }

    BackHandler(onBack = ::exitCase)

    // After the examiner finishes speaking, bring the response card into view.
    var wasSpeaking by remember { mutableStateOf(false) }
    LaunchedEffect(playback.isSpeaking) {
        val finishedSpeaking = wasSpeaking && !playback.isSpeaking && !playback.isPaused
        wasSpeaking = playback.isSpeaking
        if (finishedSpeaking &&
            viewModel.examinerMode == InteractionMode.Speech &&
            viewModel.hasStartedCase &&
            !viewModel.isPreparingDebrief
        ) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val safeMaxTurns = maxOf(if (viewModel.maxTurns > 0) viewModel.maxTurns else 8, 1)
    val pendingTurn = if (viewModel.caseCompleted) viewModel.turns.size else viewModel.turns.size + 1
    val currentTurn = pendingTurn.coerceIn(1, safeMaxTurns)
    val turnProgress = currentTurn.toFloat() / safeMaxTurns.toFloat()

    val shouldShowExamParameterControls = !viewModel.hasStartedCase || viewModel.caseCompleted

    Scaffold(
        containerColor = MaterialTheme.oralBoards.grouped,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(if (playback.hasPlayedExaminerInquiry) "Case in Progress" else "Start Case")
                },
                navigationIcon = {
                    IconButton(onClick = ::exitCase) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = ::abortAndResetCase, enabled = viewModel.hasStartedCase) {
                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Reset this case")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.oralBoards.grouped,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            ExamSetupCard(
                examinerMode = viewModel.examinerMode,
                userMode = viewModel.userMode,
                requiredMustCoverPoints = viewModel.requiredMustCoverPoints,
                allowedMajorErrors = viewModel.allowedMajorErrors,
                allowedMinorErrors = viewModel.allowedMinorErrors,
                maxTurns = viewModel.maxTurns,
                shouldShowExamParameterControls = shouldShowExamParameterControls,
                onSelectExaminerMode = viewModel::selectExaminerMode,
                onSelectUserMode = viewModel::selectUserMode,
                onShowExamParameters = { showExamParametersSheet = true },
                onShowParametersLearnMore = { showParametersLearnMore = true },
            )

            if (!playback.hasPlayedExaminerInquiry) {
                BeginCaseButton(
                    isLoading = viewModel.isStartingCase || playback.isLoadingExaminerAudio,
                    onClick = viewModel::beginCase,
                )
            }

            ExaminerCard(
                examinerMode = viewModel.examinerMode,
                caseStem = viewModel.caseStem,
                currentExaminerPrompt = viewModel.currentExaminerPrompt,
                hasPlayedExaminerInquiry = playback.hasPlayedExaminerInquiry,
                isStartingCase = viewModel.isStartingCase || playback.isLoadingExaminerAudio,
                isWaitingForNextQuestion = viewModel.examinerMode == InteractionMode.Text && viewModel.isSubmitting,
                isPlaybackActive = playback.isSpeaking,
                isPlaybackPaused = playback.isPaused,
                onRepeat = viewModel::repeatLastInquiry,
                onTogglePlayback = playback::togglePlayback,
                onReport = {
                    context.reportAiContent(
                        section = "Examiner",
                        caseTitle = viewModel.caseTitle,
                        caseId = viewModel.caseId,
                        turnNumber = currentTurn,
                        content = viewModel.currentExaminerPrompt,
                    )
                },
            )

            if (viewModel.hasStartedCase) {
                TurnProgressSection(
                    currentTurn = currentTurn,
                    maxTurns = safeMaxTurns,
                    progress = turnProgress,
                    tint = turnProgressTint(turnProgress),
                )

                if (viewModel.isSubmitting || viewModel.isRetryingSubmission) {
                    WaitingForExaminerCard(isRetrying = viewModel.isRetryingSubmission)
                } else if (viewModel.turns.isNotEmpty() &&
                    !viewModel.caseCompleted &&
                    !viewModel.isPreparingDebrief &&
                    viewModel.debriefPreparationError == null
                ) {
                    ResponseRecordedCard()
                }

                if (viewModel.isPreparingDebrief || viewModel.debriefPreparationError != null) {
                    DebriefPreparationCard(
                        isPreparingDebrief = viewModel.isPreparingDebrief,
                        isRetryingDebrief = viewModel.isRetryingDebrief,
                        errorMessage = viewModel.debriefPreparationError,
                        onTryAgain = viewModel::prepareDebrief,
                        onNewCase = {
                            viewModel.resetCompletedCaseAfterDebriefFailure()
                            scope.launch { scrollState.animateScrollTo(0) }
                        },
                    )
                } else {
                    val isDictationButtonDisabled = when {
                        viewModel.userMode != InteractionMode.Speech -> true
                        !playback.hasPlayedExaminerInquiry -> true
                        viewModel.isSubmitting -> true
                        viewModel.examinerMode == InteractionMode.Speech ->
                            playback.isLoadingExaminerAudio || playback.isSpeaking || playback.isPaused
                        else -> false
                    }
                    val draftIsBlank = viewModel.candidateResponse.isBlank()

                    ResponseCard(
                        userMode = viewModel.userMode,
                        candidateResponse = viewModel.candidateResponse,
                        onCandidateResponseChange = { viewModel.candidateResponse = it },
                        isRecordingResponse = speech.isRecordingResponse,
                        isSpeechActivityDetected = speech.isDetectingSpeech,
                        dictationButtonDisabled = isDictationButtonDisabled,
                        isSubmitting = viewModel.isSubmitting,
                        submissionRetryError = viewModel.submissionRetryError,
                        resetDisabled = draftIsBlank && speech.liveTranscript.isBlank() && !speech.isRecordingResponse,
                        submitDisabled = draftIsBlank || viewModel.isSubmitting,
                        onToggleSpeechRecognition = {
                            speech.toggle(
                                candidateResponse = { viewModel.candidateResponse },
                                setCandidateResponse = { viewModel.candidateResponse = it },
                                stopExaminerPlayback = playback::stop,
                                presentError = { speechAlertMessage = it },
                            )
                        },
                        onTranscriptBeginEditing = speech::beginManualEditing,
                        onSubmit = viewModel::submitCandidateResponse,
                        onRetrySubmission = viewModel::submitCandidateResponse,
                        onReset = viewModel::resetResponseAndTranscript,
                    )
                }
            }
        }
    }

    // Sheets
    if (showExamParametersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showExamParametersSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            ExamParametersSheet(
                requiredMustCoverPoints = viewModel.requiredMustCoverPoints,
                allowedMajorErrors = viewModel.allowedMajorErrors,
                allowedMinorErrors = viewModel.allowedMinorErrors,
                maxTurns = viewModel.maxTurns,
                onRequiredMustCoverPointsChange = { viewModel.requiredMustCoverPoints = it },
                onAllowedMajorErrorsChange = { viewModel.allowedMajorErrors = it },
                onAllowedMinorErrorsChange = { viewModel.allowedMinorErrors = it },
                onMaxTurnsChange = { viewModel.maxTurns = it },
                onRestoreStandard = viewModel::restoreStandardSettings,
                onDone = { showExamParametersSheet = false },
            )
        }
    }

    if (showParametersLearnMore) {
        ModalBottomSheet(
            onDismissRequest = { showParametersLearnMore = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            ParametersLearnMoreSheet(onDismiss = { showParametersLearnMore = false })
        }
    }

    paywallReason?.let { reason ->
        ModalBottomSheet(
            onDismissRequest = container.paywallGateManager::dismissPaywall,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            PaywallSheet(reason = reason, onDismiss = container.paywallGateManager::dismissPaywall)
        }
    }

    // Alerts
    speechAlertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { speechAlertMessage = null },
            title = { Text("Speech to Text Unavailable") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { speechAlertMessage = null }) { Text("OK") } },
        )
    }

    val startAlertMessage = viewModel.startCaseAlertMessage
    if (startAlertMessage != null) {
        AlertDialog(
            onDismissRequest = {
                viewModel.startCaseAlertTitle = null
                viewModel.startCaseAlertMessage = null
            },
            title = { Text(viewModel.startCaseAlertTitle ?: "Unable to Start Case") },
            text = { Text(startAlertMessage) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.startCaseAlertTitle = null
                    viewModel.startCaseAlertMessage = null
                }) { Text("OK") }
            },
        )
    }

    if (viewModel.showNoInternetAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.showNoInternetAlert = false },
            title = { Text("No Internet Connection") },
            text = {
                Text("An internet connection is required to start a case. Please check your connection and try again.")
            },
            confirmButton = { TextButton(onClick = { viewModel.showNoInternetAlert = false }) { Text("OK") } },
        )
    }
}

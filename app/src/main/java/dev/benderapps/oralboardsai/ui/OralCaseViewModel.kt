package dev.benderapps.oralboardsai.ui

import android.app.Application
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.data.CaseTurn
import dev.benderapps.oralboardsai.data.ExaminerTTSService
import dev.benderapps.oralboardsai.data.InteractionMode
import dev.benderapps.oralboardsai.data.NetworkMonitor
import dev.benderapps.oralboardsai.data.OralCaseDebriefResponse
import dev.benderapps.oralboardsai.data.ParseException
import dev.benderapps.oralboardsai.data.Specialty
import dev.benderapps.oralboardsai.data.StartCaseResponse
import dev.benderapps.oralboardsai.data.SubmitResponseResult
import dev.benderapps.oralboardsai.data.isTransientRequestFailure
import dev.benderapps.oralboardsai.speech.ExaminerPlaybackController
import dev.benderapps.oralboardsai.speech.SpeechRecognitionController
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

private const val TAG = "OralCaseViewModel"
private const val RETRY_DELAY_MILLIS = 1_000L
private const val SUBMISSION_FAILURE_MESSAGE =
    "Your response was not submitted. Please check your connection and try again."
private const val DEBRIEF_FAILURE_MESSAGE = "Please check your connection and try again."
private const val START_CASE_FAILURE_MESSAGE = "Please check your internet connection and try again."

/**
 * Owns server session state and the exam progress that must survive across the
 * case screen and debrief screen.
 */
class OralCaseViewModel(
    application: Application,
    private val specialty: Specialty,
) : AndroidViewModel(application) {

    private val container = application.appContainer
    private val service = container.examService
    private val ttsService = container.ttsService

    val playback = ExaminerPlaybackController(application, viewModelScope)
    val speechRecognition = SpeechRecognitionController(application)

    private val gate = container.paywallGateManager
    private val subscriptionManager = container.subscriptionManager
    private val networkMonitor = NetworkMonitor(application)

    private var sessionId: String? = null
    private var hasLoggedCaseCompleted = false

    private enum class SpeechTarget { Examiner, Response }
    private var pendingSpeechTarget: SpeechTarget? = null

    sealed interface Event {
        data object ShowDebrief : Event
    }

    private val eventChannel = Channel<Event>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    var examinerMode by mutableStateOf(InteractionMode.Text)
        private set
    var userMode by mutableStateOf(InteractionMode.Text)
        private set

    var isPreparingDebrief by mutableStateOf(false)
        private set
    var debriefPreparationError by mutableStateOf<String?>(null)
        private set
    var showNoInternetAlert by mutableStateOf(false)

    // Exam parameters (editable before the case starts).
    var allowedMajorErrors by mutableIntStateOf(1)
    var allowedMinorErrors by mutableIntStateOf(2)
    var maxTurns by mutableIntStateOf(8)
    var requiredMustCoverPoints by mutableIntStateOf(3)

    var isSubmitting by mutableStateOf(false)
        private set
    var isStartingCase by mutableStateOf(false)
        private set
    var isRetryingSubmission by mutableStateOf(false)
        private set
    var isRetryingDebrief by mutableStateOf(false)
        private set

    var candidateResponse by mutableStateOf("")
    var turns by mutableStateOf<List<CaseTurn>>(emptyList())
        private set
    var currentExaminerPrompt by mutableStateOf("")
        private set
    var caseCompleted by mutableStateOf(false)
        private set

    /** Bumped each time the server returns a new prompt that should be spoken. */
    var examinerPromptPlaybackToken by mutableIntStateOf(0)
        private set

    var caseStem by mutableStateOf("")
        private set
    var caseTitle by mutableStateOf("")
        private set
    var caseId by mutableStateOf("")
        private set
    var completionReason by mutableStateOf("")
        private set
    var coveredPoints by mutableStateOf<List<String>>(emptyList())
        private set
    var majorErrors by mutableStateOf<List<String>>(emptyList())
        private set
    var minorErrors by mutableStateOf<List<String>>(emptyList())
        private set

    var debrief by mutableStateOf<OralCaseDebriefResponse?>(null)
        private set
    var debriefLoadError by mutableStateOf<String?>(null)
        private set
    var submissionRetryError by mutableStateOf<String?>(null)
        private set

    var startCaseAlertTitle by mutableStateOf<String?>(null)
    var startCaseAlertMessage by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            subscriptionManager.state.map { it.isPro }.distinctUntilChanged().collect { isPro ->
                if (isPro) {
                    applySubscribedDefaultModesIfNeeded()
                    applyPendingSpeechSelection()
                    gate.dismissPaywall()
                }
            }
        }
        viewModelScope.launch {
            gate.activeReason.collect { reason ->
                // Dismissing the paywall without subscribing abandons the pending speech choice.
                if (reason == null && !gate.hasPremiumAccess) pendingSpeechTarget = null
            }
        }
    }

    val hasStartedCase: Boolean
        get() = sessionId != null

    val specialtyDisplayName: String
        get() = specialty.displayName

    suspend fun submitResponse(response: String): Boolean {
        val trimmed = response.trim()
        if (trimmed.isEmpty()) return false
        if (isSubmitting) return false

        isSubmitting = true
        isRetryingSubmission = false
        submissionRetryError = null

        try {
            val activeSessionId = sessionId
            if (activeSessionId == null) {
                Log.w(TAG, "submitResponse called before sessionId was available")
                submissionRetryError = SUBMISSION_FAILURE_MESSAGE
                return false
            }

            val previousMajorErrors = majorErrors.toSet()
            val previousMinorErrors = minorErrors.toSet()
            val submittedTurnIndex = turns.size

            val result: SubmitResponseResult = try {
                service.submitResponse(activeSessionId, trimmed)
            } catch (e: Exception) {
                if (!e.isTransientRequestFailure()) {
                    Log.w(TAG, "Submit failed", e)
                    submissionRetryError = SUBMISSION_FAILURE_MESSAGE
                    return false
                }

                isRetryingSubmission = true
                delay(RETRY_DELAY_MILLIS)

                try {
                    service.submitResponse(activeSessionId, trimmed)
                } catch (retryError: Exception) {
                    Log.w(TAG, "Submit retry failed", retryError)
                    submissionRetryError = SUBMISSION_FAILURE_MESSAGE
                    return false
                }
            }

            val turn = CaseTurn(
                turnIndex = submittedTurnIndex,
                examinerPrompt = currentExaminerPrompt,
                candidateResponse = trimmed,
                newMajorErrors = result.majorErrors.filter { it !in previousMajorErrors },
                newMinorErrors = result.minorErrors.filter { it !in previousMinorErrors },
                missedConcepts = result.missedConcepts,
                examinerWasLookingFor = result.examinerWasLookingFor,
            )
            turns += turn

            val nextPrompt = result.nextExaminerPrompt.trim()
            val hasFollowUpPrompt = nextPrompt.isNotEmpty()
            val exceededMajorErrors = result.majorErrors.size > allowedMajorErrors
            val exceededMinorErrors = result.minorErrors.size > allowedMinorErrors
            val reachedMaxTurns = turns.size >= maxTurns

            // The backend may occasionally mark a case complete while still
            // returning a usable follow-up prompt. The client keeps the case
            // alive until a real stop condition is reached locally.
            val shouldContinueCase = hasFollowUpPrompt &&
                !exceededMajorErrors &&
                !exceededMinorErrors &&
                !reachedMaxTurns

            caseCompleted = result.isCaseComplete && !shouldContinueCase
            completionReason = result.completionReason.orEmpty()
            coveredPoints = result.coveredPoints
            majorErrors = result.majorErrors
            minorErrors = result.minorErrors
            candidateResponse = ""
            submissionRetryError = null

            if (shouldContinueCase || (!result.isCaseComplete && hasFollowUpPrompt)) {
                currentExaminerPrompt = nextPrompt
                examinerPromptPlaybackToken += 1

                // Follow-up prompts auto-play only after the user has explicitly
                // started the case once.
                if (examinerMode == InteractionMode.Speech && playback.hasPlayedExaminerInquiry) {
                    playFollowUpPrompt()
                }
            }

            if (caseCompleted && !hasLoggedCaseCompleted) {
                hasLoggedCaseCompleted = true
                gate.recordCompletedCase()
                logPerformance()
                prepareDebrief()
            }

            return true
        } finally {
            isSubmitting = false
            isRetryingSubmission = false
        }
    }

    suspend fun startCase(caseIdOverride: String? = null): Boolean {
        if (isStartingCase) return false

        isStartingCase = true
        try {
            startCaseAlertTitle = null
            startCaseAlertMessage = null

            val result: StartCaseResponse = try {
                requestStartCase(caseIdOverride)
            } catch (e: Exception) {
                if (!e.isTransientRequestFailure()) {
                    Log.w(TAG, "Failed to start case", e)
                    presentStartCaseError(e)
                    return false
                }

                delay(RETRY_DELAY_MILLIS)

                try {
                    requestStartCase(caseIdOverride)
                } catch (retryError: Exception) {
                    Log.w(TAG, "Failed to start case after retry", retryError)
                    startCaseAlertTitle = "Unable to Start Case"
                    startCaseAlertMessage = START_CASE_FAILURE_MESSAGE
                    return false
                }
            }

            sessionId = result.sessionId
            caseId = result.caseId
            caseStem = result.caseStem
            caseTitle = result.caseTitle
            maxTurns = result.maxTurns
            completionReason = ""
            coveredPoints = emptyList()
            majorErrors = emptyList()
            minorErrors = emptyList()
            debrief = null
            debriefLoadError = null
            caseCompleted = false
            hasLoggedCaseCompleted = false
            turns = emptyList()
            candidateResponse = ""
            currentExaminerPrompt = result.examinerPrompt
            submissionRetryError = null
            isRetryingSubmission = false
            isRetryingDebrief = false
            return true
        } finally {
            isStartingCase = false
        }
    }

    // Start case always sends the selected specialty so the backend can
    // randomize within the requested specialty only.
    private suspend fun requestStartCase(caseIdOverride: String?): StartCaseResponse =
        service.startCase(
            specialty = specialty.backendSpecialty,
            caseDomain = specialty.backendCaseDomain,
            caseId = caseIdOverride,
            requiredMustCoverPoints = requiredMustCoverPoints,
            allowedMajorErrors = allowedMajorErrors,
            allowedMinorErrors = allowedMinorErrors,
            maxTurns = maxTurns,
        )

    private fun presentStartCaseError(error: Exception) {
        val message = error.message.orEmpty().lowercase()
        val isNoMatchingSpecialtyCase = error is ParseException && (
            error.code == ParseException.OBJECT_NOT_FOUND ||
                message.contains("does not match requested specialty") ||
                message.contains("no cases")
            )

        if (isNoMatchingSpecialtyCase) {
            startCaseAlertTitle = "No Cases Available"
            startCaseAlertMessage = "No cases are currently available for ${specialty.displayName}."
        } else {
            startCaseAlertTitle = "Unable to Start Case"
            startCaseAlertMessage = START_CASE_FAILURE_MESSAGE
        }
    }

    suspend fun loadDebrief() {
        val activeSessionId = sessionId
        if (activeSessionId == null) {
            debriefLoadError = "Missing session ID"
            return
        }

        isRetryingDebrief = false
        debriefLoadError = null

        try {
            debrief = service.getDebrief(activeSessionId)
            debriefLoadError = null
        } catch (e: Exception) {
            if (!e.isTransientRequestFailure()) {
                Log.w(TAG, "Debrief download failed", e)
                debrief = null
                debriefLoadError = DEBRIEF_FAILURE_MESSAGE
                return
            }

            isRetryingDebrief = true
            delay(RETRY_DELAY_MILLIS)

            try {
                debrief = service.getDebrief(activeSessionId)
                debriefLoadError = null
            } catch (retryError: Exception) {
                Log.w(TAG, "Debrief download retry failed", retryError)
                debrief = null
                debriefLoadError = DEBRIEF_FAILURE_MESSAGE
            }
        } finally {
            isRetryingDebrief = false
        }
    }

    suspend fun synthesizeExaminerSpeech(
        segments: List<String>,
        kind: ExaminerTTSService.Kind = ExaminerTTSService.Kind.Prompt,
        caseIdOverride: String? = null,
    ): ByteArray {
        val normalizedSegments = segments.map { it.trim() }.filter { it.isNotEmpty() }
        val text = normalizedSegments.joinToString("\n\n")

        // Opening playback stitches the case stem and first question together,
        // which should use the cacheable "stem" TTS path on the server.
        val isOpeningPlayback = normalizedSegments.size >= 2 &&
            caseStem.isNotEmpty() &&
            normalizedSegments.first() == caseStem

        val resolvedKind = if (isOpeningPlayback) ExaminerTTSService.Kind.Stem else kind
        val resolvedCaseId = when {
            !caseIdOverride.isNullOrEmpty() -> caseIdOverride
            resolvedKind == ExaminerTTSService.Kind.Stem && caseId.isNotEmpty() -> caseId
            else -> null
        }

        return ttsService.synthesize(text = text, kind = resolvedKind, caseId = resolvedCaseId)
    }

    suspend fun abortCase() {
        if (caseCompleted) return

        val activeSessionId = sessionId
        if (activeSessionId == null) {
            clearCaseState()
            return
        }

        try {
            service.abortCase(activeSessionId)
        } catch (e: Exception) {
            Log.w(TAG, "Abort case failed", e)
        }
        clearCaseState()
    }

    fun resetCompletedCase() {
        if (!caseCompleted) return
        clearCaseState()
    }

    fun resetResponse() {
        candidateResponse = ""
        isSubmitting = false
        isRetryingSubmission = false
        submissionRetryError = null
    }

    // MARK: Interaction modes

    fun selectExaminerMode(mode: InteractionMode) = selectMode(mode, SpeechTarget.Examiner)

    fun selectUserMode(mode: InteractionMode) = selectMode(mode, SpeechTarget.Response)

    private fun selectMode(mode: InteractionMode, target: SpeechTarget) {
        if (mode == InteractionMode.Speech && !gate.hasPremiumAccess) {
            pendingSpeechTarget = target
            gate.requestSpeechPaywall()
            return
        }
        when (target) {
            SpeechTarget.Examiner -> examinerMode = mode
            SpeechTarget.Response -> userMode = mode
        }
    }

    private fun applySubscribedDefaultModesIfNeeded() {
        if (!gate.hasPremiumAccess || hasStartedCase) return
        if (examinerMode == InteractionMode.Text && userMode == InteractionMode.Text) {
            examinerMode = InteractionMode.Speech
            userMode = InteractionMode.Speech
        }
    }

    private fun applyPendingSpeechSelection() {
        when (pendingSpeechTarget) {
            SpeechTarget.Examiner -> examinerMode = InteractionMode.Speech
            SpeechTarget.Response -> userMode = InteractionMode.Speech
            null -> Unit
        }
        pendingSpeechTarget = null
    }

    // MARK: Case flow

    /** Starts the server session if needed, then presents (and, in speech mode, plays) the case. */
    fun beginCase() {
        viewModelScope.launch {
            if (!networkMonitor.isConnected()) {
                showNoInternetAlert = true
                return@launch
            }

            if (gate.hasReachedCompletionLimit) {
                gate.requestCompletionLimitPaywall()
                return@launch
            }

            val didStart = if (hasStartedCase) true else startCase()
            if (!didStart) return@launch

            container.preferences.hasStartedFirstCase = true
            if (examinerMode == InteractionMode.Speech) {
                playInitialPrompt()
            } else {
                // In text mode the case is considered "started" once the prompt
                // is visible; no audio playback is needed.
                playback.markInquiryPresentedWithoutPlayback()
            }
        }
    }

    private fun playInitialPrompt() {
        playback.playInitialPrompt(
            caseStem = caseStem,
            currentExaminerPrompt = currentExaminerPrompt,
            synthesizeSpeech = { segments -> synthesizeExaminerSpeech(segments) },
        )
    }

    private fun playFollowUpPrompt() {
        playback.playFollowUpPrompt(
            currentExaminerPrompt = currentExaminerPrompt,
            synthesizeSpeech = { segments -> synthesizeExaminerSpeech(segments) },
        )
    }

    fun repeatLastInquiry() {
        if (examinerMode != InteractionMode.Speech) return
        if (speechRecognition.isRecordingResponse) speechRecognition.stop()
        playback.repeatLastInquiry { segments -> synthesizeExaminerSpeech(segments) }
    }

    fun prepareDebrief() {
        isPreparingDebrief = true
        debriefPreparationError = null

        viewModelScope.launch {
            loadDebrief()
            isPreparingDebrief = false
            if (debrief != null) {
                debriefPreparationError = null
                eventChannel.send(Event.ShowDebrief)
            } else {
                debriefPreparationError = DEBRIEF_FAILURE_MESSAGE
            }
        }
    }

    /** Submits the current draft, stopping dictation first. */
    fun submitCandidateResponse() {
        speechRecognition.stop()
        val response = candidateResponse.trim()
        if (response.isEmpty()) return

        viewModelScope.launch {
            if (submitResponse(response)) {
                speechRecognition.clearTranscript()
            }
        }
    }

    fun resetResponseAndTranscript() {
        speechRecognition.stop()
        speechRecognition.clearTranscript()
        resetResponse()
    }

    /** Clears view-local runtime state; server session cleanup is handled by [abortCase]. */
    fun resetViewState() {
        playback.stop()
        speechRecognition.stop()
        playback.reset()
        speechRecognition.clearTranscript()
    }

    /** Throws away a finished case whose debrief could not be loaded. */
    fun resetCompletedCaseAfterDebriefFailure() {
        resetViewState()
        isPreparingDebrief = false
        debriefPreparationError = null
        resetCompletedCase()
    }

    /** Called when the debrief is dismissed. */
    fun onDebriefDismissed() {
        if (!caseCompleted) return
        isPreparingDebrief = false
        debriefPreparationError = null
        resetViewState()
        resetCompletedCase()
    }

    fun restoreStandardSettings() {
        requiredMustCoverPoints = 3
        allowedMajorErrors = 1
        allowedMinorErrors = 2
        maxTurns = 8
    }

    private fun logPerformance() {
        // Anonymous, aggregate counts only. Runs in the background so the
        // user is never blocked on it.
        viewModelScope.launch {
            try {
                service.logPerformance(
                    deviceId = container.preferences.anonymousPerformanceId,
                    caseId = caseId,
                    specialty = specialty.raw,
                    completionReason = completionReason,
                    turnCount = turns.size,
                    majorErrorsCount = majorErrors.size,
                    minorErrorsCount = minorErrors.size,
                    coveredPointsCount = coveredPoints.size,
                )
            } catch (e: Exception) {
                Log.w(TAG, "logPerformance failed", e)
            }
        }
    }

    // Shared by both explicit aborts and the debrief return flow.
    private fun clearCaseState() {
        sessionId = null
        currentExaminerPrompt = ""
        caseId = ""
        caseStem = ""
        caseTitle = ""
        candidateResponse = ""
        turns = emptyList()
        coveredPoints = emptyList()
        majorErrors = emptyList()
        minorErrors = emptyList()
        completionReason = ""
        debrief = null
        debriefLoadError = null
        submissionRetryError = null
        startCaseAlertTitle = null
        startCaseAlertMessage = null
        caseCompleted = false
        hasLoggedCaseCompleted = false
        isSubmitting = false
        isRetryingSubmission = false
        isRetryingDebrief = false
        examinerPromptPlaybackToken += 1
    }

    override fun onCleared() {
        playback.stop()
        speechRecognition.release()
        super.onCleared()
    }

    class Factory(
        private val application: Application,
        private val specialty: Specialty,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            OralCaseViewModel(application, specialty) as T
    }
}

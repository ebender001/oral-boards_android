package dev.benderapps.oralboardsai.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val TAG = "SpeechRecognition"
private const val RESTART_DELAY_MILLIS = 150L
private const val SPEECH_ACTIVITY_HOLD_MILLIS = 420L
private const val SPEECH_RMS_THRESHOLD_DB = 2f

/**
 * Continuous dictation on top of [SpeechRecognizer].
 *
 * Android's recognizer ends a session after a pause, so this restarts it while
 * the user is still "recording" and appends each recognized segment to the text
 * dictated so far. Must be used from the main thread.
 */
class SpeechRecognitionController(private val appContext: Context) {
    var isRecordingResponse by mutableStateOf(false)
        private set
    var isDetectingSpeech by mutableStateOf(false)
        private set
    var liveTranscript by mutableStateOf("")
        private set

    private val mainHandler = Handler(Looper.getMainLooper())
    private val earconMuter = EarconMuter(appContext)
    private var recognizer: SpeechRecognizer? = null

    private var dictatedResponsePrefix = ""
    private var shouldLowercaseResumedFirstWord = false

    // Bound to the view model's response text for the active dictation session.
    private var readResponse: () -> String = { "" }
    private var writeResponse: (String) -> Unit = {}
    private var reportError: (String) -> Unit = {}

    private val clearSpeechActivity = Runnable { isDetectingSpeech = false }

    fun toggle(
        candidateResponse: () -> String,
        setCandidateResponse: (String) -> Unit,
        stopExaminerPlayback: () -> Unit,
        presentError: (String) -> Unit,
    ) {
        if (isRecordingResponse) {
            stop()
        } else {
            start(candidateResponse, setCandidateResponse, stopExaminerPlayback, presentError)
        }
    }

    fun start(
        candidateResponse: () -> String,
        setCandidateResponse: (String) -> Unit,
        stopExaminerPlayback: () -> Unit,
        presentError: (String) -> Unit,
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            presentError("Speech recognition is not supported on this device.")
            return
        }

        stop()
        stopExaminerPlayback()

        readResponse = candidateResponse
        writeResponse = setCandidateResponse
        reportError = presentError

        val existingResponse = candidateResponse()
        dictatedResponsePrefix = dictationPrefix(existingResponse)
        shouldLowercaseResumedFirstWord = shouldContinueSentence(existingResponse)
        liveTranscript = existingResponse

        isRecordingResponse = true
        isDetectingSpeech = false
        earconMuter.mute()
        listen()
    }

    /** Stops dictation, keeping whatever has been transcribed so far. */
    fun stop() {
        if (readResponse().isNotEmpty() || liveTranscript.isNotEmpty()) {
            val preserved = readResponse().ifEmpty { liveTranscript }
            if (preserved.isNotEmpty()) {
                liveTranscript = preserved
                writeResponse(preserved)
            }
        }

        isRecordingResponse = false
        isDetectingSpeech = false
        mainHandler.removeCallbacksAndMessages(null)
        releaseRecognizer()
        earconMuter.restore()
    }

    fun beginManualEditing() {
        if (isRecordingResponse) {
            stop()
        }
        val current = readResponse()
        if (current.isNotEmpty()) {
            liveTranscript = current
        }
    }

    fun clearTranscript() {
        liveTranscript = ""
        dictatedResponsePrefix = ""
        shouldLowercaseResumedFirstWord = false
    }

    fun release() {
        stop()
    }

    private fun listen() {
        releaseRecognizer()

        val newRecognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        newRecognizer.setRecognitionListener(listener)
        recognizer = newRecognizer

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
            // Hints only; recognizers are free to ignore them.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 4000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
        }
        newRecognizer.startListening(intent)
    }

    private fun releaseRecognizer() {
        recognizer?.let {
            it.setRecognitionListener(null)
            it.cancel()
            it.destroy()
        }
        recognizer = null
    }

    private fun restartIfStillRecording() {
        if (!isRecordingResponse) return
        mainHandler.postDelayed({ if (isRecordingResponse) listen() }, RESTART_DELAY_MILLIS)
    }

    private fun applyTranscript(segment: String) {
        if (!isRecordingResponse) return
        val text = dictatedResponsePrefix + normalizedResumedTranscript(segment)
        liveTranscript = text
        writeResponse(text)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onRmsChanged(rmsdB: Float) {
            if (!isRecordingResponse || rmsdB < SPEECH_RMS_THRESHOLD_DB) return
            isDetectingSpeech = true
            mainHandler.removeCallbacks(clearSpeechActivity)
            mainHandler.postDelayed(clearSpeechActivity, SPEECH_ACTIVITY_HOLD_MILLIS)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            firstResult(partialResults)?.let(::applyTranscript)
        }

        override fun onResults(results: Bundle?) {
            firstResult(results)?.let(::applyTranscript)

            // The session ended after a pause; fold what we have into the prefix
            // and keep listening until the user stops dictation.
            val soFar = readResponse()
            dictatedResponsePrefix = dictationPrefix(soFar)
            shouldLowercaseResumedFirstWord = shouldContinueSentence(soFar)
            restartIfStillRecording()
        }

        override fun onError(error: Int) {
            if (!isRecordingResponse) return

            when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> restartIfStillRecording()

                else -> {
                    Log.w(TAG, "Recognition error code $error")
                    stop()
                    reportError(messageFor(error))
                }
            }
        }
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.takeIf { it.isNotBlank() }

    private fun messageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
            "Microphone access is required to transcribe your dictated responses. Enable it in Settings."
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
        SpeechRecognizer.ERROR_SERVER ->
            "Speech recognition needs a network connection right now. Check your connection and try again."
        SpeechRecognizer.ERROR_AUDIO -> "There was a problem recording audio. Please try again."
        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE ->
            "Speech recognition could not load your language. Check your connection, or download offline speech for your language in the Google app's settings."
        else -> "Speech recognition is not available right now."
    }

    private fun dictationPrefix(existing: String): String {
        if (existing.isEmpty()) return ""
        return if (existing.last().isWhitespace()) existing else "$existing "
    }

    private fun shouldContinueSentence(existing: String): Boolean {
        val last = existing.trim().lastOrNull() ?: return false
        return last != '.' && last != '!' && last != '?'
    }

    private fun normalizedResumedTranscript(transcript: String): String {
        if (!shouldLowercaseResumedFirstWord || transcript.isEmpty()) return transcript
        return transcript.first().lowercase() + transcript.drop(1)
    }
}

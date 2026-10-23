package dev.benderapps.oralboardsai.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "ExaminerPlayback"
private const val PREROLL_MILLIS = 300L

/**
 * Plays the examiner's synthesized speech and remembers the last inquiry so it
 * can be repeated without another network round trip.
 */
class ExaminerPlaybackController(
    private val appContext: Context,
    private val scope: CoroutineScope,
) {
    var isLoadingExaminerAudio by mutableStateOf(false)
        private set
    var hasPlayedExaminerInquiry by mutableStateOf(false)
        private set
    var isSpeaking by mutableStateOf(false)
        private set
    var isPaused by mutableStateOf(false)
        private set

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var player: MediaPlayer? = null
    private var audioFile: File? = null
    private var focusRequest: AudioFocusRequest? = null
    private var playbackJob: Job? = null

    private var lastSegments: List<String> = emptyList()
    private var lastAudio: ByteArray? = null

    fun playInitialPrompt(
        caseStem: String,
        currentExaminerPrompt: String,
        synthesizeSpeech: suspend (List<String>) -> ByteArray,
    ) {
        requestPlayback(listOf(caseStem, currentExaminerPrompt), rememberAsLast = true, synthesizeSpeech)
    }

    fun playFollowUpPrompt(
        currentExaminerPrompt: String,
        synthesizeSpeech: suspend (List<String>) -> ByteArray,
    ) {
        requestPlayback(listOf(currentExaminerPrompt), rememberAsLast = true, synthesizeSpeech)
    }

    /** In text mode the case counts as started once the prompt is visible. */
    fun markInquiryPresentedWithoutPlayback() {
        hasPlayedExaminerInquiry = true
    }

    fun repeatLastInquiry(synthesizeSpeech: suspend (List<String>) -> ByteArray) {
        if (lastSegments.isEmpty()) return

        val cached = lastAudio
        if (cached != null) {
            try {
                playAudio(cached)
            } catch (e: Exception) {
                Log.w(TAG, "Replay failed", e)
            }
        } else {
            requestPlayback(lastSegments, rememberAsLast = false, synthesizeSpeech)
        }
    }

    fun togglePlayback() {
        val current = player ?: return
        try {
            if (isPaused) {
                current.start()
                isSpeaking = true
                isPaused = false
            } else if (isSpeaking) {
                current.pause()
                isSpeaking = false
                isPaused = true
            }
        } catch (e: IllegalStateException) {
            Log.w(TAG, "togglePlayback failed", e)
            stop()
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        releasePlayer()
        isLoadingExaminerAudio = false
        isSpeaking = false
        isPaused = false
    }

    fun reset() {
        stop()
        lastSegments = emptyList()
        lastAudio = null
        hasPlayedExaminerInquiry = false
    }

    private fun requestPlayback(
        segments: List<String>,
        rememberAsLast: Boolean,
        synthesizeSpeech: suspend (List<String>) -> ByteArray,
    ) {
        if (rememberAsLast) {
            lastSegments = segments
            hasPlayedExaminerInquiry = true
        }

        stop()
        isLoadingExaminerAudio = true

        playbackJob = scope.launch {
            try {
                val audio = synthesizeSpeech(segments)
                if (rememberAsLast) {
                    lastAudio = audio
                }
                playAudio(audio)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                isLoadingExaminerAudio = false
                Log.w(TAG, "Examiner TTS failed", e)
            }
        }
    }

    private fun playAudio(audio: ByteArray) {
        releasePlayer()

        val file = File(appContext.cacheDir, "examiner-${UUID.randomUUID()}.mp3")
        file.writeBytes(audio)
        audioFile = file

        requestAudioFocus()

        val newPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(file.absolutePath)
            setOnCompletionListener {
                isSpeaking = false
                isPaused = false
                abandonAudioFocus()
            }
            setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error what=$what extra=$extra")
                stop()
                true
            }
            prepare()
        }
        player = newPlayer
        isLoadingExaminerAudio = false
        isSpeaking = true
        isPaused = false

        // Short preroll so the first syllable isn't clipped while the audio route wakes up.
        playbackJob = scope.launch {
            delay(PREROLL_MILLIS)
            if (player === newPlayer) {
                try {
                    newPlayer.start()
                } catch (e: IllegalStateException) {
                    Log.w(TAG, "start failed", e)
                    stop()
                }
            }
        }
    }

    private fun releasePlayer() {
        player?.let {
            try {
                it.stop()
            } catch (_: IllegalStateException) {
                // Already stopped or never started.
            }
            it.release()
        }
        player = null
        audioFile?.delete()
        audioFile = null
        abandonAudioFocus()
    }

    private fun requestAudioFocus() {
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .build()
        focusRequest = request
        audioManager.requestAudioFocus(request)
    }

    private fun abandonAudioFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}

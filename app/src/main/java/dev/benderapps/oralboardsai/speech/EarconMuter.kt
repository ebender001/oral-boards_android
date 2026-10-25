package dev.benderapps.oralboardsai.speech

import android.content.Context
import android.media.AudioManager

private const val PREFS_NAME = "earcon_muter"
private const val KEY_MUTED_STREAMS = "muted_streams"

/**
 * The system recognizer beeps every time a session starts, and continuous
 * dictation restarts sessions every few seconds. This mutes the streams the beep
 * plays on while dictating.
 *
 * The streams we muted are persisted so [restore] can undo it on the next launch
 * if the process was killed mid-dictation; otherwise the phone would stay muted.
 */
class EarconMuter(context: Context) {
    private val appContext = context.applicationContext
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun mute() {
        if (mutedStreams().isNotEmpty()) return

        // Only mute streams that are not already muted, so restore never unmutes
        // something the user muted themselves.
        val muted = STREAMS.filter { !audioManager.isStreamMute(it) && adjust(it, AudioManager.ADJUST_MUTE) }
        saveMutedStreams(muted)
    }

    /** Unmutes whatever [mute] muted. Safe to call at any time. */
    fun restore() {
        mutedStreams().forEach { adjust(it, AudioManager.ADJUST_UNMUTE) }
        saveMutedStreams(emptyList())
    }

    private fun adjust(stream: Int, direction: Int): Boolean = try {
        audioManager.adjustStreamVolume(stream, direction, 0)
        true
    } catch (_: SecurityException) {
        // Some streams can't be changed under Do Not Disturb; skip them.
        false
    }

    private fun mutedStreams(): List<Int> =
        prefs.getStringSet(KEY_MUTED_STREAMS, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }

    private fun saveMutedStreams(streams: List<Int>) {
        prefs.edit().putStringSet(KEY_MUTED_STREAMS, streams.map { it.toString() }.toSet()).apply()
    }

    private companion object {
        val STREAMS = intArrayOf(
            AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
        )
    }
}

package dev.benderapps.oralboardsai.data

import android.content.Context
import java.util.UUID

/** Small key/value settings that need to survive app launches. */
class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("oral_boards_prefs", Context.MODE_PRIVATE)

    /**
     * Reused across launches so the server can avoid repeating recently served
     * cases for this installation.
     */
    val clientInstanceId: String
        get() = getOrCreate(KEY_CLIENT_INSTANCE_ID)

    /** Anonymous device identifier sent with `logPerformance`. */
    val anonymousPerformanceId: String
        get() = getOrCreate(KEY_ANONYMOUS_PERFORMANCE_ID)

    var selectedSpecialty: Specialty
        get() = Specialty.fromRaw(prefs.getString(KEY_SELECTED_SPECIALTY, null))
            ?: Specialty.CardiothoracicSurgery
        set(value) = prefs.edit().putString(KEY_SELECTED_SPECIALTY, value.raw).apply()

    var hasSeenEducationalUseDisclaimer: Boolean
        get() = prefs.getBoolean(KEY_SEEN_DISCLAIMER, false)
        set(value) = prefs.edit().putBoolean(KEY_SEEN_DISCLAIMER, value).apply()

    var hasStartedFirstCase: Boolean
        get() = prefs.getBoolean(KEY_STARTED_FIRST_CASE, false)
        set(value) = prefs.edit().putBoolean(KEY_STARTED_FIRST_CASE, value).apply()

    var completedCaseCount: Int
        get() = prefs.getInt(KEY_COMPLETED_CASE_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_COMPLETED_CASE_COUNT, value).apply()

    private fun getOrCreate(key: String): String {
        prefs.getString(key, null)?.takeIf { it.isNotEmpty() }?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(key, newId).apply()
        return newId
    }

    private companion object {
        const val KEY_CLIENT_INSTANCE_ID = "client_instance_id"
        const val KEY_ANONYMOUS_PERFORMANCE_ID = "anonymous_performance_id"
        const val KEY_SELECTED_SPECIALTY = "selected_specialty"
        const val KEY_SEEN_DISCLAIMER = "hasSeenEducationalUseDisclaimerSheet"
        const val KEY_STARTED_FIRST_CASE = "hasStartedFirstCase"
        const val KEY_COMPLETED_CASE_COUNT = "completed_case_count"
    }
}

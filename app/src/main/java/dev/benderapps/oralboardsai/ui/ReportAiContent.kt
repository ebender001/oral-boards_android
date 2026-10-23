package dev.benderapps.oralboardsai.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import dev.benderapps.oralboardsai.BuildConfig

private const val REPORT_EMAIL = "support@oboardsai.com"

/**
 * Opens the user's email app with a pre-filled report about AI-generated content,
 * including enough context (case, turn, the text itself) to investigate.
 */
fun Context.reportAiContent(
    section: String,
    caseTitle: String?,
    caseId: String?,
    turnNumber: Int?,
    content: String?,
) {
    val body = buildString {
        append("Please describe what was wrong or inappropriate:\n\n\n\n")
        append("--- Details (please leave in) ---\n")
        append("Section: $section\n")
        if (!caseTitle.isNullOrBlank()) append("Case: $caseTitle\n")
        if (!caseId.isNullOrBlank()) append("Case ID: $caseId\n")
        if (turnNumber != null) append("Turn: $turnNumber\n")
        if (!content.isNullOrBlank()) append("Content: ${content.take(1_500)}\n")
        append("App version: ${BuildConfig.VERSION_NAME}\n")
    }

    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "Oral Boards – Report AI content")
        putExtra(Intent.EXTRA_TEXT, body)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(this, "No email app found. Contact $REPORT_EMAIL.", Toast.LENGTH_LONG).show()
    }
}

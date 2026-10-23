package dev.benderapps.oralboardsai.export

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import dev.benderapps.oralboardsai.data.CaseTurn
import dev.benderapps.oralboardsai.data.OralCaseDebriefResponse
import dev.benderapps.oralboardsai.ui.components.humanReadable
import java.io.File
import java.util.UUID

/**
 * Renders a debrief as a plain, paginated text PDF (US Letter) for sharing.
 */
object DebriefPdfExporter {
    private const val PAGE_WIDTH = 612
    private const val PAGE_HEIGHT = 792
    private const val MARGIN = 40f

    private class Block(val text: String, val paint: TextPaint, val spaceAfter: Float)

    fun export(context: Context, debrief: OralCaseDebriefResponse, liveTurns: List<CaseTurn>): File {
        val title = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val heading = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val body = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
        val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = 0xFF555555.toInt()
        }

        val summary = debrief.performanceSummary
        val blocks = mutableListOf<Block>()
        fun add(text: String, paint: TextPaint, after: Float = 6f) = blocks.add(Block(text, paint, after))

        add("Oral Boards AI — Case Debrief", title, 10f)
        add(debrief.caseInfo.title, heading, 4f)
        add(
            "Outcome: ${if (summary.outcome == "max_turns_reached") "Case Depth Reached" else humanReadable(summary.outcome)}" +
                " (${if (summary.passed) "Pass" else "Failure"})",
            body,
        )
        debrief.completionReason?.takeIf { it.isNotEmpty() }?.let { add(humanReadable(it), body, 12f) }

        add("Performance", heading, 4f)
        add("Required critical concepts: ${summary.mustCoverPointsRequired}", body, 2f)
        add("Covered critical concepts: ${summary.mustCoverPointsCovered}", body, 2f)
        add("Critical failures: ${summary.totalMajorErrors}", body, 2f)
        add("Recoverable errors: ${summary.totalMinorErrors}", body, 12f)

        if (summary.failureReasons.isNotEmpty()) {
            add(if (summary.passed) "Completion Reasons" else "Failure Reasons", heading, 4f)
            summary.failureReasons.forEach { add("• ${humanReadable(it)}", body, 2f) }
            add("", body, 8f)
        }

        debrief.caseInfo.stem?.takeIf { it.isNotEmpty() }?.let {
            add("Question Stem", heading, 4f)
            add(it, body, 12f)
        }

        add("Case Timeline", heading, 6f)
        debrief.turns.forEach { turn ->
            add("Turn ${turn.turnIndex + 1}", label, 2f)
            add("Examiner: ${turn.examinerPrompt}", body, 3f)
            add("Candidate: ${turn.candidateResponse}", body, 4f)

            liveTurns.firstOrNull { it.turnIndex == turn.turnIndex }?.let { live ->
                live.newMajorErrors.forEach { add("Critical failure: $it", body, 2f) }
                live.newMinorErrors.forEach { add("Recoverable error: $it", body, 2f) }
                live.missedConcepts.forEach { add("Missed: ${it.missedConcept}", body, 2f) }
            }
            add("", body, 8f)
        }

        val document = PdfDocument()
        val contentWidth = (PAGE_WIDTH - 2 * MARGIN).toInt()
        val contentBottom = PAGE_HEIGHT - MARGIN
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var y = MARGIN

        for (block in blocks) {
            val layout = StaticLayout.Builder
                .obtain(block.text, 0, block.text.length, block.paint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.1f)
                .build()

            if (y + layout.height > contentBottom && y > MARGIN) {
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
                y = MARGIN
            }

            page.canvas.save()
            page.canvas.translate(MARGIN, y)
            layout.draw(page.canvas)
            page.canvas.restore()
            y += layout.height + block.spaceAfter
        }
        document.finishPage(page)

        val directory = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(directory, "OralBoards-Debrief-${UUID.randomUUID()}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }
}

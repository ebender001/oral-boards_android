package dev.benderapps.oralboardsai.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.data.CaseTurn
import dev.benderapps.oralboardsai.data.OralCaseDebriefResponse
import dev.benderapps.oralboardsai.export.DebriefPdfExporter
import dev.benderapps.oralboardsai.ui.OralCaseViewModel
import dev.benderapps.oralboardsai.ui.components.MetricTile
import dev.benderapps.oralboardsai.ui.components.ReportProblemButton
import dev.benderapps.oralboardsai.ui.components.OutlinedOBCard
import dev.benderapps.oralboardsai.ui.components.SectionHeader
import dev.benderapps.oralboardsai.ui.components.StatusPill
import dev.benderapps.oralboardsai.ui.components.humanReadable
import dev.benderapps.oralboardsai.ui.reportAiContent
import dev.benderapps.oralboardsai.ui.theme.BrandBlue
import dev.benderapps.oralboardsai.ui.theme.CriticalRed
import dev.benderapps.oralboardsai.ui.theme.SuccessGreen
import dev.benderapps.oralboardsai.ui.theme.WarningOrange
import dev.benderapps.oralboardsai.ui.theme.oralBoards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class DebriefTurnStatus(val title: String) {
    MajorIssue("Critical Issue"),
    Partial("Partially Covered"),
    Covered("Covered"),
    Reviewed("Reviewed"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebriefScreen(
    viewModel: OralCaseViewModel,
    onClose: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    val context = LocalContext.current
    val store = context.appContainer.performanceStore
    val scope = rememberCoroutineScope()
    val debrief = viewModel.debrief

    var isPreparingShare by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    BackHandler(onBack = onClose)

    LaunchedEffect(Unit) {
        if (viewModel.debrief == null && viewModel.debriefLoadError == null) {
            viewModel.loadDebrief()
        }
    }

    // Keep a local copy of every completed case for the progress dashboard.
    LaunchedEffect(debrief?.sessionId) {
        debrief?.let { store.saveDebrief(it) }
    }

    fun sharePdf(debrief: OralCaseDebriefResponse) {
        isPreparingShare = true
        scope.launch {
            try {
                val file: File = withContext(Dispatchers.IO) {
                    DebriefPdfExporter.export(context, debrief, viewModel.turns)
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, "Share debrief"))
            } catch (e: Exception) {
                exportError = e.message ?: "The debrief could not be exported."
            } finally {
                isPreparingShare = false
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.oralBoards.grouped,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Case Debrief") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { debrief?.let(::sharePdf) },
                        enabled = debrief != null && !isPreparingShare,
                    ) {
                        if (isPreparingShare) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Share, contentDescription = "Share debrief")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.oralBoards.grouped,
                ),
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                debrief != null -> DebriefContent(
                    debrief = debrief,
                    viewModel = viewModel,
                    onOpenProgress = onOpenProgress,
                    onReport = {
                        context.reportAiContent(
                            section = "Debrief feedback",
                            caseTitle = debrief.caseInfo.title,
                            caseId = debrief.caseInfo.id,
                            turnNumber = null,
                            content = null,
                        )
                    },
                )

                viewModel.debriefLoadError == null -> Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                ) {
                    CircularProgressIndicator()
                    Text(
                        "Preparing your debrief…",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> DebriefErrorCard(
                    onTryAgain = { scope.launch { viewModel.loadDebrief() } },
                    onReturn = onClose,
                )
            }
        }
    }

    exportError?.let { message ->
        AlertDialog(
            onDismissRequest = { exportError = null },
            title = { Text("Export Failed") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { exportError = null }) { Text("OK") } },
        )
    }
}

@Composable
private fun DebriefErrorCard(onTryAgain: () -> Unit, onReturn: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.oralBoards.card)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(28.dp))
            Text("Unable to load debrief", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "The case finished, but the debrief could not be prepared.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Text(
                "Please try again, return to the case, or start a new case.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = onTryAgain) { Text("Try Again") }
                OutlinedButton(onClick = onReturn) { Text("Return to Case") }
            }
        }
    }
}

@Composable
private fun DebriefContent(
    debrief: OralCaseDebriefResponse,
    viewModel: OralCaseViewModel,
    onOpenProgress: () -> Unit,
    onReport: () -> Unit,
) {
    val summary = debrief.performanceSummary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        OutlinedOBCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Outcome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            humanReadableOutcome(summary.outcome),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    StatusPill(
                        text = if (summary.passed) "Pass" else "Failure",
                        tint = if (summary.passed) SuccessGreen else CriticalRed,
                    )
                }
                debrief.completionReason?.takeIf { it.isNotEmpty() }?.let {
                    Text(
                        humanReadable(it),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        OutlinedOBCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Case Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Title: ${debrief.caseInfo.title}", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Status: ${humanReadable(debrief.status)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Tapping the performance card opens trends across cases.
        Box(modifier = Modifier.clickable(onClick = onOpenProgress)) {
            OutlinedOBCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            value = "${summary.mustCoverPointsRequired}",
                            label = "Required critical concepts",
                            tint = BrandBlue,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        MetricTile(
                            value = "${summary.mustCoverPointsCovered}",
                            label = "Covered critical concepts",
                            tint = SuccessGreen,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            value = "${summary.totalMajorErrors}",
                            label = "Critical failures",
                            tint = CriticalRed,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                        MetricTile(
                            value = "${summary.totalMinorErrors}",
                            label = "Recoverable errors",
                            tint = WarningOrange,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    }
                    Row(
                        modifier = Modifier.padding(top = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(18.dp))
                        Text(
                            "View your progress",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandBlue,
                        )
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowForwardIos,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }

        if (summary.failureReasons.isNotEmpty()) {
            OutlinedOBCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        if (summary.passed) "Completion Reasons" else "Failure Reasons",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    summary.failureReasons.forEach { reason ->
                        Text(
                            "• ${humanReadable(reason)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (viewModel.coveredPoints.isNotEmpty()) {
            OutlinedOBCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader("Critical Concepts Covered", Icons.Rounded.CheckCircle, SuccessGreen, "Covered")
                    viewModel.coveredPoints.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        OutlinedOBCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    "Critical Failures",
                    Icons.Rounded.Warning,
                    CriticalRed,
                    if (viewModel.majorErrors.isEmpty()) "None" else "Critical",
                )
                if (viewModel.majorErrors.isEmpty()) {
                    Text(
                        "No critical failures",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    viewModel.majorErrors.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        OutlinedOBCard {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionHeader(
                    "Recoverable Errors",
                    Icons.Rounded.ErrorOutline,
                    WarningOrange,
                    if (viewModel.minorErrors.isEmpty()) "None" else "Recoverable",
                )
                if (viewModel.minorErrors.isEmpty()) {
                    Text(
                        "No recoverable errors",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    viewModel.minorErrors.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }

        debrief.caseInfo.stem?.takeIf { it.isNotEmpty() }?.let { stem ->
            OutlinedOBCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Question Stem", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stem, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (debrief.turns.isNotEmpty()) {
            OutlinedOBCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Case Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    debrief.turns.forEach { turn ->
                        TimelineTurn(turn, debrief, viewModel.turns.firstOrNull { it.turnIndex == turn.turnIndex })
                    }
                }
            }
        }

        ReportProblemButton(onClick = onReport, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun TimelineTurn(
    turn: OralCaseDebriefResponse.Turn,
    debrief: OralCaseDebriefResponse,
    submittedTurn: CaseTurn?,
) {
    val events = debrief.turnAlignment
        .firstOrNull { it.turnId == turn.id || it.turnIndex == turn.turnIndex }
        ?.events
        .orEmpty()
    val status = turnStatus(events, submittedTurn)
    val statusColor = statusTint(status)
    val responseColor = responseColor(events, submittedTurn)

    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(shape)
            .background(MaterialTheme.oralBoards.cardInset)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)), shape),
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 12.dp)
                .width(4.dp)
                .fillMaxHeight()
                .background(statusColor.copy(alpha = 0.9f)),
        )
        Column(
            modifier = Modifier.weight(1f).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill("Turn ${turn.turnIndex + 1}", MaterialTheme.colorScheme.onSurfaceVariant)
                StatusPill(status.title, statusColor)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Examiner",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(turn.examinerPrompt, style = MaterialTheme.typography.bodyMedium)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Candidate",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(turn.candidateResponse, style = MaterialTheme.typography.bodyMedium, color = responseColor)
            }

            val hasLiveFeedback = submittedTurn != null &&
                (submittedTurn.missedConcepts.isNotEmpty() || submittedTurn.examinerWasLookingFor.isNotEmpty())

            if (submittedTurn != null && hasLiveFeedback) {
                Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (submittedTurn.missedConcepts.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Missed concepts",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                            )
                            submittedTurn.missedConcepts.forEach {
                                Text("• ${it.missedConcept}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    if (submittedTurn.examinerWasLookingFor.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Examiner was looking for",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor,
                            )
                            expectationGroups(submittedTurn).forEach { (label, explanations) ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (label != null) {
                                        Text(
                                            label,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    explanations.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                                }
                            }
                        }
                    }
                }
            } else if (events.any { it.severity != null } || !turn.aiSummary.isNullOrBlank()) {
                // No live feedback was kept for this turn (e.g. the final response), so
                // fall back to the evaluator's notes from the debrief itself.
                Column(modifier = Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Evaluator notes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = statusColor,
                    )
                    events.filter { it.explanation.isNotBlank() }.forEach {
                        Text("• ${it.explanation}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (events.isEmpty()) {
                        turn.aiSummary?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

private fun humanReadableOutcome(value: String): String =
    if (value == "max_turns_reached") "Case Depth Reached" else humanReadable(value)

private fun turnStatus(
    events: List<OralCaseDebriefResponse.EvaluationEvent>,
    submittedTurn: CaseTurn?,
): DebriefTurnStatus = when {
    events.any { it.severity?.lowercase() == "major" } ||
        submittedTurn?.newMajorErrors?.isNotEmpty() == true -> DebriefTurnStatus.MajorIssue

    events.any { it.severity?.lowercase() == "minor" } ||
        submittedTurn?.newMinorErrors?.isNotEmpty() == true -> DebriefTurnStatus.Partial

    events.isNotEmpty() || submittedTurn != null -> DebriefTurnStatus.Covered
    else -> DebriefTurnStatus.Reviewed
}

private fun statusTint(status: DebriefTurnStatus): Color = when (status) {
    DebriefTurnStatus.MajorIssue -> CriticalRed
    DebriefTurnStatus.Partial -> WarningOrange
    DebriefTurnStatus.Covered -> SuccessGreen
    DebriefTurnStatus.Reviewed -> Color.Gray
}

@Composable
private fun responseColor(
    events: List<OralCaseDebriefResponse.EvaluationEvent>,
    submittedTurn: CaseTurn?,
): Color = when {
    events.any { it.severity?.lowercase() == "major" } -> CriticalRed
    events.any { it.severity?.lowercase() == "minor" } -> WarningOrange
    events.isNotEmpty() -> SuccessGreen
    submittedTurn != null -> when {
        submittedTurn.newMajorErrors.isNotEmpty() -> CriticalRed
        submittedTurn.newMinorErrors.isNotEmpty() -> WarningOrange
        else -> SuccessGreen
    }
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun expectationGroups(turn: CaseTurn): List<Pair<String?, List<String>>> =
    turn.examinerWasLookingFor
        .groupBy { it.label.trim().ifEmpty { null } }
        .map { (label, values) -> label to values.map { it.explanation } }
        .sortedWith(
            compareBy<Pair<String?, List<String>>> { it.first == null }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.first.orEmpty() }
        )

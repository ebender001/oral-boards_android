package dev.benderapps.oralboardsai.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.data.LocalCasePerformance
import dev.benderapps.oralboardsai.data.OralCaseDebriefResponse
import dev.benderapps.oralboardsai.ui.components.MetricTile
import dev.benderapps.oralboardsai.ui.components.OutlinedOBCard
import dev.benderapps.oralboardsai.ui.components.StatusPill
import dev.benderapps.oralboardsai.ui.components.humanReadable
import dev.benderapps.oralboardsai.ui.theme.BrandBlue
import dev.benderapps.oralboardsai.ui.theme.ConceptPurple
import dev.benderapps.oralboardsai.ui.theme.CriticalRed
import dev.benderapps.oralboardsai.ui.theme.SuccessGreen
import dev.benderapps.oralboardsai.ui.theme.Teal
import dev.benderapps.oralboardsai.ui.theme.WarningOrange
import dev.benderapps.oralboardsai.ui.theme.oralBoards
import java.text.DateFormat
import java.util.Date
import java.util.Locale

// MARK: - Dashboard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceDashboardScreen(
    onBack: () -> Unit,
    onOpenCase: (String) -> Unit,
) {
    val cases by LocalContext.current.appContainer.performanceStore.cases.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.oralBoards.grouped,
        topBar = { SimpleTopBar("Your Progress", onBack) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OverviewCard(cases)
            if (cases.isEmpty()) {
                EmptyStateCard()
            } else {
                WeaknessSummaryCard(cases)
                RecentCasesCard(cases, onOpenCase)
            }
        }
    }
}

@Composable
private fun OverviewCard(cases: List<LocalCasePerformance>) {
    val passRate = if (cases.isEmpty()) "--" else
        "${(cases.count { it.passed }.toDouble() / cases.size * 100).let { Math.round(it) }}%"
    val averageCovered = if (cases.isEmpty()) "--" else
        String.format(Locale.US, "%.1f", cases.sumOf { it.mustCoverPointsCovered }.toDouble() / cases.size)
    val averageRecoverable = if (cases.isEmpty()) "--" else
        String.format(Locale.US, "%.1f", cases.sumOf { it.totalMinorErrors }.toDouble() / cases.size)

    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Performance Overview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile("${cases.size}", "Total cases", BrandBlue, Modifier.weight(1f))
                MetricTile(passRate, "Pass rate", SuccessGreen, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricTile(averageCovered, "Avg covered concepts", Teal, Modifier.weight(1f))
                MetricTile(averageRecoverable, "Avg recoverable errors", WarningOrange, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EmptyStateCard() {
    OutlinedOBCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.ShowChart, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(30.dp))
            }
            Text("No saved cases yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "Complete cases and open the debrief to start building your performance trends.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun WeaknessSummaryCard(cases: List<LocalCasePerformance>) {
    val allPassed = cases.all { it.passed }

    val reasons = cases.flatMap { it.failureReasons }.map(::humanReadable)
    val reasonText = if (reasons.isEmpty()) {
        if (allPassed) "No recurring completion reasons yet" else "No recurring failure reasons yet"
    } else {
        topCounts(reasons, limit = 2).joinToString(", ")
    }

    val domains = cases.filter { !it.passed }.mapNotNull { it.caseDomain?.takeIf(String::isNotEmpty) }.map(::humanReadable)
    val domainText = if (domains.isEmpty()) "No non-passing domains yet" else topCounts(domains, limit = 2).joinToString(", ")

    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Weakness Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            WeaknessRow(
                title = if (allPassed) "Common completion reasons" else "Common failure reasons",
                value = reasonText,
                tint = CriticalRed,
            )
            WeaknessRow("Non-passing domains", domainText, WarningOrange)
            WeaknessRow("Recoverable error trend", recoverableErrorTrend(cases), BrandBlue)
        }
    }
}

@Composable
private fun WeaknessRow(title: String, value: String, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(9.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.5f)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun RecentCasesCard(cases: List<LocalCasePerformance>, onOpenCase: (String) -> Unit) {
    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Recent Cases", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            cases.forEachIndexed { index, case ->
                RecentCaseRow(case, onClick = { onOpenCase(case.sessionId) })
                if (index != cases.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun RecentCaseRow(case: LocalCasePerformance, onClick: () -> Unit) {
    val tint = if (case.passed) SuccessGreen else CriticalRed
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(case.caseTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(case.createdAtMillis)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusPill(humanReadable(case.outcome), tint)
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp).padding(top = 2.dp),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("${case.mustCoverPointsCovered} / ${case.mustCoverPointsRequired} concepts", SuccessGreen)
            StatusPill("${case.totalMajorErrors} critical", CriticalRed)
            StatusPill("${case.totalMinorErrors} recoverable", WarningOrange)
        }
    }
}

private fun topCounts(values: List<String>, limit: Int): List<String> =
    values.groupingBy { it }.eachCount().entries
        .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
        .take(limit)
        .map { (value, count) -> if (count > 1) "$value ($count)" else value }

private fun recoverableErrorTrend(cases: List<LocalCasePerformance>): String {
    if (cases.size < 2) return "More cases needed for a trend"
    val sorted = cases.sortedByDescending { it.createdAtMillis }
    val recent = sorted.take(minOf(3, sorted.size))
    val older = sorted.drop(recent.size)
    if (older.isEmpty()) return "More cases needed for a trend"

    val recentAverage = recent.sumOf { it.totalMinorErrors }.toDouble() / recent.size
    val olderAverage = older.sumOf { it.totalMinorErrors }.toDouble() / older.size
    return when {
        recentAverage < olderAverage -> "Improving"
        recentAverage > olderAverage -> "Increasing"
        else -> "Stable"
    }
}

// MARK: - Historical review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoricalCaseReviewScreen(sessionId: String, onBack: () -> Unit) {
    val cases by LocalContext.current.appContainer.performanceStore.cases.collectAsState()
    val case = cases.firstOrNull { it.sessionId == sessionId }

    Scaffold(
        containerColor = MaterialTheme.oralBoards.grouped,
        topBar = { SimpleTopBar("Case Review", onBack) },
    ) { innerPadding ->
        if (case == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("This case is no longer available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ReviewOutcomeCard(case)
            ReviewPerformanceCard(case)
            ReviewCoachingCard(case)
            ReviewEvaluationTimelineCard(case)
            if (case.turns.isNotEmpty()) ReviewTranscriptCard(case)
        }
    }
}

@Composable
private fun ReviewOutcomeCard(case: LocalCasePerformance) {
    val outcome = humanReadable(case.outcome)
    val tint = when {
        !case.passed -> CriticalRed
        outcome.contains("borderline", ignoreCase = true) -> WarningOrange
        else -> SuccessGreen
    }

    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Outcome", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(case.caseTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    case.caseDomain?.takeIf { it.isNotEmpty() }?.let {
                        Text(
                            humanReadable(it),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                StatusPill(outcome, tint)
            }
            case.completionReason?.takeIf { it.isNotEmpty() }?.let {
                HorizontalDivider()
                Text(
                    "Completion: ${humanReadable(it)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReviewPerformanceCard(case: LocalCasePerformance) {
    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Performance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("${case.mustCoverPointsRequired}", "Required concepts", BrandBlue, Modifier.weight(1f))
                MetricTile("${case.mustCoverPointsCovered}", "Covered concepts", SuccessGreen, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("${case.totalMajorErrors}", "Critical failures", CriticalRed, Modifier.weight(1f))
                MetricTile("${case.totalMinorErrors}", "Recoverable errors", WarningOrange, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricTile("${case.turnCount}", "Turns", ConceptPurple, Modifier.weight(1f))
                Box(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ReviewCoachingCard(case: LocalCasePerformance) {
    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val clean = case.failureReasons.isEmpty()
                Icon(
                    if (clean) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = if (clean) SuccessGreen else WarningOrange,
                )
                Text("Coaching", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (case.failureReasons.isEmpty()) {
                Text(
                    "No major coaching concerns identified.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                )
            } else {
                case.failureReasons.forEach { reason ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("•", color = WarningOrange, style = MaterialTheme.typography.titleMedium)
                        Text(
                            humanReadable(reason),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewEvaluationTimelineCard(case: LocalCasePerformance) {
    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Evaluation Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (case.evaluationEvents.isEmpty()) {
                Text(
                    "No evaluation events were saved for this case.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                case.evaluationEvents.sortedBy { it.turnIndex }.forEachIndexed { index, event ->
                    EvaluationEventRow(event)
                    if (index != case.evaluationEvents.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun EvaluationEventRow(event: OralCaseDebriefResponse.EvaluationEvent) {
    val normalizedKind = event.kind.replace("_", "").replace(" ", "").lowercase()
    val kindLabel = when (normalizedKind) {
        "mustcoverpoint", "mustcover" -> "Critical Concept"
        "casestatus" -> "Case Status"
        "majorerror" -> "Critical Failure"
        "minorerror" -> "Recoverable Error"
        "error" -> "Evaluation Concern"
        else -> humanReadable(event.kind)
    }
    val tint = when {
        event.isDecisive == true || event.severity == "major" -> CriticalRed
        event.severity == "minor" -> WarningOrange
        normalizedKind == "mustcoverpoint" || normalizedKind == "mustcover" -> SuccessGreen
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.padding(top = 6.dp).size(10.dp).clip(CircleShape).background(tint.copy(alpha = 0.5f)))
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Turn ${event.turnIndex + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(kindLabel, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = tint)
                event.severity?.takeIf { it.isNotEmpty() }?.let { StatusPill(humanReadable(it), tint) }
            }
            Text(
                event.explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f),
            )
        }
    }
}

@Composable
private fun ReviewTranscriptCard(case: LocalCasePerformance) {
    OutlinedOBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Case Transcript", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            case.turns.sortedBy { it.turnIndex }.forEachIndexed { index, turn ->
                TranscriptTurn(turn)
                if (index != case.turns.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun TranscriptTurn(turn: OralCaseDebriefResponse.Turn) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Turn ${turn.turnIndex + 1}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TranscriptBlock("Examiner", turn.examinerPrompt)
                TranscriptBlock("Candidate", turn.candidateResponse)
            }
        }
    }
}

@Composable
private fun TranscriptBlock(title: String, text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleTopBar(title: String, onBack: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.oralBoards.grouped),
    )
}

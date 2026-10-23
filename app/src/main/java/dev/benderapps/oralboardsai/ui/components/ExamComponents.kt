package dev.benderapps.oralboardsai.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Chat
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benderapps.oralboardsai.data.InteractionMode
import dev.benderapps.oralboardsai.ui.theme.BeginGreen
import dev.benderapps.oralboardsai.ui.theme.BeginGreenDark
import dev.benderapps.oralboardsai.ui.theme.BrandBlue
import dev.benderapps.oralboardsai.ui.theme.ConceptPurple
import dev.benderapps.oralboardsai.ui.theme.CriticalRed
import dev.benderapps.oralboardsai.ui.theme.SuccessGreen
import dev.benderapps.oralboardsai.ui.theme.WarningOrange
import dev.benderapps.oralboardsai.ui.theme.oralBoards
import kotlin.math.pow
import kotlin.math.sin

// MARK: - Exam setup

@Composable
fun ExamParameterSummary(
    requiredMustCoverPoints: Int,
    allowedMajorErrors: Int,
    allowedMinorErrors: Int,
    maxTurns: Int,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryItem(
                tint = SuccessGreen,
                number = "$requiredMustCoverPoints",
                label = "Critical Concepts",
                caption = "Required",
                modifier = Modifier.weight(1f),
            )
            SummaryItem(
                tint = CriticalRed,
                number = "$allowedMajorErrors",
                label = if (allowedMajorErrors == 1) "Critical Failure" else "Critical Failures",
                caption = "Maximum",
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SummaryItem(
                tint = WarningOrange,
                number = "$allowedMinorErrors",
                label = if (allowedMinorErrors == 1) "Recoverable Error" else "Recoverable Errors",
                caption = "Maximum",
                modifier = Modifier.weight(1f),
            )
            SummaryItem(
                tint = ConceptPurple,
                number = "$maxTurns",
                label = if (maxTurns == 1) "Turn" else "Turns",
                caption = "Case Depth",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryItem(
    tint: Color,
    number: String,
    label: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(tint))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
                Text(number, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Text(caption, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ExamSettingsCard(
    requiredMustCoverPoints: Int,
    allowedMajorErrors: Int,
    allowedMinorErrors: Int,
    maxTurns: Int,
    onTap: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.oralBoards.card)
            .border(BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.16f)), RoundedCornerShape(18.dp))
            .clickable(onClick = onTap)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(SuccessGreen.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Tune, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(13.dp))
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Exam Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Adjust difficulty and scoring",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ExamParameterSummary(requiredMustCoverPoints, allowedMajorErrors, allowedMinorErrors, maxTurns)
    }
}

@Composable
fun ExamSetupCard(
    examinerMode: InteractionMode,
    userMode: InteractionMode,
    requiredMustCoverPoints: Int,
    allowedMajorErrors: Int,
    allowedMinorErrors: Int,
    maxTurns: Int,
    shouldShowExamParameterControls: Boolean,
    onSelectExaminerMode: (InteractionMode) -> Unit,
    onSelectUserMode: (InteractionMode) -> Unit,
    onShowExamParameters: () -> Unit,
    onShowParametersLearnMore: () -> Unit,
) {
    OBCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Exam Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

            PillSegmentedPicker(
                title = "Examiner Mode",
                subtitle = "How the examiner communicates",
                selection = examinerMode,
                onSelect = onSelectExaminerMode,
            )
            PillSegmentedPicker(
                title = "Response Mode",
                subtitle = "How you answer the examiner",
                selection = userMode,
                onSelect = onSelectUserMode,
            )

            AnimatedVisibility(
                visible = shouldShowExamParameterControls,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    ExamSettingsCard(
                        requiredMustCoverPoints = requiredMustCoverPoints,
                        allowedMajorErrors = allowedMajorErrors,
                        allowedMinorErrors = allowedMinorErrors,
                        maxTurns = maxTurns,
                        onTap = onShowExamParameters,
                    )
                    Row(
                        modifier = Modifier.clickable(onClick = onShowParametersLearnMore).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            tint = Color(0xFF576E85),
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            "How these settings affect scoring",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF576E85),
                        )
                    }
                }
            }
        }
    }
}

// MARK: - Examiner

@Composable
fun BeginCaseButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(BeginGreen, BeginGreenDark)))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)), RoundedCornerShape(24.dp))
            .clickable(enabled = !isLoading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = Color.White,
                    strokeWidth = 2.dp,
                )
                Text("Loading case...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            } else {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                Text("Begin Case", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ExaminerCard(
    examinerMode: InteractionMode,
    caseStem: String,
    currentExaminerPrompt: String,
    hasPlayedExaminerInquiry: Boolean,
    isStartingCase: Boolean,
    isWaitingForNextQuestion: Boolean,
    isPlaybackActive: Boolean,
    isPlaybackPaused: Boolean,
    onRepeat: () -> Unit,
    onTogglePlayback: () -> Unit,
) {
    OBCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (examinerMode == InteractionMode.Speech) Icons.Rounded.VolumeUp else Icons.Rounded.Chat,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text("Examiner", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }

            if (!hasPlayedExaminerInquiry) {
                InactivePlaceholder()
            } else if (examinerMode == InteractionMode.Text) {
                TextExaminerContent(caseStem, currentExaminerPrompt, isWaitingForNextQuestion)
            } else {
                SpeechExaminerContent(
                    isStartingCase = isStartingCase,
                    isPlaybackActive = isPlaybackActive,
                    isPlaybackPaused = isPlaybackPaused,
                    onRepeat = onRepeat,
                    onTogglePlayback = onTogglePlayback,
                )
            }
        }
    }
}

@Composable
private fun InactivePlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .padding(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "Case presentation will appear here",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "The examiner will begin once the case starts",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TextExaminerContent(
    caseStem: String,
    currentExaminerPrompt: String,
    isWaitingForNextQuestion: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.oralBoards.cardInset)
                .border(BorderStroke(1.dp, BrandBlue.copy(alpha = 0.26f)), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "CASE PRESENTATION",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.6.sp,
                    color = BrandBlue.copy(alpha = 0.74f),
                )
                Text(
                    "Read carefully before responding",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(caseStem, style = MaterialTheme.typography.bodyLarge)
        }

        if (currentExaminerPrompt.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.oralBoards.cardInset)
                    .border(BorderStroke(1.dp, ConceptPurple.copy(alpha = 0.28f)), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                        tint = ConceptPurple.copy(alpha = 0.74f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "QUESTION",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.6.sp,
                        color = ConceptPurple.copy(alpha = 0.76f),
                    )
                    if (isWaitingForNextQuestion) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = ConceptPurple,
                        )
                    }
                }
                Text(
                    text = currentExaminerPrompt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(ConceptPurple.copy(alpha = 0.07f))
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun SpeechExaminerContent(
    isStartingCase: Boolean,
    isPlaybackActive: Boolean,
    isPlaybackPaused: Boolean,
    onRepeat: () -> Unit,
    onTogglePlayback: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            "The examiner will present the case verbally.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(onClick = onRepeat, modifier = Modifier.fillMaxWidth(), enabled = !isStartingCase) {
            if (isStartingCase) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(Modifier.size(8.dp))
                Text("Loading case...")
            } else {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Repeat Last Inquiry")
            }
        }

        if (isPlaybackActive || isPlaybackPaused) {
            OutlinedButton(onClick = onTogglePlayback, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    if (isPlaybackPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(if (isPlaybackPaused) "Resume Speech" else "Pause Speech")
            }
        }
    }
}

// MARK: - Response

@Composable
fun ResponseCard(
    userMode: InteractionMode,
    candidateResponse: String,
    onCandidateResponseChange: (String) -> Unit,
    isRecordingResponse: Boolean,
    isSpeechActivityDetected: Boolean,
    dictationButtonDisabled: Boolean,
    isSubmitting: Boolean,
    submissionRetryError: String?,
    resetDisabled: Boolean,
    submitDisabled: Boolean,
    onToggleSpeechRecognition: () -> Unit,
    onTranscriptBeginEditing: () -> Unit,
    onSubmit: () -> Unit,
    onRetrySubmission: () -> Unit,
    onReset: () -> Unit,
) {
    OBCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = if (userMode == InteractionMode.Speech) Icons.Rounded.Mic else Icons.Rounded.Keyboard,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Text("Your Response", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "State your answer as if you were responding to the oral examiner.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (userMode == InteractionMode.Speech) {
                Button(
                    onClick = onToggleSpeechRecognition,
                    enabled = !dictationButtonDisabled,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    colors = if (isRecordingResponse) {
                        ButtonDefaults.buttonColors()
                    } else {
                        ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                    },
                ) {
                    Icon(
                        imageVector = if (isRecordingResponse) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        if (isRecordingResponse) "Stop Dictation" else "Start Dictation",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                SpeechWaveform(isAnimating = isRecordingResponse && isSpeechActivityDetected)

                // Dictated text can still be corrected by hand once there is some.
                if (candidateResponse.isNotBlank()) {
                    TranscriptField(
                        label = "Live Transcript",
                        value = candidateResponse,
                        onValueChange = onCandidateResponseChange,
                        onFocused = onTranscriptBeginEditing,
                        placeholder = "Your dictated response will appear here.",
                    )
                } else {
                    Text(
                        "Your dictated response will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                TranscriptField(
                    label = null,
                    value = candidateResponse,
                    onValueChange = onCandidateResponseChange,
                    onFocused = {},
                    placeholder = "Type your response…",
                )
            }

            ResponseActionButtons(
                isSubmitting = isSubmitting,
                submitDisabled = submitDisabled,
                resetDisabled = resetDisabled,
                onSubmit = onSubmit,
                onReset = onReset,
            )

            if (submissionRetryError != null) {
                SubmissionRetryErrorCard(submissionRetryError, onRetrySubmission)
            }
        }
    }
}

@Composable
private fun TranscriptField(
    label: String?,
    value: String,
    onValueChange: (String) -> Unit,
    onFocused: () -> Unit,
    placeholder: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (label != null) {
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .onFocusChanged { if (it.isFocused) onFocused() },
            placeholder = { Text(placeholder) },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrectEnabled = false,
            ),
            shape = RoundedCornerShape(12.dp),
        )
    }
}

@Composable
private fun ResponseActionButtons(
    isSubmitting: Boolean,
    submitDisabled: Boolean,
    resetDisabled: Boolean,
    onSubmit: () -> Unit,
    onReset: () -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(
            onClick = onSubmit,
            enabled = !submitDisabled,
            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Submit Response", maxLines = 1)
            }
        }
        OutlinedButton(onClick = onReset, enabled = !resetDisabled, modifier = Modifier.heightIn(min = 48.dp)) {
            Icon(Icons.Rounded.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.size(6.dp))
            Text("Reset", maxLines = 1)
        }
    }
}

@Composable
private fun SubmissionRetryErrorCard(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .border(BorderStroke(1.dp, WarningOrange.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.WifiOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Connection Interrupted", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onRetry) { Text("Retry Submission") }
    }
}

/** Animated bars that move while the user is audibly speaking. */
@Composable
private fun SpeechWaveform(isAnimating: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(durationMillis = 1_400, easing = LinearEasing), RepeatMode.Restart),
        label = "waveformPhase",
    )
    val level by animateFloatAsState(
        targetValue = if (isAnimating) 1f else 0f,
        animationSpec = tween(if (isAnimating) 550 else 1_250),
        label = "waveformLevel",
    )
    val borderColor by animateColorAsState(
        if (isAnimating) SuccessGreen.copy(alpha = 0.22f) else Color.Gray.copy(alpha = 0.20f),
        label = "waveformBorder",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(118.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.oralBoards.cardInset)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        val idlePattern = remember { floatArrayOf(0.16f, 0.24f, 0.34f, 0.25f, 0.18f, 0.30f) }
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val barCount = 42
            val barWidth = 3.5.dp.toPx()
            val gap = (size.width - barCount * barWidth) / (barCount - 1)
            val brush = Brush.verticalGradient(listOf(BrandBlue.copy(alpha = 0.82f), SuccessGreen.copy(alpha = 0.95f)))

            for (index in 0 until barCount) {
                val idle = idlePattern[index % idlePattern.size]
                val primary = sin((phase * 3.57f) - index * 0.46f)
                val secondary = sin((phase * 1.86f) - index * 0.24f)
                val active = (((primary + secondary + 2f) / 4f).coerceIn(0f, 1f)).pow(1.25f).coerceIn(0.10f, 1f)
                val amplitude = idle + (active - idle) * level
                val barHeight = (10.dp.toPx() + amplitude * 78.dp.toPx()).coerceAtMost(size.height)
                drawRoundRect(
                    brush = brush,
                    topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2f),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f),
                )
            }
        }
    }
}

// MARK: - Progress & status cards

@Composable
fun TurnProgressSection(currentTurn: Int, maxTurns: Int, progress: Float, tint: Color) {
    Column(modifier = Modifier.padding(horizontal = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "Turn $currentTurn of $maxTurns",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$currentTurn / $maxTurns",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
            color = tint,
            trackColor = tint.copy(alpha = 0.18f),
        )
    }
}

@Composable
fun WaitingForExaminerCard(isRetrying: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SuccessGreen.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.22f)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp, color = SuccessGreen)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                if (isRetrying) "Reconnecting…" else "Waiting for examiner response…",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (isRetrying) "Trying to reach the examiner again." else "Your response has been submitted and is being reviewed.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun ResponseRecordedCard() {
    OBCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(20.dp))
            Text(
                "Response Recorded",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = SuccessGreen,
            )
        }
    }
}

@Composable
fun DebriefPreparationCard(
    isPreparingDebrief: Boolean,
    isRetryingDebrief: Boolean,
    errorMessage: String?,
    onTryAgain: () -> Unit,
    onNewCase: () -> Unit,
) {
    OBCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (isPreparingDebrief) {
                CircularProgressIndicator()
                Text("Case complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "Preparing your debrief…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (isRetryingDebrief) {
                    Text(
                        "Reconnecting to generate feedback.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
                Text("Unable to load debrief", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    errorMessage ?: "Please check your connection and try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "You can try again or begin a fresh case.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onTryAgain) { Text("Try Again") }
                    OutlinedButton(onClick = onNewCase) { Text("New Case") }
                }
            }
        }
    }
}

/** Color of the turn progress bar: green early, yellow mid-case, red near the limit. */
fun turnProgressTint(progress: Float): Color = when {
    progress < 0.3f -> SuccessGreen
    progress <= 0.8f -> Color(0xFFE8B100)
    else -> CriticalRed
}

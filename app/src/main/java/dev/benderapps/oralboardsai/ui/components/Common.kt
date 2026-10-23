package dev.benderapps.oralboardsai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benderapps.oralboardsai.data.InteractionMode
import dev.benderapps.oralboardsai.ui.theme.SuccessGreen
import dev.benderapps.oralboardsai.ui.theme.oralBoards

/** "max_turns_reached" -> "Max Turns Reached". */
fun humanReadable(value: String): String =
    value.replace("_", " ")
        .split(" ")
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word -> word.first().uppercase() + word.drop(1).lowercase() }

/** Rounded surface on the card tone, the building block of most screens. */
@Composable
fun OBCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.oralBoards.card)
            .padding(padding),
        content = content,
    )
}

/** Card with a faint outline, used on the debrief/dashboard screens. */
@Composable
fun OutlinedOBCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.oralBoards.card)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                RoundedCornerShape(20.dp),
            )
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun StatusPill(text: String, tint: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        color = tint,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
fun MetricTile(
    value: String,
    label: String,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(tint.copy(alpha = 0.08f))
            .border(BorderStroke(1.dp, tint.copy(alpha = 0.14f)), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color,
    badgeTitle: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.size(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        StatusPill(text = badgeTitle, tint = tint)
    }
}

@Composable
fun BulletText(text: String, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = tint.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurface)
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Two-segment Text/Speech toggle with a green selected pill. */
@Composable
fun PillSegmentedPicker(
    title: String,
    subtitle: String?,
    selection: InteractionMode,
    onSelect: (InteractionMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (subtitle != null) {
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Row(
            modifier = Modifier
                .size(width = 172.dp, height = 43.dp)
                .clip(CircleShape)
                .background(MaterialTheme.oralBoards.cardInset)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            InteractionMode.entries.forEach { mode ->
                val selected = mode == selection
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(37.dp)
                        .clip(CircleShape)
                        .background(if (selected) SuccessGreen else Color.Transparent)
                        .clickable { onSelect(mode) },
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = if (mode == InteractionMode.Speech) Icons.Rounded.Mic else Icons.Rounded.Keyboard,
                        contentDescription = null,
                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(
                        text = mode.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

/** A labelled section divider used by the sheets. */
@Composable
fun SheetTitle(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        if (subtitle != null) {
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Subtle "Report a problem" link shown next to AI-generated content. */
@Composable
fun ReportProblemButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.TextButton(onClick = onClick, modifier = modifier) {
        Icon(
            androidx.compose.material.icons.Icons.Rounded.Flag,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
        )
        androidx.compose.foundation.layout.Spacer(Modifier.size(6.dp))
        Text("Report a problem", fontSize = 13.sp)
    }
}

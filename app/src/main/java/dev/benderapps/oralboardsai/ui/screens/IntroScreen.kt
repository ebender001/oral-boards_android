package dev.benderapps.oralboardsai.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.benderapps.oralboardsai.R
import dev.benderapps.oralboardsai.appContainer
import dev.benderapps.oralboardsai.data.Specialty
import dev.benderapps.oralboardsai.ui.theme.BrandBlue
import dev.benderapps.oralboardsai.ui.theme.BrandBlueLight
import dev.benderapps.oralboardsai.ui.theme.BrandNavy
import dev.benderapps.oralboardsai.ui.theme.oralBoards

private const val DEMO_VIDEO_URL = "https://benderapps.dev/videos/OralBoardsDemo.mp4"
private const val DISCLAIMER_URL = "https://oboardsai.com/disclaimer/"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntroScreen(
    onStartCase: (Specialty) -> Unit,
    onOpenProgress: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val preferences = container.preferences
    val savedCases by container.performanceStore.cases.collectAsState()
    val uriHandler = LocalUriHandler.current

    // Persist the last chosen specialty so repeated launches jump straight back
    // into the same oral board track.
    var selectedSpecialty by remember { mutableStateOf(preferences.selectedSpecialty) }
    var showSpecialtySheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showDisclaimerSheet by remember { mutableStateOf(!preferences.hasSeenEducationalUseDisclaimer) }
    var showHowItWorks by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var permissionRationale by remember { mutableStateOf(false) }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            onStartCase(selectedSpecialty)
        } else {
            permissionRationale = true
            showPermissionDialog = true
        }
    }

    fun startCase() {
        val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            onStartCase(selectedSpecialty)
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isDark = MaterialTheme.oralBoards.isDark

    Box(modifier = Modifier.fillMaxSize()) {
        if (!isDark) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFFEBF7FF), Color(0xFFF7FAFF), Color.White),
                        )
                    )
            )
            Image(
                painter = painterResource(R.drawable.intro_anatomy_bg),
                contentDescription = null,
                modifier = Modifier.fillMaxSize().alpha(0.6f),
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopEnd,
            )
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.18f)))
        } else {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .padding(top = 28.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    HeaderSection(headerTitle = selectedSpecialty.displayName, isDark = isDark)

                    ActionButtonsSection(
                        specialty = selectedSpecialty,
                        startTitle = if (preferences.hasStartedFirstCase) "Start New Case" else "Start Your First Case",
                        hasSavedCases = savedCases.isNotEmpty(),
                        onSelectSpecialty = { showSpecialtySheet = true },
                        onStart = ::startCase,
                        onOpenProgress = onOpenProgress,
                    )

                    HowItWorksSection(
                        expanded = showHowItWorks,
                        onToggle = { showHowItWorks = !showHowItWorks },
                        onOpenDemoVideo = { uriHandler.openUri(DEMO_VIDEO_URL) },
                    )
                }

                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(top = 12.dp, end = 12.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.oralBoards.card.copy(alpha = 0.9f)),
                ) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Settings",
                        tint = BrandBlue,
                    )
                }
            }

            DisclaimerFooter(onOpen = { uriHandler.openUri(DISCLAIMER_URL) })
        }
    }

    if (showSpecialtySheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpecialtySheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            SpecialtySelectionSheet(
                selected = selectedSpecialty,
                onSelect = { specialty ->
                    selectedSpecialty = specialty
                    preferences.selectedSpecialty = specialty
                    showSpecialtySheet = false
                },
            )
        }
    }

    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            SettingsSheet()
        }
    }

    if (showDisclaimerSheet) {
        ModalBottomSheet(
            onDismissRequest = { showDisclaimerSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.oralBoards.grouped,
        ) {
            EducationalUseDisclaimerSheet(
                onContinue = {
                    preferences.hasSeenEducationalUseDisclaimer = true
                    showDisclaimerSheet = false
                },
            )
        }
    }

    if (showPermissionDialog) {
        val canAskAgain = (context as? android.app.Activity)
            ?.shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO) == true
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            title = { Text("Permissions Required") },
            text = {
                Text(
                    if (canAskAgain || !permissionRationale) {
                        "Microphone access is required to record your spoken responses."
                    } else {
                        "Microphone access is still required to record your spoken responses. " +
                            "In Settings, enable Microphone."
                    }
                )
            },
            confirmButton = {
                if (!canAskAgain) {
                    TextButton(onClick = {
                        showPermissionDialog = false
                        openAppSettings(context)
                    }) { Text("Open Settings") }
                } else {
                    TextButton(onClick = { showPermissionDialog = false }) { Text("OK") }
                }
            },
            dismissButton = if (!canAskAgain) {
                { TextButton(onClick = { showPermissionDialog = false }) { Text("OK") } }
            } else {
                null
            },
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}

// MARK: - Sections

@Composable
private fun HeaderSection(headerTitle: String, isDark: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .shadow(16.dp, CircleShape, ambientColor = BrandBlue, spotColor = BrandBlue)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF0D8CFF), BrandBlueLight))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
            }
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 2.dp)
                    .size(28.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.Chat,
                    contentDescription = null,
                    tint = Color(0xFF2E52B8),
                    modifier = Modifier.size(14.dp),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = headerTitle,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                color = if (isDark) MaterialTheme.colorScheme.onBackground else BrandNavy,
            )
            Text(
                text = "Oral Exam Prep",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                color = BrandBlue,
            )
            Text(
                text = "Practice high-stakes, case-based decision making in a realistic oral board format.",
                modifier = Modifier.padding(top = 4.dp),
                fontFamily = FontFamily.Serif,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionButtonsSection(
    specialty: Specialty,
    startTitle: String,
    hasSavedCases: Boolean,
    onSelectSpecialty: () -> Unit,
    onStart: () -> Unit,
    onOpenProgress: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 4.dp)) {
        Text(
            "Specialty",
            modifier = Modifier.padding(start = 2.dp),
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // Specialty picker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.oralBoards.card.copy(alpha = 0.86f))
                .border(BorderStroke(1.5.dp, BrandBlue.copy(alpha = 0.28f)), RoundedCornerShape(16.dp))
                .clickable(onClick = onSelectSpecialty)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null, tint = BrandBlue)
            Text(
                text = specialty.displayName,
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = BrandBlue,
                maxLines = 1,
            )
            Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = BrandBlue.copy(alpha = 0.85f))
        }

        // Primary CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(10.dp, RoundedCornerShape(18.dp), ambientColor = BrandBlue, spotColor = BrandBlue)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF0D73FF), Color(0xFF2EADFF))))
                .clickable(onClick = onStart),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color.White)
                Text(
                    startTitle,
                    color = Color.White,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                )
            }
            Text(
                "Takes ~2-3 minutes",
                color = Color.White.copy(alpha = 0.82f),
                fontFamily = FontFamily.Serif,
                fontSize = 12.sp,
            )
        }

        if (hasSavedCases) {
            ProgressDashboardCard(onClick = onOpenProgress)
        }
    }
}

@Composable
private fun ProgressDashboardCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.oralBoards.card.copy(alpha = 0.9f))
            .border(BorderStroke(1.dp, BrandBlue.copy(alpha = 0.14f)), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ShowChart, contentDescription = null, tint = Color(0xFF1457B8))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "Your Progress",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 19.sp,
            )
            Text(
                "Review past cases and trends",
                fontFamily = FontFamily.Serif,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Icon(
            Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp),
        )
    }
}

@Composable
private fun HowItWorksSection(
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenDemoVideo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.oralBoards.card.copy(alpha = 0.82f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                RoundedCornerShape(24.dp),
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = BrandBlue)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    "What to expect",
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                )
                Text(
                    "One quick case, dynamic follow-ups, then structured feedback.",
                    fontFamily = FontFamily.Serif,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(0.75f),
            )
        }

        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                HorizontalDivider()
                FeatureRow(Icons.Rounded.LocalHospital, "Case-based simulation", "A clinical case is presented by the examiner.")
                HorizontalDivider()
                FeatureRow(Icons.AutoMirrored.Rounded.Chat, "Respond your way", "You respond verbally or by text.")
                HorizontalDivider()
                FeatureRow(Icons.Rounded.Groups, "Dynamic follow-ups", "The examiner asks follow-up questions based on your answers.")
                HorizontalDivider()
                FeatureRow(Icons.Rounded.BarChart, "Assessed in real time", "You are assessed on recognition, management, and surgical judgment.")
                HorizontalDivider()
                FeatureRow(Icons.Rounded.WorkspacePremium, "Structured feedback", "At the end, you receive detailed, structured feedback.")
                HorizontalDivider()
                FeatureRow(
                    icon = Icons.Rounded.PlayCircle,
                    title = "Watch the demo",
                    subtitle = "See a short walkthrough of a full case.",
                    onClick = onOpenDemoVideo,
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(BrandBlue.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(
                subtitle,
                fontFamily = FontFamily.Serif,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DisclaimerFooter(onOpen: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.clickable(onClick = onOpen),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF576170), modifier = Modifier.size(16.dp))
            Text(
                "Educational use only",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color(0xFF576170),
            )
        }
        Text(
            "For educational purposes only.",
            fontFamily = FontFamily.Serif,
            fontSize = 12.sp,
            color = Color(0xFF878F9E),
        )
    }
}

// MARK: - Sheets

@Composable
private fun SpecialtySelectionSheet(selected: Specialty, onSelect: (Specialty) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Select Specialty", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                "Choose the case pool you want to practice.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Specialty.entries.forEach { specialty ->
            val isSelected = specialty == selected
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.oralBoards.card)
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isSelected) BrandBlue.copy(alpha = 0.45f) else Color.Gray.copy(alpha = 0.15f),
                        ),
                        RoundedCornerShape(18.dp),
                    )
                    .clickable { onSelect(specialty) }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clip(CircleShape).background(BrandBlue.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ListAlt, contentDescription = null, tint = BrandBlue, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(specialty.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        specialty.selectionDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isSelected) {
                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = BrandBlue)
                }
            }
        }
    }
}

@Composable
private fun EducationalUseDisclaimerSheet(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = Color(0xFF576170))
            Text(
                "Educational Use Disclaimer",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = Color(0xFF576170),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(
                "• Oral Exam Prep is for educational and exam-preparation purposes only.",
                "• It does not provide medical advice, clinical guidance, diagnosis, or treatment recommendations.",
                "• Some case scenarios, prompts, feedback, and responses may be AI-generated and may be incomplete or inaccurate.",
                "• You can view the full Educational Use Disclaimer anytime from the footer link.",
            ).forEach { line ->
                Text(
                    line,
                    fontFamily = FontFamily.Serif,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3D3D45)),
        ) {
            Text("Continue")
        }
    }
}

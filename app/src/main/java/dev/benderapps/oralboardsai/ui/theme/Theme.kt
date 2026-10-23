package dev.benderapps.oralboardsai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF3D3D45),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF111114),
    surface = Color.White,
    onSurface = Color(0xFF111114),
    surfaceVariant = Color(0xFFE9E9EF),
    onSurfaceVariant = Color(0xFF5E5E68),
    outline = Color(0xFFC6C6CC),
    outlineVariant = Color(0xFFE0E0E5),
    error = CriticalRed,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4C9BFF),
    onPrimary = Color.White,
    secondary = Color(0xFFD0D0D8),
    background = Color(0xFF000000),
    onBackground = Color(0xFFF2F2F7),
    surface = Color(0xFF1C1C1E),
    onSurface = Color(0xFFF2F2F7),
    surfaceVariant = Color(0xFF2C2C2E),
    onSurfaceVariant = Color(0xFFA0A0AA),
    outline = Color(0xFF48484A),
    outlineVariant = Color(0xFF38383A),
    error = Color(0xFFFF6369),
)

/**
 * Extra surface tones that mirror the iOS grouped-background hierarchy: the page
 * sits on [grouped] and cards on [card]; [cardInset] is for content nested inside a card.
 */
@Immutable
data class OralBoardsColors(
    val grouped: Color,
    val card: Color,
    val cardInset: Color,
    val isDark: Boolean,
)

val LocalOralBoardsColors = staticCompositionLocalOf {
    OralBoardsColors(
        grouped = Color(0xFFF2F2F7),
        card = Color.White,
        cardInset = Color(0xFFF7F7FA),
        isDark = false,
    )
}

val MaterialTheme.oralBoards: OralBoardsColors
    @Composable get() = LocalOralBoardsColors.current

@Composable
fun OralBoardsAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) {
        OralBoardsColors(
            grouped = Color(0xFF000000),
            card = Color(0xFF1C1C1E),
            cardInset = Color(0xFF2C2C2E),
            isDark = true,
        )
    } else {
        OralBoardsColors(
            grouped = Color(0xFFF2F2F7),
            card = Color.White,
            cardInset = Color(0xFFF7F7FA),
            isDark = false,
        )
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalOralBoardsColors provides colors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = Typography,
            content = content,
        )
    }
}

package io.github.flagrant99.romulist.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80,

    // Background of the whole screen
    background = Black,

    // Color of the "Paper" or "Cards"
    surface = Color(0xFF1C1B1F),

    // THE TEXT COLOR (This is what you likely want to change)
    onBackground = Green,
    onSurface = Green,

    // Subtitle/Small text color
    onSurfaceVariant = Green
)

@Composable
fun RomulistTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
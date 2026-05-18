package com.scanfolio.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UpRed = Color(0xFFE53935)
val DownGreen = Color(0xFF4CAF50)
val UpRedLight = Color(0xFFFFCDD2)
val DownGreenLight = Color(0xFFC8E6C9)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3a7bd1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFd8e9fb),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = Color(0xFF7380a9),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFe8ecf4),
    onSecondaryContainer = Color(0xFF1C313A),
    tertiary = Color(0xFFa1a7d9),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFe8eaf6),
    onTertiaryContainer = Color(0xFF1a1b3a),
    surface = Color(0xFFf4f7fb),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = Color.White,
    onSurfaceVariant = Color(0xFF49454F),
    background = Color(0xFFf4f7fb),
    onBackground = Color(0xFF1C1B1F),
    error = Color(0xFFE53935),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = Color(0xFFE0E0E0),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF83b6ed),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF00497D),
    onPrimaryContainer = Color(0xFFd8e9fb),
    secondary = Color(0xFF90A4AE),
    onSecondary = Color(0xFF1C313A),
    secondaryContainer = Color(0xFF37474F),
    onSecondaryContainer = Color(0xFFCFD8DC),
    tertiary = Color(0xFFc2a48a),
    onTertiary = Color(0xFF3a2a1a),
    tertiaryContainer = Color(0xFF544030),
    onTertiaryContainer = Color(0xFFe8d5c4),
    surface = Color(0xFF0d0d11),
    onSurface = Color(0xFFf6f0ea),
    surfaceVariant = Color(0xFF1a1a1e),
    onSurfaceVariant = Color(0xFFCAC4D0),
    background = Color(0xFF0d0d11),
    onBackground = Color(0xFFf6f0ea),
    error = Color(0xFFEF9A9A),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = Color(0xFF333333),
)

@Composable
fun MomentumTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}

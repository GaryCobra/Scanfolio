package com.scanfolio.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PrimaryColor = Color(0xFF1565C0)
private val OnPrimaryColor = Color(0xFFFFFFFF)
private val PrimaryContainerColor = Color(0xFFD1E4FF)
private val SecondaryColor = Color(0xFF546E7A)

private val PrimaryColorDark = Color(0xFF90CAF9)
private val OnPrimaryColorDark = Color(0xFF0D47A1)
private val PrimaryContainerColorDark = Color(0xFF1565C0)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryColor,
    onPrimary = OnPrimaryColor,
    primaryContainer = PrimaryContainerColor,
    secondary = SecondaryColor,
)

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryColorDark,
    onPrimary = OnPrimaryColorDark,
    primaryContainer = PrimaryContainerColorDark,
)

@Composable
fun ScanfolioTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(colorScheme = colorScheme, content = content)
}

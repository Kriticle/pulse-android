package com.pulse.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Purple400,
    onPrimary = Light100,
    primaryContainer = Purple700,
    onPrimaryContainer = Purple200,
    secondary = Pink400,
    onSecondary = Light100,
    secondaryContainer = Pink700,
    onSecondaryContainer = Pink200,
    background = Dark900,
    onBackground = Light100,
    surface = Dark800,
    onSurface = Light100,
    surfaceVariant = Dark700,
    onSurfaceVariant = Light400,
    outline = Dark500,
    error = ErrorRed,
    onError = Light100
)

private val LightColorScheme = lightColorScheme(
    primary = Purple600,
    onPrimary = Light100,
    primaryContainer = Purple200,
    onPrimaryContainer = Purple700,
    secondary = Pink600,
    onSecondary = Light100,
    secondaryContainer = Pink200,
    onSecondaryContainer = Pink700,
    background = Light200,
    onBackground = Dark900,
    surface = Light100,
    onSurface = Dark900,
    surfaceVariant = Light300,
    onSurfaceVariant = Dark600,
    outline = Light400,
    error = ErrorRed,
    onError = Light100
)

@Composable
fun PulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
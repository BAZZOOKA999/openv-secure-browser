package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OpenVBlue,
    onPrimary = OpenVTextPrimary,
    primaryContainer = OpenVSurfaceVariant,
    onPrimaryContainer = OpenVBlueHover,
    secondary = OpenVShieldGreen,
    onSecondary = OpenVBackground,
    secondaryContainer = OpenVSurfaceVariant,
    onSecondaryContainer = OpenVShieldGreen,
    tertiary = OpenVShieldGreen,
    onTertiary = OpenVBackground,
    background = OpenVBackground,
    onBackground = OpenVTextPrimary,
    surface = OpenVSurface,
    onSurface = OpenVTextPrimary,
    surfaceVariant = OpenVSurfaceVariant,
    onSurfaceVariant = OpenVTextSecondary,
    outline = OpenVBorder,
    outlineVariant = OpenVBorderSubtle,
    error = OpenVAlertRed,
    onError = OpenVTextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = OpenVBlue,
    onPrimary = OpenVTextPrimary,
    primaryContainer = OpenVLightSurfaceVariant,
    onPrimaryContainer = OpenVBlue,
    secondary = OpenVShieldGreen,
    onSecondary = OpenVTextPrimary,
    background = OpenVLightBackground,
    onBackground = OpenVLightTextPrimary,
    surface = OpenVLightSurface,
    onSurface = OpenVLightTextPrimary,
    surfaceVariant = OpenVLightSurfaceVariant,
    onSurfaceVariant = OpenVLightTextSecondary,
    outline = OpenVLightBorder,
    error = OpenVAlertRed,
    onError = OpenVTextPrimary
)

@Composable
fun OpenVTheme(
    darkTheme: Boolean = true, // OpenV default dark futuristic style
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    OpenVTheme(darkTheme = darkTheme, content = content)
}

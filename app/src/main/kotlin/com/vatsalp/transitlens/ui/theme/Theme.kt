package com.vatsalp.transitlens.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = TransitBlueLight,
    onPrimary = OnLight,
    background = DarkSurface,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDark,
    secondary = Amber,
    onSecondary = OnLight,
)

private val LightColors = lightColorScheme(
    primary = TransitBlue,
    onPrimary = OnDark,
    background = LightSurface,
    onBackground = OnLight,
    surface = LightSurface,
    onSurface = OnLight,
    secondary = TransitBlue,
    onSecondary = OnDark,
)

@Composable
fun TransitLensTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = TransitTypography,
        content = content,
    )
}

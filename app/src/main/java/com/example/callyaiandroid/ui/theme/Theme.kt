package com.example.callyaiandroid.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme: ColorScheme = lightColorScheme(
    primary = BlackPrimary,
    onPrimary = GreyOnPrimary,
    primaryContainer = BlackPrimary,
    onPrimaryContainer = GreyOnPrimary,
    secondary = BlackPrimary,
    onSecondary = GreyOnPrimary,

    background = LightSurface,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    outline = LightOutline
)

private val DarkScheme: ColorScheme = darkColorScheme(
    primary = WhitePrimary,
    onPrimary = BlackOnPrimary,
    primaryContainer = WhitePrimary,
    onPrimaryContainer = BlackOnPrimary,
    secondary = WhitePrimary,
    onSecondary = BlackOnPrimary,

    background = DarkSurface,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    outline = DarkOutline
)

@Composable
fun AppTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}

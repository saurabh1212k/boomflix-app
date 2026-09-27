package com.example.boomflix.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BoomflixDarkColorScheme = darkColorScheme(
    primary = BoomflixRed,
    onPrimary = BoomflixWhite,
    secondary = BoomflixDarkRed,
    onSecondary = BoomflixWhite,
    background = BoomflixBlack,
    onBackground = BoomflixWhite,
    surface = BoomflixDarkSurface,
    onSurface = BoomflixWhite,
    surfaceVariant = BoomflixCardSurface,
    onSurfaceVariant = BoomflixLightGray,
    error = Color(0xFFCF6679),
    onError = BoomflixBlack,
)

@Composable
fun BOOMFLIXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BoomflixDarkColorScheme,
        typography = Typography,
        content = content
    )
}

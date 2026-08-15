package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GameDarkColorScheme = darkColorScheme(
    primary = FireOrange,
    secondary = NeonOrange,
    tertiary = GoldYellow,
    background = DarkGrey,
    surface = CarbonGrey,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DarkGrey,
    onBackground = Color.White,
    onSurface = Color.White,
    error = BloodRed
)

private val GameLightColorScheme = lightColorScheme(
    primary = FireOrange,
    secondary = NeonOrange,
    tertiary = GoldYellow,
    background = DarkGrey, // Force dark theme for the gaming vibe!
    surface = CarbonGrey,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = DarkGrey,
    onBackground = Color.White,
    onSurface = Color.White,
    error = BloodRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for gaming vibe
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce the rigid orange/red brand style
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) GameDarkColorScheme else GameLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

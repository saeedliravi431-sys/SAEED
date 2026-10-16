package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepNavy,
    primaryContainer = ElectricBlue,
    onPrimaryContainer = Color.White,
    secondary = CopperOrange,
    onSecondary = Color.Black,
    secondaryContainer = NavyCard,
    onSecondaryContainer = CopperAmber,
    tertiary = RegenGreen,
    onTertiary = Color.White,
    background = DeepNavy,
    onBackground = TextPrimaryDark,
    surface = NavySurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = NavyCard,
    onSurfaceVariant = TextSecondaryDark,
    outline = NavyCardBorder,
    error = ThermalRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = ElectricBlueLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = CopperOrange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFEDD5),
    onSecondaryContainer = Color(0xFFC2410C),
    tertiary = RegenGreen,
    onTertiary = Color.White,
    background = LightSurface,
    onBackground = TextPrimaryLight,
    surface = LightCard,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TextSecondaryLight,
    outline = LightCardBorder,
    error = ThermalRed,
    onError = Color.White
)

@Composable
fun EVPowertrainTheme(
    darkTheme: Boolean = true, // Default to sleek engineering dark mode for high visual contrast
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

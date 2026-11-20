package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = EmeraldDeep,
    onPrimary = Color.White,
    primaryContainer = EmeraldSoft,
    onPrimaryContainer = EmeraldDeep,
    secondary = GoldRich,
    onSecondary = Color.White,
    secondaryContainer = GoldSoft,
    onSecondaryContainer = InkDark,
    tertiary = EmeraldBright,
    onTertiary = Color.White,
    background = IvoryBackground,
    onBackground = InkDark,
    surface = CardWhite,
    onSurface = InkDark,
    surfaceVariant = CreamSurface,
    onSurfaceVariant = MutedSlate,
    outline = BorderSand,
    outlineVariant = BorderSand,
    error = StatusRejected,
    onError = Color.White
)

private val DarkColors = darkColorScheme(
    primary = GoldBright,
    onPrimary = InkDark,
    primaryContainer = EmeraldDeep,
    onPrimaryContainer = EmeraldSoft,
    secondary = EmeraldBright,
    onSecondary = Color.White,
    secondaryContainer = DarkCard,
    onSecondaryContainer = GoldBright,
    tertiary = GoldRich,
    onTertiary = InkDark,
    background = DarkObsidian,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkCard,
    onSurfaceVariant = DarkMuted,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = Color(0xFFEF5350),
    onError = InkDark
)

val GideonShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GideonGlobalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = GideonTypography,
        shapes = GideonShapes,
        content = content
    )
}

package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestEmerald,
    onPrimary = CreamSurface,
    primaryContainer = SoftSageContainer,
    onPrimaryContainer = OnSoftSage,
    secondary = TurmericAmber,
    onSecondary = CreamSurface,
    secondaryContainer = WarmAmberContainer,
    onSecondaryContainer = OnWarmAmber,
    tertiary = TerracottaClay,
    onTertiary = CreamSurface,
    tertiaryContainer = TerracottaContainer,
    onTertiaryContainer = OnTerracotta,
    background = SunlitParchment,
    onBackground = DeepSoilInk,
    surface = CreamSurface,
    onSurface = DeepSoilInk,
    surfaceVariant = WarmFieldVariant,
    onSurfaceVariant = MutedMossInk,
    outline = SubtleFieldOutline,
    outlineVariant = WarmFieldVariant,
    error = LossRed,
    onError = CreamSurface,
    errorContainer = LossRedBg,
    onErrorContainer = LossRed
)

private val DarkColorScheme = darkColorScheme(
    primary = BrightEmeraldDark,
    onPrimary = OnSoftSage,
    primaryContainer = DarkSageContainer,
    onPrimaryContainer = SoftSageContainer,
    secondary = SoftGoldDark,
    onSecondary = OnWarmAmber,
    secondaryContainer = DarkAmberContainer,
    onSecondaryContainer = WarmAmberContainer,
    tertiary = SoftGoldDark,
    onTertiary = OnTerracotta,
    tertiaryContainer = DarkAmberContainer,
    onTertiaryContainer = TerracottaContainer,
    background = DeepSoilBackground,
    onBackground = CreamInkDark,
    surface = DarkMossSurface,
    onSurface = CreamInkDark,
    surfaceVariant = DarkCardVariant,
    onSurfaceVariant = MutedSageDark,
    outline = DarkOutline,
    outlineVariant = DarkCardVariant,
    error = LossRedDark,
    onError = LossRedBgDark,
    errorContainer = LossRedBgDark,
    onErrorContainer = LossRedDark
)

val AgriMitraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AgriMitraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AgriMitraTypography,
        shapes = AgriMitraShapes,
        content = content
    )
}

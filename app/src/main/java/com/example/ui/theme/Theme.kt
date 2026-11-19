package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = ForestSage,
    onPrimary = Color.White,
    primaryContainer = MintMist,
    onPrimaryContainer = ForestSageDark,
    secondary = DairyAmber,
    onSecondary = Color.White,
    secondaryContainer = DairyAmberBg,
    onSecondaryContainer = DairyAmberInk,
    tertiary = HiddenRiskPlum,
    onTertiary = Color.White,
    tertiaryContainer = HiddenRiskBg,
    onTertiaryContainer = HiddenRiskInk,
    error = MammalDangerRed,
    onError = Color.White,
    errorContainer = MammalDangerBg,
    onErrorContainer = MammalDangerInk,
    background = LinenIvory,
    onBackground = CharcoalInk,
    surface = PureCardWhite,
    onSurface = CharcoalInk,
    surfaceVariant = WarmParchment,
    onSurfaceVariant = SlateSage,
    outline = WarmHairline,
    outlineVariant = Color(0xFFE7E2D8)
)

private val DarkColorScheme = darkColorScheme(
    primary = SageGlow,
    onPrimary = Color(0xFF062E1F),
    primaryContainer = DarkSafeBg,
    onPrimaryContainer = Color(0xFFC6F7DF),
    secondary = DarkDairyAmber,
    onSecondary = Color(0xFF3A2200),
    secondaryContainer = DarkDairyBg,
    onSecondaryContainer = Color(0xFFFFE7C4),
    tertiary = DarkHiddenPlum,
    onTertiary = Color(0xFF28193B),
    tertiaryContainer = DarkHiddenBg,
    onTertiaryContainer = Color(0xFFEBDDFC),
    error = DarkMammalDanger,
    onError = Color(0xFF4A0D07),
    errorContainer = DarkMammalBg,
    onErrorContainer = Color(0xFFFFDAD5),
    background = PineNightBg,
    onBackground = IvoryText,
    surface = PineNightSurface,
    onSurface = IvoryText,
    surfaceVariant = PineNightSurfaceHigh,
    onSurfaceVariant = MutedSageText,
    outline = Color(0xFF33453D),
    outlineVariant = Color(0xFF25342D)
)

val GalSafeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun GalSafeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val allergenColors = if (darkTheme) DarkAllergenColors else LightAllergenColors

    CompositionLocalProvider(LocalAllergenColors provides allergenColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = GalSafeTypography,
            shapes = GalSafeShapes,
            content = content
        )
    }
}

package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Crisp Botanical Apothecary Palette
val ForestSage = Color(0xFF1D5B45)
val ForestSageDark = Color(0xFF0E3829)
val MintMist = Color(0xFFCDE9DA)
val BotanicalEmerald = Color(0xFF1E7E58)

val LinenIvory = Color(0xFFFAF8F4)
val WarmParchment = Color(0xFFF1EDE4)
val PureCardWhite = Color(0xFFFFFFFF)
val CharcoalInk = Color(0xFF19211D)
val SlateSage = Color(0xFF53615A)
val WarmHairline = Color(0xFFD9D3C7)

// Allergen Semantic Colors — Light
val MammalDangerRed = Color(0xFFC63D2F)
val MammalDangerBg = Color(0xFFFCE3DF)
val MammalDangerInk = Color(0xFF6B150D)

val DairyAmber = Color(0xFFC97A16)
val DairyAmberBg = Color(0xFFFDEBD0)
val DairyAmberInk = Color(0xFF5A3100)

val HiddenRiskPlum = Color(0xFF6B4C8C)
val HiddenRiskBg = Color(0xFFEDE6F6)
val HiddenRiskInk = Color(0xFF392352)

val GlutenCobalt = Color(0xFF2858A8)
val GlutenCobaltBg = Color(0xFFDCE8FC)
val GlutenCobaltInk = Color(0xFF102A57)

val SafeLeafGreen = Color(0xFF1E7E58)
val SafeLeafBg = Color(0xFFDDF3E8)
val SafeLeafInk = Color(0xFF0C3E2A)

// Dark Theme Tokens
val PineNightBg = Color(0xFF0E1512)
val PineNightSurface = Color(0xFF16201B)
val PineNightSurfaceHigh = Color(0xFF1F2D26)
val SageGlow = Color(0xFF79D9AE)
val IvoryText = Color(0xFFECF2EE)
val MutedSageText = Color(0xFFA7B8B0)

val DarkMammalDanger = Color(0xFFFF8A7D)
val DarkMammalBg = Color(0xFF4B1914)
val DarkDairyAmber = Color(0xFFFFC16B)
val DarkDairyBg = Color(0xFF472C04)
val DarkHiddenPlum = Color(0xFFCBB2EC)
val DarkHiddenBg = Color(0xFF2F2042)
val DarkGlutenCobalt = Color(0xFF8AB4FF)
val DarkGlutenBg = Color(0xFF172B4D)
val DarkSafeGreen = Color(0xFF75DFB1)
val DarkSafeBg = Color(0xFF113C2B)

@Immutable
data class AllergenColors(
    val mammalBadge: Color,
    val mammalContainer: Color,
    val mammalOnContainer: Color,
    val dairyBadge: Color,
    val dairyContainer: Color,
    val dairyOnContainer: Color,
    val glutenBadge: Color,
    val glutenContainer: Color,
    val glutenOnContainer: Color,
    val hiddenBadge: Color,
    val hiddenContainer: Color,
    val hiddenOnContainer: Color,
    val safeBadge: Color,
    val safeContainer: Color,
    val safeOnContainer: Color
)

val LightAllergenColors = AllergenColors(
    mammalBadge = MammalDangerRed,
    mammalContainer = MammalDangerBg,
    mammalOnContainer = MammalDangerInk,
    dairyBadge = DairyAmber,
    dairyContainer = DairyAmberBg,
    dairyOnContainer = DairyAmberInk,
    glutenBadge = GlutenCobalt,
    glutenContainer = GlutenCobaltBg,
    glutenOnContainer = GlutenCobaltInk,
    hiddenBadge = HiddenRiskPlum,
    hiddenContainer = HiddenRiskBg,
    hiddenOnContainer = HiddenRiskInk,
    safeBadge = SafeLeafGreen,
    safeContainer = SafeLeafBg,
    safeOnContainer = SafeLeafInk
)

val DarkAllergenColors = AllergenColors(
    mammalBadge = DarkMammalDanger,
    mammalContainer = DarkMammalBg,
    mammalOnContainer = Color(0xFFFFDAD5),
    dairyBadge = DarkDairyAmber,
    dairyContainer = DarkDairyBg,
    dairyOnContainer = Color(0xFFFFE7C4),
    glutenBadge = DarkGlutenCobalt,
    glutenContainer = DarkGlutenBg,
    glutenOnContainer = Color(0xFFD6E4FF),
    hiddenBadge = DarkHiddenPlum,
    hiddenContainer = DarkHiddenBg,
    hiddenOnContainer = Color(0xFFEBDDFC),
    safeBadge = DarkSafeGreen,
    safeContainer = DarkSafeBg,
    safeOnContainer = Color(0xFFC6F7DF)
)

val LocalAllergenColors = staticCompositionLocalOf { LightAllergenColors }

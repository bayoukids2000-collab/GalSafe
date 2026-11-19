package com.example.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AllergenCategory {
    MAMMAL_MEAT,
    DAIRY,
    GLUTEN,
    HIDDEN_RISK,
    SAFE
}

@Serializable
enum class SafetyVerdict {
    SAFE,
    CAUTION,
    UNSAFE,
    UNKNOWN
}

@Serializable
data class IngredientInsight(
    val rawName: String,
    val simpleName: String,
    val plainEnglishExplanation: String,
    val category: AllergenCategory,
    val sourceOrigin: String,
    val whyItMattersForAlphaGal: String = ""
)

@Serializable
data class ProductAnalysisResult(
    val barcode: String,
    val productName: String,
    val brand: String,
    val imageUrl: String? = null,
    val rawIngredientsText: String,
    val allergenTagsFromLabel: List<String> = emptyList(),
    val overallVerdict: SafetyVerdict,
    val verdictHeadline: String,
    val verdictSummary: String,
    val mammalMeatTriggers: List<String> = emptyList(),
    val dairyTriggers: List<String> = emptyList(),
    val glutenTriggers: List<String> = emptyList(),
    val hiddenRiskTriggers: List<String> = emptyList(),
    val ingredients: List<IngredientInsight> = emptyList(),
    val isAiEnhanced: Boolean = false,
    val aiErrorMessage: String? = null,
    val scannedAtEpochMs: Long = System.currentTimeMillis(),
    val isBookmarked: Boolean = false
)

data class UserAllergenPreferences(
    val avoidMammalMeat: Boolean = true,
    val avoidDairy: Boolean = true,
    val avoidGluten: Boolean = true,
    val flagHiddenMammalDerivatives: Boolean = true,
    val flagCarrageenan: Boolean = true
)

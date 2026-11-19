package com.example

import com.example.domain.AlphaGalRuleEngine
import com.example.domain.model.AllergenCategory
import com.example.domain.model.SafetyVerdict
import com.example.domain.model.UserAllergenPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AlphaGalRuleEngineTest {

    private val defaultPrefs = UserAllergenPreferences()

    @Test
    fun `flags mammal meat and dairy triggers accurately`() {
        val result = AlphaGalRuleEngine.analyzeLocally(
            barcode = "123456789012",
            productName = "Cheesy Meat Snack",
            brand = "TestBrand",
            imageUrl = null,
            rawIngredientsText = "Wheat flour, beef tallow, whey protein, gelatin, natural flavors, sea salt",
            allergenTagsFromLabel = emptyList(),
            prefs = defaultPrefs
        )

        assertEquals(SafetyVerdict.UNSAFE, result.overallVerdict)
        assertTrue(result.mammalMeatTriggers.any { it.contains("beef tallow", ignoreCase = true) })
        assertTrue(result.mammalMeatTriggers.any { it.contains("gelatin", ignoreCase = true) })
        assertTrue(result.dairyTriggers.any { it.contains("whey", ignoreCase = true) })
        assertTrue(result.hiddenRiskTriggers.any { it.contains("natural flavors", ignoreCase = true) })
    }

    @Test
    fun `does not falsely flag plant milks cocoa butter or lactic acid as dairy`() {
        val result = AlphaGalRuleEngine.analyzeLocally(
            barcode = "000111222333",
            productName = "Dark Chocolate Oat Bar",
            brand = "PlantBrand",
            imageUrl = null,
            rawIngredientsText = "Organic cocoa butter, oat milk, cane sugar, coconut milk, cream of tartar, lactic acid, sunflower lecithin",
            allergenTagsFromLabel = emptyList(),
            prefs = defaultPrefs
        )

        assertEquals(SafetyVerdict.SAFE, result.overallVerdict)
        assertTrue(result.mammalMeatTriggers.isEmpty())
        assertTrue(result.dairyTriggers.isEmpty())
        assertTrue(result.ingredients.all { it.category == AllergenCategory.SAFE })
    }

    @Test
    fun `flags carrageenan when flagCarrageenan is enabled`() {
        val result = AlphaGalRuleEngine.analyzeLocally(
            barcode = "999888777666",
            productName = "Almond Beverage",
            brand = "NutBrand",
            imageUrl = null,
            rawIngredientsText = "Filtered water, almonds, cane sugar, carrageenan, sea salt",
            allergenTagsFromLabel = emptyList(),
            prefs = defaultPrefs.copy(flagCarrageenan = true)
        )

        assertEquals(SafetyVerdict.UNSAFE, result.overallVerdict)
        assertTrue(result.mammalMeatTriggers.any { it.contains("carrageenan", ignoreCase = true) })
    }

    @Test
    fun `flags gluten triggers for celiac disease and oats cross-contact`() {
        val result = AlphaGalRuleEngine.analyzeLocally(
            barcode = "555444333222",
            productName = "Multigrain Crackers",
            brand = "GrainBrand",
            imageUrl = null,
            rawIngredientsText = "Enriched wheat flour, barley malt extract, rye flour, rolled oats, sea salt",
            allergenTagsFromLabel = emptyList(),
            prefs = defaultPrefs.copy(avoidGluten = true)
        )

        assertEquals(SafetyVerdict.UNSAFE, result.overallVerdict)
        assertTrue(result.glutenTriggers.any { it.contains("wheat flour", ignoreCase = true) })
        assertTrue(result.glutenTriggers.any { it.contains("barley malt", ignoreCase = true) })
        assertTrue(result.glutenTriggers.any { it.contains("rye flour", ignoreCase = true) })
        assertTrue(result.hiddenRiskTriggers.any { it.contains("rolled oats", ignoreCase = true) })
    }

    @Test
    fun `does not falsely flag buckwheat or maltodextrin as gluten`() {
        val result = AlphaGalRuleEngine.analyzeLocally(
            barcode = "777666555444",
            productName = "Buckwheat Crisp",
            brand = "CeliacSafe",
            imageUrl = null,
            rawIngredientsText = "Organic buckwheat flour, rice flour, maltodextrin, tapioca starch, sea salt",
            allergenTagsFromLabel = emptyList(),
            prefs = defaultPrefs.copy(avoidGluten = true)
        )

        assertEquals(SafetyVerdict.SAFE, result.overallVerdict)
        assertTrue(result.glutenTriggers.isEmpty())
        assertTrue(result.ingredients.all { it.category == AllergenCategory.SAFE })
    }
}

package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.AlphaGalRuleEngine
import com.example.domain.model.AllergenCategory
import com.example.domain.model.IngredientInsight
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.SafetyVerdict
import com.example.domain.model.UserAllergenPreferences
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

private const val TAG = "AiIngredientAnalyzer"
private const val MODEL_NAME = "gemini-3.8-flash"

class AiIngredientAnalyzer {

    private val ingredientItemSchema = Schema.obj(
        properties = mapOf(
            "rawName" to Schema.string(description = "Exact ingredient name from the label"),
            "simpleName" to Schema.string(description = "Short, everyday plain-English name (2-5 words)"),
            "plainEnglishExplanation" to Schema.string(
                description = "1-2 simple sentences explaining what this ingredient is, what it comes from, and why it is in the food"
            ),
            "category" to Schema.enumeration(
                values = listOf("MAMMAL_MEAT", "DAIRY", "GLUTEN", "HIDDEN_RISK", "SAFE"),
                description = "Allergen risk category for someone avoiding mammal meat (Alpha-Gal), dairy, and gluten (Celiac Disease)"
            ),
            "sourceOrigin" to Schema.string(
                description = "Short origin label, e.g. 'Cattle / Beef', 'Cow Milk (Dairy)', 'Wheat / Barley (Gluten)', 'Plant / Corn', 'Pork / Beef Gelatin'"
            ),
            "whyItMattersForAlphaGal" to Schema.string(
                description = "Brief 1-sentence guidance for Alpha-Gal Syndrome, dairy avoidance, or Celiac Disease"
            )
        )
    )

    private val textAnalysisSchema = Schema.obj(
        properties = mapOf(
            "overallVerdict" to Schema.enumeration(
                values = listOf("SAFE", "CAUTION", "UNSAFE"),
                description = "Overall safety verdict for Alpha-Gal (no mammal meat, no dairy) and Celiac (no gluten)"
            ),
            "verdictHeadline" to Schema.string(description = "Short 4-7 word verdict title"),
            "verdictSummary" to Schema.string(
                description = "Clear 1-2 sentence summary naming any mammal meat, carrageenan, dairy, or ambiguous ingredients found"
            ),
            "ingredients" to Schema.array(
                items = ingredientItemSchema,
                description = "Every ingredient translated into simple plain English and categorized"
            )
        )
    )

    private val photoAnalysisSchema = Schema.obj(
        properties = mapOf(
            "productName" to Schema.string(description = "Product name if visible, otherwise 'Scanned Food Label'"),
            "extractedIngredientsText" to Schema.string(description = "Full verbatim ingredients list read from the photo"),
            "overallVerdict" to Schema.enumeration(
                values = listOf("SAFE", "CAUTION", "UNSAFE")
            ),
            "verdictHeadline" to Schema.string(description = "Short 4-7 word verdict title"),
            "verdictSummary" to Schema.string(description = "Clear 1-2 sentence summary for Alpha-Gal & dairy avoidance"),
            "ingredients" to Schema.array(items = ingredientItemSchema)
        )
    )

    private val textModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = textAnalysisSchema
                temperature = 0.2f
            }
        )
    }

    private val photoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = photoAnalysisSchema
                temperature = 0.2f
            }
        )
    }

    suspend fun enhanceAnalysis(
        baseline: ProductAnalysisResult,
        prefs: UserAllergenPreferences
    ): ProductAnalysisResult {
        if (baseline.rawIngredientsText.isBlank()) {
            return baseline
        }

        val prompt = buildString {
            appendLine("You are a clinical nutrition translator helping a patient with Alpha-Gal Syndrome and Celiac Disease.")
            appendLine("Dietary restrictions:")
            appendLine("- Avoid mammal meat & mammal by-products (beef, pork, lamb, veal, venison, gelatin, lard, tallow, suet, animal rennet, bone broth, lanolin): ${prefs.avoidMammalMeat}")
            appendLine("- Avoid dairy (cow/goat/sheep milk, cream, butter, ghee, cheese, whey, casein, caseinate, lactose, milkfat): ${prefs.avoidDairy}")
            appendLine("- Avoid gluten for Celiac Disease (wheat, enriched/whole/durum/semolina/spelt/kamut/farro flour, vital wheat gluten, barley, malt, malt extract, brewer's yeast, rye, triticale, seitan): ${prefs.avoidGluten}")
            appendLine("- Flag carrageenan (contains alpha-gal epitope): ${prefs.flagCarrageenan}")
            appendLine("- Flag ambiguous/hidden animal or cross-contact additives (natural flavors, glycerin, mono- & diglycerides, stearic acid, magnesium stearate, vitamin D3, non-certified oats, traditional soy sauce): ${prefs.flagHiddenMammalDerivatives}")
            appendLine()
            appendLine("IMPORTANT RULES:")
            appendLine("1. Translate EVERY ingredient into simple, jargon-free plain English anyone can understand.")
            appendLine("2. Do NOT falsely flag plant foods with 'milk', 'butter', or 'wheat' in their name (cocoa butter, peanut butter, coconut milk, oat milk, almond milk, soy milk, cream of tartar, plant-fermented lactic acid, buckwheat, corn/rice maltodextrin) — explain clearly that they are SAFE.")
            appendLine("3. Classify gelatin, collagen, lard, tallow, beef/pork/lamb, and carrageenan (if enabled) as MAMMAL_MEAT.")
            appendLine("4. Classify milk, whey, casein, sodium caseinate, butter, ghee, lactose, and cheese as DAIRY.")
            appendLine("5. Classify wheat, barley, rye, malt, brewer's yeast, triticale, spelt, semolina, and seitan (if enabled) as GLUTEN.")
            appendLine("6. Classify uncertified oats or traditional soy sauce as HIDDEN_RISK when avoiding gluten.")
            appendLine("7. Keep the rawName close to the label wording so it matches the package.")
            appendLine()
            appendLine("Product: ${baseline.productName} (${baseline.brand})")
            if (baseline.allergenTagsFromLabel.isNotEmpty()) {
                appendLine("Package allergen tags: ${baseline.allergenTagsFromLabel.joinToString(", ")}")
            }
            appendLine("Ingredients list:")
            appendLine(baseline.rawIngredientsText)
        }

        return try {
            val response = withTimeout(45_000L) {
                textModel.generateContent(prompt)
            }
            val jsonText = response.text
                ?: throw IllegalStateException("AI returned an empty response.")
            val root = JSONObject(jsonText)
            mergeAiResponse(baseline, root, prefs)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI ingredient analysis timed out", e)
            baseline.copy(
                aiErrorMessage = "AI plain-English analysis timed out. Showing instant local allergen check."
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI ingredient analysis failed", e)
            baseline.copy(
                aiErrorMessage = e.localizedMessage
                    ?: "Could not reach Gemini AI right now. Showing instant local allergen check."
            )
        }
    }

    suspend fun analyzeLabelPhoto(
        bitmap: Bitmap,
        prefs: UserAllergenPreferences
    ): Result<ProductAnalysisResult> {
        val prompt = buildString {
            appendLine("Read the food ingredients list from this photo and analyze it for someone with Alpha-Gal Syndrome and Celiac Disease who avoids mammal meat (and carrageenan), dairy, and gluten.")
            appendLine("Settings: avoidMammalMeat=${prefs.avoidMammalMeat}, avoidDairy=${prefs.avoidDairy}, avoidGluten=${prefs.avoidGluten}, flagHiddenDerivatives=${prefs.flagHiddenMammalDerivatives}, flagCarrageenan=${prefs.flagCarrageenan}.")
            appendLine("Extract the full ingredients text verbatim, then explain every ingredient in simple plain English and categorize each as MAMMAL_MEAT, DAIRY, GLUTEN, HIDDEN_RISK, or SAFE.")
            appendLine("Do not misclassify cocoa butter, coconut milk, oat milk, almond milk, buckwheat, maltodextrin, or cream of tartar.")
        }

        return try {
            val response = withTimeout(60_000L) {
                photoModel.generateContent(
                    content {
                        image(bitmap)
                        text(prompt)
                    }
                )
            }
            val jsonText = response.text
                ?: return Result.failure(IllegalStateException("Could not read text from label photo."))
            val root = JSONObject(jsonText)
            val extractedText = root.optString("extractedIngredientsText", "").trim()
            val productName = root.optString("productName", "Scanned Label")
                .ifBlank { "Scanned Label" }

            val baseline = AlphaGalRuleEngine.analyzeLocally(
                barcode = "LABEL-${System.currentTimeMillis()}",
                productName = productName,
                brand = "Label Photo Scan",
                imageUrl = null,
                rawIngredientsText = extractedText,
                allergenTagsFromLabel = emptyList(),
                prefs = prefs
            )
            Result.success(mergeAiResponse(baseline, root, prefs))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Label photo analysis timed out", e)
            Result.failure(Exception("Reading the label photo timed out. Try a clearer, closer photo."))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Label photo analysis failed", e)
            Result.failure(Exception(e.localizedMessage ?: "Failed to analyze label photo."))
        }
    }

    private fun mergeAiResponse(
        baseline: ProductAnalysisResult,
        root: JSONObject,
        prefs: UserAllergenPreferences
    ): ProductAnalysisResult {
        val aiIngredientsArray = root.optJSONArray("ingredients")
        val aiInsights = mutableListOf<IngredientInsight>()
        if (aiIngredientsArray != null) {
            for (i in 0 until aiIngredientsArray.length()) {
                val obj = aiIngredientsArray.optJSONObject(i) ?: continue
                val rawName = obj.optString("rawName", "").trim()
                if (rawName.isBlank()) continue
                val simpleName = obj.optString("simpleName", rawName).ifBlank { rawName }
                val explanation = obj.optString("plainEnglishExplanation", "").trim()
                val origin = obj.optString("sourceOrigin", "Unknown").trim()
                val whyMatters = obj.optString("whyItMattersForAlphaGal", "").trim()
                val aiCat = parseCategory(obj.optString("category", "SAFE"))

                // Safety guardrail: never let AI downgrade a deterministic MAMMAL_MEAT, DAIRY, or GLUTEN match
                val localCheck = AlphaGalRuleEngine.classifyIngredient(rawName, prefs)
                val finalCategory = when {
                    localCheck.category == AllergenCategory.MAMMAL_MEAT -> AllergenCategory.MAMMAL_MEAT
                    localCheck.category == AllergenCategory.DAIRY -> AllergenCategory.DAIRY
                    localCheck.category == AllergenCategory.GLUTEN -> AllergenCategory.GLUTEN
                    aiCat == AllergenCategory.MAMMAL_MEAT && prefs.avoidMammalMeat -> AllergenCategory.MAMMAL_MEAT
                    aiCat == AllergenCategory.DAIRY && prefs.avoidDairy -> AllergenCategory.DAIRY
                    aiCat == AllergenCategory.GLUTEN && prefs.avoidGluten -> AllergenCategory.GLUTEN
                    localCheck.category == AllergenCategory.HIDDEN_RISK -> AllergenCategory.HIDDEN_RISK
                    aiCat == AllergenCategory.HIDDEN_RISK && prefs.flagHiddenMammalDerivatives -> AllergenCategory.HIDDEN_RISK
                    else -> AllergenCategory.SAFE
                }

                aiInsights.add(
                    IngredientInsight(
                        rawName = rawName,
                        simpleName = simpleName,
                        plainEnglishExplanation = explanation.ifBlank { localCheck.plainEnglishExplanation },
                        category = finalCategory,
                        sourceOrigin = origin.ifBlank { localCheck.sourceOrigin },
                        whyItMattersForAlphaGal = whyMatters.ifBlank { localCheck.whyItMattersForAlphaGal }
                    )
                )
            }
        }

        val finalIngredients = if (aiInsights.isNotEmpty()) aiInsights else baseline.ingredients

        val mammalTriggers = (finalIngredients
            .filter { it.category == AllergenCategory.MAMMAL_MEAT }
            .map { it.rawName } + baseline.mammalMeatTriggers)
            .distinctBy { it.lowercase() }

        val dairyTriggers = (finalIngredients
            .filter { it.category == AllergenCategory.DAIRY }
            .map { it.rawName } + baseline.dairyTriggers)
            .distinctBy { it.lowercase() }

        val glutenTriggers = (finalIngredients
            .filter { it.category == AllergenCategory.GLUTEN }
            .map { it.rawName } + baseline.glutenTriggers)
            .distinctBy { it.lowercase() }

        val hiddenTriggers = (finalIngredients
            .filter { it.category == AllergenCategory.HIDDEN_RISK }
            .map { it.rawName } + baseline.hiddenRiskTriggers)
            .distinctBy { it.lowercase() }

        val mergedVerdict = when {
            finalIngredients.isEmpty() && baseline.rawIngredientsText.isBlank() -> SafetyVerdict.UNKNOWN
            mammalTriggers.isNotEmpty() || dairyTriggers.isNotEmpty() || glutenTriggers.isNotEmpty() -> SafetyVerdict.UNSAFE
            hiddenTriggers.isNotEmpty() -> SafetyVerdict.CAUTION
            else -> when (root.optString("overallVerdict", "SAFE").uppercase()) {
                "UNSAFE" -> SafetyVerdict.UNSAFE
                "CAUTION" -> SafetyVerdict.CAUTION
                else -> SafetyVerdict.SAFE
            }
        }

        val headline = when {
            mergedVerdict == SafetyVerdict.UNSAFE -> buildString {
                append("Avoid — ")
                val parts = mutableListOf<String>()
                if (mammalTriggers.isNotEmpty()) parts.add("Mammal")
                if (dairyTriggers.isNotEmpty()) parts.add("Dairy")
                if (glutenTriggers.isNotEmpty()) parts.add("Gluten")
                append(parts.joinToString(" & "))
                append(" Found")
            }
            else -> root.optString("verdictHeadline", baseline.verdictHeadline)
                .ifBlank { baseline.verdictHeadline }
        }

        val summary = root.optString("verdictSummary", baseline.verdictSummary)
            .ifBlank { baseline.verdictSummary }

        return baseline.copy(
            overallVerdict = mergedVerdict,
            verdictHeadline = headline,
            verdictSummary = summary,
            mammalMeatTriggers = mammalTriggers,
            dairyTriggers = dairyTriggers,
            glutenTriggers = glutenTriggers,
            hiddenRiskTriggers = hiddenTriggers,
            ingredients = finalIngredients,
            isAiEnhanced = true,
            aiErrorMessage = null
        )
    }

    private fun parseCategory(value: String): AllergenCategory {
        return when (value.uppercase().trim()) {
            "MAMMAL_MEAT" -> AllergenCategory.MAMMAL_MEAT
            "DAIRY" -> AllergenCategory.DAIRY
            "GLUTEN" -> AllergenCategory.GLUTEN
            "HIDDEN_RISK" -> AllergenCategory.HIDDEN_RISK
            else -> AllergenCategory.SAFE
        }
    }
}

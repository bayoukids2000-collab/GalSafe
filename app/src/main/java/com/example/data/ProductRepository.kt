package com.example.data

import android.graphics.Bitmap
import com.example.data.ai.AiIngredientAnalyzer
import com.example.data.local.PreferencesRepository
import com.example.data.local.ScanHistoryDao
import com.example.data.local.ScannedProductEntity
import com.example.data.remote.OffProductDto
import com.example.data.remote.OpenFoodFactsService
import com.example.domain.AlphaGalRuleEngine
import com.example.domain.model.ProductAnalysisResult
import com.example.domain.model.UserAllergenPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

sealed interface LookupOutcome {
    data class Found(val product: ProductAnalysisResult) : LookupOutcome
    data class NotInDatabase(val barcode: String) : LookupOutcome
    data class NetworkError(val barcode: String, val message: String) : LookupOutcome
}

class ProductRepository(
    private val offService: OpenFoodFactsService,
    private val scanHistoryDao: ScanHistoryDao,
    private val preferencesRepository: PreferencesRepository,
    private val aiAnalyzer: AiIngredientAnalyzer
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val scanHistory: Flow<List<ProductAnalysisResult>> = scanHistoryDao
        .observeAllScans()
        .map { entities -> entities.mapNotNull { it.toDomain(json) } }

    val preferences: Flow<UserAllergenPreferences> = preferencesRepository.preferencesFlow

    suspend fun getCachedProduct(barcode: String): ProductAnalysisResult? {
        return scanHistoryDao.getScanByBarcode(barcode)?.toDomain(json)
    }

    suspend fun fetchProductFast(barcode: String): LookupOutcome {
        val cleanCode = barcode.trim()
        val prefs = preferences.first()

        val cached = getCachedProduct(cleanCode)
        if (cached != null) {
            // Re-run local classification with latest preferences if user changed toggles
            val refreshed = if (cached.isAiEnhanced) cached else AlphaGalRuleEngine.analyzeLocally(
                barcode = cached.barcode,
                productName = cached.productName,
                brand = cached.brand,
                imageUrl = cached.imageUrl,
                rawIngredientsText = cached.rawIngredientsText,
                allergenTagsFromLabel = cached.allergenTagsFromLabel,
                prefs = prefs
            ).copy(isBookmarked = cached.isBookmarked, scannedAtEpochMs = System.currentTimeMillis())

            saveToHistory(refreshed)
            return LookupOutcome.Found(refreshed)
        }

        return try {
            val response = offService.getProductByBarcode(cleanCode)
            val dto = response.product
            if (response.status != 1 || dto == null) {
                LookupOutcome.NotInDatabase(cleanCode)
            } else {
                val result = mapDtoToBaseline(cleanCode, dto, prefs)
                saveToHistory(result)
                LookupOutcome.Found(result)
            }
        } catch (e: Exception) {
            LookupOutcome.NetworkError(
                barcode = cleanCode,
                message = e.localizedMessage ?: "Network error looking up barcode."
            )
        }
    }

    suspend fun searchProductsByName(query: String): Result<List<ProductAnalysisResult>> {
        return try {
            val prefs = preferences.first()
            val response = offService.searchProducts(query.trim())
            val mapped = response.products
                .filter { !it.code.isNullOrBlank() && (!it.productNameEn.isNullOrBlank() || !it.productName.isNullOrBlank()) }
                .map { dto ->
                    val code = dto.code.orEmpty()
                    mapDtoToBaseline(code, dto, prefs)
                }
            Result.success(mapped)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun analyzeCustomIngredients(
        barcode: String,
        productName: String,
        rawIngredients: String
    ): ProductAnalysisResult {
        val prefs = preferences.first()
        val baseline = AlphaGalRuleEngine.analyzeLocally(
            barcode = barcode,
            productName = productName.ifBlank { "Custom Ingredient Check" },
            brand = "Manual Entry",
            imageUrl = null,
            rawIngredientsText = rawIngredients,
            allergenTagsFromLabel = emptyList(),
            prefs = prefs
        )
        saveToHistory(baseline)
        return baseline
    }

    suspend fun enhanceWithAi(product: ProductAnalysisResult): ProductAnalysisResult {
        val prefs = preferences.first()
        val enhanced = aiAnalyzer.enhanceAnalysis(product, prefs)
        val preserved = enhanced.copy(
            isBookmarked = product.isBookmarked,
            scannedAtEpochMs = product.scannedAtEpochMs
        )
        saveToHistory(preserved)
        return preserved
    }

    suspend fun analyzeLabelPhoto(bitmap: Bitmap): Result<ProductAnalysisResult> {
        val prefs = preferences.first()
        val result = aiAnalyzer.analyzeLabelPhoto(bitmap, prefs)
        result.getOrNull()?.let { saveToHistory(it) }
        return result
    }

    suspend fun saveToHistory(result: ProductAnalysisResult) {
        scanHistoryDao.upsertScan(ScannedProductEntity.fromDomain(result, json))
    }

    suspend fun toggleBookmark(barcode: String, currentlyBookmarked: Boolean) {
        scanHistoryDao.updateBookmark(barcode, !currentlyBookmarked)
    }

    suspend fun deleteFromHistory(barcode: String) {
        scanHistoryDao.deleteByBarcode(barcode)
    }

    suspend fun clearUnbookmarkedHistory() {
        scanHistoryDao.clearUnbookmarkedHistory()
    }

    private fun mapDtoToBaseline(
        barcode: String,
        dto: OffProductDto,
        prefs: UserAllergenPreferences
    ): ProductAnalysisResult {
        val name = dto.productNameEn?.takeIf { it.isNotBlank() }
            ?: dto.productName?.takeIf { it.isNotBlank() }
            ?: "Unknown Product"
        val brand = dto.brands?.takeIf { it.isNotBlank() } ?: "Brand not listed"
        val ingredients = dto.ingredientsTextEn?.takeIf { it.isNotBlank() }
            ?: dto.ingredientsText?.takeIf { it.isNotBlank() }
            ?: ""
        val allergens = (dto.allergensTags.orEmpty() + dto.tracesTags.orEmpty())
            .map { it.removePrefix("en:").replace('-', ' ') }
            .distinct()

        return AlphaGalRuleEngine.analyzeLocally(
            barcode = barcode,
            productName = name,
            brand = brand,
            imageUrl = dto.imageFrontUrl ?: dto.imageFrontSmallUrl,
            rawIngredientsText = ingredients,
            allergenTagsFromLabel = allergens,
            prefs = prefs
        )
    }
}

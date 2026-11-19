package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ProductAnalysisResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "scan_history")
data class ScannedProductEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val brand: String,
    val imageUrl: String?,
    val rawIngredientsText: String,
    val overallVerdict: String,
    val verdictHeadline: String,
    val verdictSummary: String,
    val mammalCount: Int,
    val dairyCount: Int,
    val hiddenCount: Int,
    val analysisJson: String,
    val scannedAtEpochMs: Long,
    val isBookmarked: Boolean
) {
    fun toDomain(json: Json): ProductAnalysisResult? {
        return try {
            json.decodeFromString<ProductAnalysisResult>(analysisJson)
                .copy(isBookmarked = isBookmarked, scannedAtEpochMs = scannedAtEpochMs)
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        fun fromDomain(result: ProductAnalysisResult, json: Json): ScannedProductEntity {
            return ScannedProductEntity(
                barcode = result.barcode,
                productName = result.productName,
                brand = result.brand,
                imageUrl = result.imageUrl,
                rawIngredientsText = result.rawIngredientsText,
                overallVerdict = result.overallVerdict.name,
                verdictHeadline = result.verdictHeadline,
                verdictSummary = result.verdictSummary,
                mammalCount = result.mammalMeatTriggers.size,
                dairyCount = result.dairyTriggers.size,
                hiddenCount = result.hiddenRiskTriggers.size,
                analysisJson = json.encodeToString(result),
                scannedAtEpochMs = result.scannedAtEpochMs,
                isBookmarked = result.isBookmarked
            )
        }
    }
}

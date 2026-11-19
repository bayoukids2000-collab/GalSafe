package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAtEpochMs DESC")
    fun observeAllScans(): Flow<List<ScannedProductEntity>>

    @Query("SELECT * FROM scan_history WHERE barcode = :barcode LIMIT 1")
    suspend fun getScanByBarcode(barcode: String): ScannedProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScan(entity: ScannedProductEntity)

    @Query("UPDATE scan_history SET isBookmarked = :bookmarked WHERE barcode = :barcode")
    suspend fun updateBookmark(barcode: String, bookmarked: Boolean)

    @Query("DELETE FROM scan_history WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM scan_history WHERE isBookmarked = 0")
    suspend fun clearUnbookmarkedHistory()
}

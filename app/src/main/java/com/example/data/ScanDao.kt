package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scan_history ORDER BY timestamp DESC")
    fun getAllRecords(): Flow<List<ScanRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: ScanRecord): Long

    @Delete
    suspend fun deleteRecord(record: ScanRecord)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

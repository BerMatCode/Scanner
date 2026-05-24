package com.example.data

import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allRecords: Flow<List<ScanRecord>> = scanDao.getAllRecords()

    suspend fun insertRecord(record: ScanRecord): Long {
        return scanDao.insertRecord(record)
    }

    suspend fun deleteRecord(record: ScanRecord) {
        scanDao.deleteRecord(record)
    }

    suspend fun clearAll() {
        scanDao.clearAll()
    }
}

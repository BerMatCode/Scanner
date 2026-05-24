package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "scan_history")
data class ScanRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val rawContent: String,
    val qrType: String, // "LINK", "TEXT", "WIFI", "PHONE", "EMAIL", "WHATSAPP", "YAPE"
    val timestamp: Long = System.currentTimeMillis()
) : Serializable

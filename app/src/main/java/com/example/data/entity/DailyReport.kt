package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_reports")
data class DailyReport(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val date: String, // Format: YYYY-MM-DD
    val location: String,
    val workPlanNext: String = "",
    val reportStatus: String = "SELESAI", // DRAFT, BERJALAN, SELESAI
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

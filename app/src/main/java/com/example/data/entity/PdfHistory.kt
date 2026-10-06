package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_history")
data class PdfHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dailyReportId: Long,
    val date: String,
    val location: String,
    val filePath: String,
    val fileName: String,
    val fileSize: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

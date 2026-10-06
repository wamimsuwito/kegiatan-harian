package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_items",
    foreignKeys = [
        ForeignKey(
            entity = DailyReport::class,
            parentColumns = ["id"],
            childColumns = ["dailyReportId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["dailyReportId"]),
        Index(value = ["permanentWorkId"])
    ]
)
data class WorkItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val dailyReportId: Long,
    val permanentWorkId: String, // Unique identifier shared across multi-day tasks (e.g. WRK-1712345678)
    val sequenceNumber: Int, // 1, 2, 3... in the context of the daily report
    val description: String,
    val targetStartDate: String, // DD/MM/YYYY or YYYY-MM-DD
    val targetEndDate: String,
    val realizationDate: String = "",
    val workers: String = "", // e.g. "3 orang: Hendra, Johanes, Rahmat"
    val tools: String = "", // e.g. "Chain block, kunci inggris, palu"
    val currentProgress: Int = 0, // 0 - 100
    val status: String = "BERJALAN", // "BELUM_MULAI", "BERJALAN", "SELESAI"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

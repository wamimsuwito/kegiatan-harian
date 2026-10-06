package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_progress_history",
    indices = [
        Index(value = ["permanentWorkId"]),
        Index(value = ["dailyReportId"])
    ]
)
data class WorkProgressHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val permanentWorkId: String,
    val dailyReportId: Long,
    val date: String,
    val progress: Int,
    val notes: String = "",
    val workers: String = "",
    val tools: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

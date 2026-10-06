package com.example.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "work_photos",
    indices = [
        Index(value = ["workItemId"]),
        Index(value = ["permanentWorkId"]),
        Index(value = ["dailyReportId"])
    ]
)
data class WorkPhoto(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val workItemId: Long,
    val permanentWorkId: String,
    val dailyReportId: Long,
    val photoType: String, // "BEFORE", "PROCESS", "AFTER"
    val filePath: String, // relative or absolute local storage path
    val capturedAt: Long = System.currentTimeMillis()
)

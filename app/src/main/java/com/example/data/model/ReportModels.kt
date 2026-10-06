package com.example.data.model

import androidx.room.Embedded
import androidx.room.Relation
import com.example.data.entity.DailyReport
import com.example.data.entity.WorkItem
import com.example.data.entity.WorkPhoto
import com.example.data.entity.WorkProgressHistory

data class WorkItemWithPhotos(
    @Embedded val workItem: WorkItem,
    @Relation(
        parentColumn = "id",
        entityColumn = "workItemId"
    )
    val photos: List<WorkPhoto> = emptyList()
) {
    val beforePhoto: WorkPhoto?
        get() = photos.lastOrNull { it.photoType == "BEFORE" }

    val processPhoto: WorkPhoto?
        get() = photos.lastOrNull { it.photoType == "PROCESS" }

    val afterPhoto: WorkPhoto?
        get() = photos.lastOrNull { it.photoType == "AFTER" }
}

data class DailyReportWithDetails(
    @Embedded val report: DailyReport,
    @Relation(
        entity = WorkItem::class,
        parentColumn = "id",
        entityColumn = "dailyReportId"
    )
    val workItems: List<WorkItemWithPhotos> = emptyList()
)

data class OngoingWorkSummary(
    val permanentWorkId: String,
    val latestWorkItemId: Long,
    val description: String,
    val targetStartDate: String,
    val targetEndDate: String,
    val latestProgress: Int,
    val status: String,
    val workers: String,
    val tools: String,
    val lastReportDate: String,
    val lastLocation: String,
    val beforePhotoPath: String?,
    val processPhotoPath: String?,
    val afterPhotoPath: String?
)

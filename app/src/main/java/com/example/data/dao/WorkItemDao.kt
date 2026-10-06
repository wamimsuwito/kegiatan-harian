package com.example.data.dao

import androidx.room.*
import com.example.data.entity.WorkItem
import com.example.data.entity.WorkPhoto
import com.example.data.entity.WorkProgressHistory
import com.example.data.model.WorkItemWithPhotos
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItem(workItem: WorkItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkItems(workItems: List<WorkItem>): List<Long>

    @Update
    suspend fun updateWorkItem(workItem: WorkItem)

    @Delete
    suspend fun deleteWorkItem(workItem: WorkItem)

    @Query("DELETE FROM work_items WHERE id = :id")
    suspend fun deleteWorkItemById(id: Long)

    @Query("DELETE FROM work_items WHERE dailyReportId = :reportId")
    suspend fun deleteWorkItemsByReportId(reportId: Long)

    @Transaction
    @Query("SELECT * FROM work_items WHERE id = :id")
    fun getWorkItemWithPhotosById(id: Long): Flow<WorkItemWithPhotos?>

    @Transaction
    @Query("SELECT * FROM work_items WHERE id = :id")
    suspend fun getWorkItemWithPhotosByIdDirect(id: Long): WorkItemWithPhotos?

    @Transaction
    @Query("SELECT * FROM work_items WHERE dailyReportId = :reportId ORDER BY sequenceNumber ASC")
    fun getWorkItemsByReportId(reportId: Long): Flow<List<WorkItemWithPhotos>>

    @Transaction
    @Query("SELECT * FROM work_items WHERE dailyReportId = :reportId ORDER BY sequenceNumber ASC")
    suspend fun getWorkItemsByReportIdDirect(reportId: Long): List<WorkItemWithPhotos>

    // Ongoing work: find distinct permanentWorkId where status != 'SELESAI' and currentProgress < 100
    // We order by targetEndDate ASC, then targetStartDate ASC
    @Transaction
    @Query("""
        SELECT * FROM work_items 
        WHERE id IN (
            SELECT MAX(id) FROM work_items 
            GROUP BY permanentWorkId
        )
        AND status != 'SELESAI' 
        AND currentProgress < 100
        ORDER BY targetEndDate ASC, targetStartDate ASC
    """)
    fun getOngoingWorkItems(): Flow<List<WorkItemWithPhotos>>

    // All work items for a permanentWorkId to view full history
    @Transaction
    @Query("SELECT * FROM work_items WHERE permanentWorkId = :permanentWorkId ORDER BY id ASC")
    fun getWorkItemsByPermanentId(permanentWorkId: String): Flow<List<WorkItemWithPhotos>>

    @Transaction
    @Query("""
        SELECT * FROM work_items 
        WHERE description LIKE '%' || :query || '%' 
           OR workers LIKE '%' || :query || '%' 
           OR tools LIKE '%' || :query || '%' 
           OR notes LIKE '%' || :query || '%'
        ORDER BY id DESC
    """)
    fun searchWorkItems(query: String): Flow<List<WorkItemWithPhotos>>

    // Statistics
    @Query("""
        SELECT COUNT(*) FROM work_items 
        WHERE dailyReportId IN (SELECT id FROM daily_reports WHERE date = :date)
    """)
    fun getTodayWorkCount(date: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM work_items 
        WHERE dailyReportId IN (SELECT id FROM daily_reports WHERE date = :date)
          AND (status = 'SELESAI' OR currentProgress >= 100)
    """)
    fun getTodayCompletedWorkCount(date: String): Flow<Int>

    @Query("""
        SELECT COUNT(*) FROM work_items 
        WHERE dailyReportId IN (SELECT id FROM daily_reports WHERE date = :date)
          AND status != 'SELESAI' AND currentProgress < 100
    """)
    fun getTodayOngoingWorkCount(date: String): Flow<Int>

    // Photos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: WorkPhoto): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<WorkPhoto>): List<Long>

    @Query("DELETE FROM work_photos WHERE id = :photoId")
    suspend fun deletePhotoById(photoId: Long)

    @Query("DELETE FROM work_photos WHERE workItemId = :workItemId AND photoType = :photoType")
    suspend fun deletePhotoByType(workItemId: Long, photoType: String)

    @Query("SELECT * FROM work_photos WHERE permanentWorkId = :permanentWorkId ORDER BY capturedAt ASC")
    fun getPhotosByPermanentWorkId(permanentWorkId: String): Flow<List<WorkPhoto>>

    @Query("SELECT * FROM work_photos WHERE workItemId = :workItemId")
    suspend fun getPhotosByWorkItemId(workItemId: Long): List<WorkPhoto>

    // Progress History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressHistory(history: WorkProgressHistory): Long

    @Query("SELECT * FROM work_progress_history WHERE permanentWorkId = :permanentWorkId ORDER BY date ASC, id ASC")
    fun getProgressHistoryByPermanentId(permanentWorkId: String): Flow<List<WorkProgressHistory>>

    @Query("SELECT * FROM work_progress_history WHERE permanentWorkId = :permanentWorkId ORDER BY date ASC, id ASC")
    suspend fun getProgressHistoryByPermanentIdDirect(permanentWorkId: String): List<WorkProgressHistory>
}

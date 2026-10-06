package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.AppDatabase
import com.example.data.entity.*
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.WorkItemWithPhotos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class ReportRepository(
    private val database: AppDatabase,
    private val context: Context
) {
    private val reportDao = database.reportDao()
    private val workItemDao = database.workItemDao()
    private val profileDao = database.userProfileDao()
    private val pdfHistoryDao = database.pdfHistoryDao()

    val allReports: Flow<List<DailyReportWithDetails>> = reportDao.getAllReportsWithDetails()
    val ongoingWorkItems: Flow<List<WorkItemWithPhotos>> = workItemDao.getOngoingWorkItems()
    val userProfile: Flow<UserProfile?> = profileDao.getUserProfile()
    val allPdfHistory: Flow<List<PdfHistory>> = pdfHistoryDao.getAllPdfHistory()

    fun getReportById(reportId: Long): Flow<DailyReportWithDetails?> =
        reportDao.getReportWithDetailsById(reportId)

    suspend fun getReportByIdDirect(reportId: Long): DailyReportWithDetails? =
        reportDao.getReportWithDetailsByIdDirect(reportId)

    suspend fun getReportByDateDirect(date: String): DailyReportWithDetails? =
        reportDao.getReportByDate(date)

    fun getWorkItemById(id: Long): Flow<WorkItemWithPhotos?> =
        workItemDao.getWorkItemWithPhotosById(id)

    suspend fun getWorkItemByIdDirect(id: Long): WorkItemWithPhotos? =
        workItemDao.getWorkItemWithPhotosByIdDirect(id)

    fun getProgressHistory(permanentWorkId: String): Flow<List<WorkProgressHistory>> =
        workItemDao.getProgressHistoryByPermanentId(permanentWorkId)

    fun getWorkItemsByPermanentId(permanentWorkId: String): Flow<List<WorkItemWithPhotos>> =
        workItemDao.getWorkItemsByPermanentId(permanentWorkId)

    fun searchWorkItems(query: String): Flow<List<WorkItemWithPhotos>> =
        workItemDao.searchWorkItems(query)

    fun getTodayWorkCount(date: String): Flow<Int> = workItemDao.getTodayWorkCount(date)
    fun getTodayCompletedWorkCount(date: String): Flow<Int> = workItemDao.getTodayCompletedWorkCount(date)
    fun getTodayOngoingWorkCount(date: String): Flow<Int> = workItemDao.getTodayOngoingWorkCount(date)

    suspend fun saveReportWithItems(
        report: DailyReport,
        itemsWithPhotos: List<Pair<WorkItem, List<WorkPhoto>>>
    ): Long = withContext(Dispatchers.IO) {
        database.withTransaction {
            val reportId = if (report.id == 0L) {
                reportDao.insertReport(report)
            } else {
                reportDao.updateReport(report.copy(updatedAt = System.currentTimeMillis()))
                report.id
            }

            // Get existing items for this report to handle deletions
            val existingItems = workItemDao.getWorkItemsByReportIdDirect(reportId)
            val currentItemIds = itemsWithPhotos.map { it.first.id }.filter { it != 0L }

            for (existing in existingItems) {
                if (existing.workItem.id !in currentItemIds) {
                    workItemDao.deleteWorkItemById(existing.workItem.id)
                }
            }

            // Insert / update work items and their photos
            itemsWithPhotos.forEachIndexed { index, (item, photos) ->
                val calculatedStatus = if (item.currentProgress >= 100) "SELESAI" else "BERJALAN"
                val itemToSave = item.copy(
                    dailyReportId = reportId,
                    sequenceNumber = index + 1,
                    status = calculatedStatus
                )

                val workItemId = if (itemToSave.id == 0L) {
                    workItemDao.insertWorkItem(itemToSave)
                } else {
                    workItemDao.updateWorkItem(itemToSave)
                    itemToSave.id
                }

                // Record progress history for this date
                workItemDao.insertProgressHistory(
                    WorkProgressHistory(
                        permanentWorkId = itemToSave.permanentWorkId,
                        dailyReportId = reportId,
                        date = report.date,
                        progress = itemToSave.currentProgress,
                        notes = itemToSave.notes,
                        workers = itemToSave.workers,
                        tools = itemToSave.tools
                    )
                )

                // Save photos
                photos.forEach { photo ->
                    val photoToSave = photo.copy(
                        workItemId = workItemId,
                        permanentWorkId = itemToSave.permanentWorkId,
                        dailyReportId = reportId
                    )
                    // Delete previous photo of same type if new one is assigned
                    workItemDao.deletePhotoByType(workItemId, photoToSave.photoType)
                    workItemDao.insertPhoto(photoToSave)
                }
            }

            reportId
        }
    }

    suspend fun updateWorkItemProgress(
        workItemId: Long,
        newProgress: Int,
        date: String,
        notes: String,
        newPhotos: List<WorkPhoto> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val currentItem = workItemDao.getWorkItemWithPhotosByIdDirect(workItemId) ?: return@withContext
        val newStatus = if (newProgress >= 100) "SELESAI" else "BERJALAN"
        val updated = currentItem.workItem.copy(
            currentProgress = newProgress,
            status = newStatus,
            notes = if (notes.isNotBlank()) notes else currentItem.workItem.notes
        )
        workItemDao.updateWorkItem(updated)

        // Add history
        workItemDao.insertProgressHistory(
            WorkProgressHistory(
                permanentWorkId = updated.permanentWorkId,
                dailyReportId = updated.dailyReportId,
                date = date,
                progress = newProgress,
                notes = notes,
                workers = updated.workers,
                tools = updated.tools
            )
        )

        // Insert new photos if any
        newPhotos.forEach { photo ->
            workItemDao.deletePhotoByType(workItemId, photo.photoType)
            workItemDao.insertPhoto(photo.copy(workItemId = workItemId, permanentWorkId = updated.permanentWorkId))
        }
    }

    suspend fun deleteReport(reportId: Long) = withContext(Dispatchers.IO) {
        reportDao.deleteReportById(reportId)
    }

    suspend fun saveProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        profileDao.insertOrUpdateProfile(profile)
    }

    suspend fun getProfileDirect(): UserProfile? = withContext(Dispatchers.IO) {
        profileDao.getUserProfileDirect()
    }

    suspend fun savePdfHistory(pdfHistory: PdfHistory) = withContext(Dispatchers.IO) {
        pdfHistoryDao.insertPdfHistory(pdfHistory)
    }

    suspend fun deletePdfHistory(id: Long) = withContext(Dispatchers.IO) {
        pdfHistoryDao.deletePdfHistoryById(id)
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val existing = reportDao.getReportByDate("2026-06-03")
        if (existing != null) return@withContext

        // User Profile sample
        val currentProfile = profileDao.getUserProfileDirect()
        if (currentProfile == null || currentProfile.name.isBlank()) {
            profileDao.insertOrUpdateProfile(
                UserProfile(
                    id = 1L,
                    name = "WAMIN",
                    position = "Pelaksana Lapangan / Maintenance",
                    signaturePath = null
                )
            )
        }

        // Demo Report 1: 03 Juni 2026 (From user spec Point 43)
        val report1 = DailyReport(
            id = 0L,
            date = "2026-06-03",
            location = "Dumai - Pekanbaru",
            workPlanNext = "1. perbaikan chute untuk aliran beton dari mixer bp ke truck mixer (chute banyak keropos dan berlubang)",
            reportStatus = "SELESAI"
        )
        val rep1Id = reportDao.insertReport(report1)

        val item1 = WorkItem(
            dailyReportId = rep1Id,
            permanentWorkId = "WRK-2026-001",
            sequenceNumber = 1,
            description = "Perbaikan Screw semen silo 2 dan 4 (silo 2 ganti dinamo screw, silo 3 perbaikan pipa screw mampet)",
            targetStartDate = "03/06/2026",
            targetEndDate = "03/06/2026",
            realizationDate = "03/06/2026",
            workers = "3 orang: Hendra, Johanes, Rahmat",
            tools = "Chain block, kunci inggris, palu",
            currentProgress = 100,
            status = "SELESAI",
            notes = "Dinamo baru telah dipasang dan ditest running dengan lancar."
        )
        val i1Id = workItemDao.insertWorkItem(item1)
        workItemDao.insertProgressHistory(
            WorkProgressHistory(
                permanentWorkId = "WRK-2026-001",
                dailyReportId = rep1Id,
                date = "2026-06-03",
                progress = 100,
                notes = "Selesai 100%",
                workers = item1.workers,
                tools = item1.tools
            )
        )

        val item2 = WorkItem(
            dailyReportId = rep1Id,
            permanentWorkId = "WRK-2026-002",
            sequenceNumber = 2,
            description = "Perbaikan / penyetelan ulang gearbox mixer BP, setel area seal yang bocor",
            targetStartDate = "03/06/2026",
            targetEndDate = "03/06/2026",
            realizationDate = "03/06/2026",
            workers = "2 orang: Wamin dan Hendra",
            tools = "Kunci pipa, kunci inggris",
            currentProgress = 100,
            status = "SELESAI",
            notes = "Seal diganti baru dan kebocoran teratasi."
        )
        val i2Id = workItemDao.insertWorkItem(item2)
        workItemDao.insertProgressHistory(
            WorkProgressHistory(
                permanentWorkId = "WRK-2026-002",
                dailyReportId = rep1Id,
                date = "2026-06-03",
                progress = 100,
                notes = "Penyetelan selesai",
                workers = item2.workers,
                tools = item2.tools
            )
        )

        val item3 = WorkItem(
            dailyReportId = rep1Id,
            permanentWorkId = "WRK-2026-003",
            sequenceNumber = 3,
            description = "Perapihan kabel-kabel panel kontrol BP",
            targetStartDate = "03/06/2026",
            targetEndDate = "03/06/2026",
            realizationDate = "03/06/2026",
            workers = "1 orang: Wamin",
            tools = "Tang amper, multi tester",
            currentProgress = 100,
            status = "SELESAI",
            notes = "Wiring rapi dan penomoran kabel terpasang."
        )
        val i3Id = workItemDao.insertWorkItem(item3)
        workItemDao.insertProgressHistory(
            WorkProgressHistory(
                permanentWorkId = "WRK-2026-003",
                dailyReportId = rep1Id,
                date = "2026-06-03",
                progress = 100,
                notes = "Selesai perapihan",
                workers = item3.workers,
                tools = item3.tools
            )
        )

        // Demo Multi-hari (Point 44):
        // 08 Juli 2026: Perbaikan chute mixer BP (30%)
        val reportJuly8 = DailyReport(
            id = 0L,
            date = "2026-07-08",
            location = "Dumai - Pekanbaru",
            workPlanNext = "Lanjutkan pengelasan plat pelapis chute.",
            reportStatus = "BERJALAN"
        )
        val repJuly8Id = reportDao.insertReport(reportJuly8)
        val chuteWork = WorkItem(
            dailyReportId = repJuly8Id,
            permanentWorkId = "WRK-2026-004",
            sequenceNumber = 1,
            description = "Perbaikan chute mixer BP untuk aliran beton ke truck mixer",
            targetStartDate = "08/07/2026",
            targetEndDate = "10/07/2026",
            realizationDate = "08/07/2026",
            workers = "3 orang: Hendra, Johanes, Rahmat",
            tools = "Mesin las, gerinda potong, chain block",
            currentProgress = 30,
            status = "BERJALAN",
            notes = "Pembongkaran chute keropos dan pemotongan plat baru."
        )
        workItemDao.insertWorkItem(chuteWork)
        workItemDao.insertProgressHistory(
            WorkProgressHistory(
                permanentWorkId = "WRK-2026-004",
                dailyReportId = repJuly8Id,
                date = "2026-07-08",
                progress = 30,
                notes = "Pembongkaran plat lama dan marking plat baru.",
                workers = chuteWork.workers,
                tools = chuteWork.tools
            )
        )
    }
}

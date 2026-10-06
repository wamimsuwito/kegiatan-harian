package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.DailyReport
import com.example.data.entity.PdfHistory
import com.example.data.entity.WorkItem
import com.example.data.entity.WorkPhoto
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.WorkItemWithPhotos
import com.example.data.repository.ReportRepository
import com.example.util.DateUtils
import com.example.util.FileUtils
import com.example.util.PdfGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class WorkItemDraft(
    val tempId: String = UUID.randomUUID().toString(),
    val id: Long = 0L,
    val permanentWorkId: String = "WRK-${System.currentTimeMillis()}-${(100..999).random()}",
    val sequenceNumber: Int = 1,
    var description: String = "",
    var targetStartDate: String = DateUtils.getTodayDmy(),
    var targetEndDate: String = DateUtils.getTodayDmy(),
    var realizationDate: String = DateUtils.getTodayDmy(),
    var workers: String = "",
    var tools: String = "",
    var currentProgress: Int = 0,
    var notes: String = "",
    var beforePhotoPath: String? = null,
    var processPhotoPath: String? = null,
    var afterPhotoPath: String? = null
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ReportRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = ReportRepository(db, application)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    val allReports = repository.allReports
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ongoingWorkItems = repository.ongoingWorkItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pdfHistory = repository.allPdfHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayWorkCount = repository.getTodayWorkCount(DateUtils.getTodayIso())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayCompletedCount = repository.getTodayCompletedWorkCount(DateUtils.getTodayIso())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayOngoingCount = repository.getTodayOngoingWorkCount(DateUtils.getTodayIso())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Form State for Report Creation/Edit
    var currentReportId = MutableStateFlow<Long>(0L)
    var reportDate = MutableStateFlow(DateUtils.getTodayIso())
    var reportLocation = MutableStateFlow("Dumai - Pekanbaru")
    var workPlanNext = MutableStateFlow("")
    var formWorkItems = MutableStateFlow<List<WorkItemDraft>>(listOf(WorkItemDraft(sequenceNumber = 1)))

    var isSaving = MutableStateFlow(false)
    var errorMessage = MutableStateFlow<String?>(null)
    var saveSuccess = MutableStateFlow(false)

    // PDF generation output
    var generatedPdfFile = MutableStateFlow<File?>(null)
    var isGeneratingPdf = MutableStateFlow(false)

    // Auto-save draft check
    private val prefs = application.getSharedPreferences("report_draft_prefs", Context.MODE_PRIVATE)

    fun hasDraft(): Boolean {
        return prefs.getBoolean("has_draft", false)
    }

    fun saveDraftLocally() {
        if (formWorkItems.value.isEmpty()) return
        prefs.edit().apply {
            putBoolean("has_draft", true)
            putString("draft_date", reportDate.value)
            putString("draft_location", reportLocation.value)
            putString("draft_plan", workPlanNext.value)
            apply()
        }
    }

    fun clearDraftLocally() {
        prefs.edit().clear().apply()
    }

    fun restoreDraft() {
        if (hasDraft()) {
            reportDate.value = prefs.getString("draft_date", DateUtils.getTodayIso()) ?: DateUtils.getTodayIso()
            reportLocation.value = prefs.getString("draft_location", "") ?: ""
            workPlanNext.value = prefs.getString("draft_plan", "") ?: ""
        }
    }

    fun resetForm() {
        currentReportId.value = 0L
        reportDate.value = DateUtils.getTodayIso()
        reportLocation.value = "Dumai - Pekanbaru"
        workPlanNext.value = ""
        formWorkItems.value = listOf(WorkItemDraft(sequenceNumber = 1))
        errorMessage.value = null
        saveSuccess.value = false
        generatedPdfFile.value = null
    }

    fun loadReportForEdit(reportWithDetails: DailyReportWithDetails) {
        val report = reportWithDetails.report
        currentReportId.value = report.id
        reportDate.value = report.date
        reportLocation.value = report.location
        workPlanNext.value = report.workPlanNext

        val items = reportWithDetails.workItems.mapIndexed { idx, it ->
            WorkItemDraft(
                id = it.workItem.id,
                permanentWorkId = it.workItem.permanentWorkId,
                sequenceNumber = idx + 1,
                description = it.workItem.description,
                targetStartDate = it.workItem.targetStartDate,
                targetEndDate = it.workItem.targetEndDate,
                realizationDate = it.workItem.realizationDate,
                workers = it.workItem.workers,
                tools = it.workItem.tools,
                currentProgress = it.workItem.currentProgress,
                notes = it.workItem.notes,
                beforePhotoPath = it.beforePhoto?.filePath,
                processPhotoPath = it.processPhoto?.filePath,
                afterPhotoPath = it.afterPhoto?.filePath
            )
        }
        formWorkItems.value = if (items.isNotEmpty()) items else listOf(WorkItemDraft(sequenceNumber = 1))
    }

    fun addWorkItem() {
        val nextSeq = formWorkItems.value.size + 1
        val newItem = WorkItemDraft(sequenceNumber = nextSeq)
        formWorkItems.value = formWorkItems.value + newItem
    }

    fun removeWorkItem(tempId: String) {
        val updated = formWorkItems.value.filter { it.tempId != tempId }
            .mapIndexed { index, draft -> draft.copy(sequenceNumber = index + 1) }
        formWorkItems.value = if (updated.isEmpty()) listOf(WorkItemDraft(sequenceNumber = 1)) else updated
    }

    fun importOngoingWork(ongoing: WorkItemWithPhotos) {
        val nextSeq = formWorkItems.value.size + 1
        val item = ongoing.workItem
        val imported = WorkItemDraft(
            id = 0L, // New entry for this report date, but links permanentWorkId!
            permanentWorkId = item.permanentWorkId,
            sequenceNumber = nextSeq,
            description = item.description,
            targetStartDate = item.targetStartDate,
            targetEndDate = item.targetEndDate,
            realizationDate = DateUtils.getTodayDmy(),
            workers = item.workers,
            tools = item.tools,
            currentProgress = item.currentProgress,
            notes = item.notes,
            beforePhotoPath = ongoing.beforePhoto?.filePath,
            processPhotoPath = ongoing.processPhoto?.filePath,
            afterPhotoPath = ongoing.afterPhoto?.filePath
        )
        formWorkItems.value = formWorkItems.value + imported
    }

    fun updateWorkItem(tempId: String, update: (WorkItemDraft) -> WorkItemDraft) {
        formWorkItems.value = formWorkItems.value.map {
            if (it.tempId == tempId) update(it) else it
        }
    }

    fun saveReport(onComplete: (Boolean) -> Unit) {
        val loc = reportLocation.value.trim()
        if (loc.isBlank()) {
            errorMessage.value = "Lokasi laporan wajib diisi."
            onComplete(false)
            return
        }

        // Validate each item
        formWorkItems.value.forEachIndexed { index, item ->
            val num = index + 1
            if (item.description.trim().isBlank()) {
                errorMessage.value = "Deskripsi kegiatan nomor $num wajib diisi."
                onComplete(false)
                return
            }
            if (item.targetStartDate.trim().isBlank()) {
                errorMessage.value = "Target mulai kegiatan nomor $num wajib diisi."
                onComplete(false)
                return
            }
            if (item.targetEndDate.trim().isBlank()) {
                errorMessage.value = "Target selesai kegiatan nomor $num wajib diisi."
                onComplete(false)
                return
            }
            // If progress is 100%, after photo is required per prompt rule:
            // "Jika progress sudah 100%: Foto sesudah harus tersedia sebelum kegiatan ditandai selesai."
            if (item.currentProgress >= 100 && item.afterPhotoPath.isNullOrBlank()) {
                errorMessage.value = "Kegiatan nomor $num memiliki progress 100%. Foto SESUDAH wajib diisi sebelum selesai."
                onComplete(false)
                return
            }
        }

        viewModelScope.launch {
            isSaving.value = true
            errorMessage.value = null

            try {
                val report = DailyReport(
                    id = currentReportId.value,
                    date = reportDate.value,
                    location = loc,
                    workPlanNext = workPlanNext.value.trim(),
                    reportStatus = if (formWorkItems.value.all { it.currentProgress >= 100 }) "SELESAI" else "BERJALAN"
                )

                val itemsWithPhotos = formWorkItems.value.mapIndexed { idx, draft ->
                    val workItem = WorkItem(
                        id = draft.id,
                        dailyReportId = currentReportId.value,
                        permanentWorkId = draft.permanentWorkId,
                        sequenceNumber = idx + 1,
                        description = draft.description.trim(),
                        targetStartDate = draft.targetStartDate.trim(),
                        targetEndDate = draft.targetEndDate.trim(),
                        realizationDate = draft.realizationDate.trim(),
                        workers = draft.workers.trim(),
                        tools = draft.tools.trim(),
                        currentProgress = draft.currentProgress,
                        status = if (draft.currentProgress >= 100) "SELESAI" else "BERJALAN",
                        notes = draft.notes.trim()
                    )

                    val photos = mutableListOf<WorkPhoto>()
                    draft.beforePhotoPath?.let {
                        photos.add(WorkPhoto(workItemId = draft.id, permanentWorkId = draft.permanentWorkId, dailyReportId = currentReportId.value, photoType = "BEFORE", filePath = it))
                    }
                    draft.processPhotoPath?.let {
                        photos.add(WorkPhoto(workItemId = draft.id, permanentWorkId = draft.permanentWorkId, dailyReportId = currentReportId.value, photoType = "PROCESS", filePath = it))
                    }
                    draft.afterPhotoPath?.let {
                        photos.add(WorkPhoto(workItemId = draft.id, permanentWorkId = draft.permanentWorkId, dailyReportId = currentReportId.value, photoType = "AFTER", filePath = it))
                    }

                    Pair(workItem, photos)
                }

                val savedId = repository.saveReportWithItems(report, itemsWithPhotos)
                currentReportId.value = savedId
                clearDraftLocally()
                saveSuccess.value = true
                onComplete(true)
            } catch (e: Exception) {
                errorMessage.value = "Gagal menyimpan laporan: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                isSaving.value = false
            }
        }
    }

    fun generatePdfForCurrentReport(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            isGeneratingPdf.value = true
            try {
                // Ensure saved first if needed, or build directly from current data
                val profile = repository.getProfileDirect()
                val currentRep = repository.getReportByIdDirect(currentReportId.value)
                if (currentRep != null) {
                    val pdfFile = PdfGenerator.generatePdf(getApplication(), currentRep, profile)
                    generatedPdfFile.value = pdfFile

                    // Save to history
                    repository.savePdfHistory(
                        PdfHistory(
                            dailyReportId = currentRep.report.id,
                            date = currentRep.report.date,
                            location = currentRep.report.location,
                            filePath = pdfFile.absolutePath,
                            fileName = pdfFile.name,
                            fileSize = pdfFile.length()
                        )
                    )
                    onComplete(pdfFile)
                } else {
                    errorMessage.value = "Simpan laporan terlebih dahulu sebelum membuat PDF."
                    onComplete(null)
                }
            } catch (e: Exception) {
                errorMessage.value = "Gagal membuat PDF: ${e.localizedMessage}"
                onComplete(null)
            } finally {
                isGeneratingPdf.value = false
            }
        }
    }

    fun generatePdfForReportId(reportId: Long, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            isGeneratingPdf.value = true
            try {
                val profile = repository.getProfileDirect()
                val rep = repository.getReportByIdDirect(reportId)
                if (rep != null) {
                    val pdfFile = PdfGenerator.generatePdf(getApplication(), rep, profile)
                    repository.savePdfHistory(
                        PdfHistory(
                            dailyReportId = rep.report.id,
                            date = rep.report.date,
                            location = rep.report.location,
                            filePath = pdfFile.absolutePath,
                            fileName = pdfFile.name,
                            fileSize = pdfFile.length()
                        )
                    )
                    onComplete(pdfFile)
                } else {
                    onComplete(null)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(null)
            } finally {
                isGeneratingPdf.value = false
            }
        }
    }

    fun deleteReport(reportId: Long, onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.deleteReport(reportId)
            onComplete()
        }
    }

    fun deletePdfHistory(id: Long) {
        viewModelScope.launch {
            repository.deletePdfHistory(id)
        }
    }

    fun seedDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            onComplete()
        }
    }
}

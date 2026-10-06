package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.WorkItem
import com.example.data.entity.WorkPhoto
import com.example.data.entity.WorkProgressHistory
import com.example.data.model.WorkItemWithPhotos
import com.example.data.repository.ReportRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class WorkItemViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(AppDatabase.getInstance(application), application)

    val ongoingWorkItems = repository.ongoingWorkItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _statusFilter = MutableStateFlow("ALL") // "ALL", "BERJALAN", "SELESAI"
    val statusFilter = _statusFilter.asStateFlow()

    val searchResults: StateFlow<List<WorkItemWithPhotos>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.ongoingWorkItems
            } else {
                repository.searchWorkItems(query)
            }
        }
        .combine(_statusFilter) { items, filter ->
            when (filter) {
                "BERJALAN" -> items.filter { it.workItem.currentProgress < 100 }
                "SELESAI" -> items.filter { it.workItem.currentProgress >= 100 }
                else -> items
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setStatusFilter(filter: String) {
        _statusFilter.value = filter
    }

    fun getWorkItemDetails(id: Long): Flow<WorkItemWithPhotos?> =
        repository.getWorkItemById(id)

    fun getProgressHistory(permanentWorkId: String): Flow<List<WorkProgressHistory>> =
        repository.getProgressHistory(permanentWorkId)

    fun updateProgress(
        workItemId: Long,
        newProgress: Int,
        notes: String,
        newPhotos: List<WorkPhoto>,
        onComplete: () -> Unit
    ) {
        viewModelScope.launch {
            repository.updateWorkItemProgress(
                workItemId = workItemId,
                newProgress = newProgress,
                date = DateUtils.getTodayIso(),
                notes = notes,
                newPhotos = newPhotos
            )
            onComplete()
        }
    }
}

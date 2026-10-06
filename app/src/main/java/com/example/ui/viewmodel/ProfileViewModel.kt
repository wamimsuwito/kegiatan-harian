package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.entity.UserProfile
import com.example.data.repository.ReportRepository
import com.example.util.FileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(AppDatabase.getInstance(application), application)

    val profile = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    var backupStatus = MutableStateFlow<String?>(null)
    var isBackingUp = MutableStateFlow(false)

    fun saveProfile(name: String, position: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            val updated = (current ?: UserProfile()).copy(
                name = name.trim(),
                position = position.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProfile(updated)
            onComplete()
        }
    }

    fun saveSignature(bitmap: Bitmap, onComplete: () -> Unit) {
        viewModelScope.launch {
            val path = FileUtils.saveSignatureBitmap(getApplication(), bitmap)
            val current = repository.getProfileDirect()
            val updated = (current ?: UserProfile()).copy(
                signaturePath = path,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveProfile(updated)
            onComplete()
        }
    }

    fun clearSignature(onComplete: () -> Unit) {
        viewModelScope.launch {
            val current = repository.getProfileDirect()
            if (current != null) {
                current.signaturePath?.let {
                    val file = File(it)
                    if (file.exists()) file.delete()
                }
                repository.saveProfile(current.copy(signaturePath = null))
            }
            onComplete()
        }
    }

    fun backupData(onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            isBackingUp.value = true
            try {
                val backupDir = File(getApplication<Application>().getExternalFilesDir(null), "backups")
                if (!backupDir.exists()) backupDir.mkdirs()
                val backupFile = File(backupDir, "backup_laporan_${System.currentTimeMillis()}.zip")
                val success = FileUtils.createBackupZip(getApplication(), backupFile)
                if (success) {
                    backupStatus.value = "Backup berhasil disimpan di: ${backupFile.name}"
                    onComplete(backupFile)
                } else {
                    backupStatus.value = "Gagal membuat file backup."
                    onComplete(null)
                }
            } catch (e: Exception) {
                backupStatus.value = "Error backup: ${e.localizedMessage}"
                onComplete(null)
            } finally {
                isBackingUp.value = false
            }
        }
    }

    fun restoreData(zipUri: Uri, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            isBackingUp.value = true
            try {
                val inputStream = getApplication<Application>().contentResolver.openInputStream(zipUri)
                if (inputStream != null) {
                    val success = FileUtils.restoreBackupZip(getApplication(), inputStream)
                    backupStatus.value = if (success) "Restore berhasil! Silakan restart aplikasi." else "Gagal merestore file backup."
                    onComplete(success)
                } else {
                    backupStatus.value = "File backup tidak dapat dibaca."
                    onComplete(false)
                }
            } catch (e: Exception) {
                backupStatus.value = "Error restore: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                isBackingUp.value = false
            }
        }
    }
}

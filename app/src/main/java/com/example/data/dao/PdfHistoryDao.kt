package com.example.data.dao

import androidx.room.*
import com.example.data.entity.PdfHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface PdfHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPdfHistory(pdfHistory: PdfHistory): Long

    @Query("SELECT * FROM pdf_history ORDER BY createdAt DESC")
    fun getAllPdfHistory(): Flow<List<PdfHistory>>

    @Query("SELECT * FROM pdf_history WHERE dailyReportId = :reportId ORDER BY createdAt DESC")
    fun getPdfHistoryByReportId(reportId: Long): Flow<List<PdfHistory>>

    @Query("DELETE FROM pdf_history WHERE id = :id")
    suspend fun deletePdfHistoryById(id: Long)
}

package com.example.data.dao

import androidx.room.*
import com.example.data.entity.DailyReport
import com.example.data.model.DailyReportWithDetails
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: DailyReport): Long

    @Update
    suspend fun updateReport(report: DailyReport)

    @Delete
    suspend fun deleteReport(report: DailyReport)

    @Query("DELETE FROM daily_reports WHERE id = :reportId")
    suspend fun deleteReportById(reportId: Long)

    @Query("SELECT * FROM daily_reports ORDER BY date DESC, id DESC")
    fun getAllReports(): Flow<List<DailyReport>>

    @Transaction
    @Query("SELECT * FROM daily_reports ORDER BY date DESC, id DESC")
    fun getAllReportsWithDetails(): Flow<List<DailyReportWithDetails>>

    @Transaction
    @Query("SELECT * FROM daily_reports WHERE id = :reportId")
    fun getReportWithDetailsById(reportId: Long): Flow<DailyReportWithDetails?>

    @Transaction
    @Query("SELECT * FROM daily_reports WHERE id = :reportId")
    suspend fun getReportWithDetailsByIdDirect(reportId: Long): DailyReportWithDetails?

    @Transaction
    @Query("SELECT * FROM daily_reports WHERE date = :date LIMIT 1")
    suspend fun getReportByDate(date: String): DailyReportWithDetails?

    @Query("SELECT * FROM daily_reports WHERE date LIKE :monthPrefix || '%' ORDER BY date DESC")
    fun getReportsByMonth(monthPrefix: String): Flow<List<DailyReport>>

    @Query("SELECT COUNT(*) FROM daily_reports WHERE date = :today")
    fun getTodayReportCount(today: String): Flow<Int>
}

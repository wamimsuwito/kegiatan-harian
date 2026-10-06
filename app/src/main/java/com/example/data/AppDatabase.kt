package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.PdfHistoryDao
import com.example.data.dao.ReportDao
import com.example.data.dao.UserProfileDao
import com.example.data.dao.WorkItemDao
import com.example.data.entity.DailyReport
import com.example.data.entity.PdfHistory
import com.example.data.entity.UserProfile
import com.example.data.entity.WorkItem
import com.example.data.entity.WorkPhoto
import com.example.data.entity.WorkProgressHistory

@Database(
    entities = [
        UserProfile::class,
        DailyReport::class,
        WorkItem::class,
        WorkProgressHistory::class,
        WorkPhoto::class,
        PdfHistory::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao
    abstract fun workItemDao(): WorkItemDao
    abstract fun userProfileDao(): UserProfileDao
    abstract fun pdfHistoryDao(): PdfHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "laporan_kegiatan_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

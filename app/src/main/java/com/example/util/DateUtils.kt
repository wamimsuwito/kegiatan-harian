package com.example.util

import java.text.SimpleDateFormat
import java.util.*

object DateUtils {
    private val localeId = Locale("id", "ID")

    fun getTodayIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date())
    }

    fun getTodayDmy(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        return sdf.format(Date())
    }

    fun formatToIndonesianDate(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(isoDate) ?: return isoDate
            val formatter = SimpleDateFormat("dd MMMM yyyy", localeId)
            formatter.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatToDmy(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(isoDate) ?: return isoDate
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            formatter.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun dmyToIso(dmyDate: String): String {
        return try {
            val parser = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            val date = parser.parse(dmyDate) ?: return dmyDate
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            formatter.format(date)
        } catch (e: Exception) {
            dmyDate
        }
    }

    fun getMonthYearLabel(isoDate: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = parser.parse(isoDate) ?: return isoDate
            val formatter = SimpleDateFormat("MMMM yyyy", localeId)
            formatter.format(date)
        } catch (e: Exception) {
            isoDate
        }
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", localeId)
        return sdf.format(Date(timestamp))
    }
}

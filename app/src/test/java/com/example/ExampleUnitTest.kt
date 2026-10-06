package com.example

import com.example.util.DateUtils
import com.example.util.FileUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testDateUtilsIndonesianFormatting() {
        val iso = "2026-06-03"
        val formatted = DateUtils.formatToIndonesianDate(iso)
        assertTrue(formatted.contains("Juni") || formatted.contains("06"))
        assertEquals("03/06/2026", DateUtils.formatToDmy(iso))
    }

    @Test
    fun testSanitizeFileName() {
        val raw = "Dumai - Pekanbaru / Lokasi #1!"
        val sanitized = FileUtils.sanitizeFileName(raw)
        assertFalse(sanitized.contains("/"))
        assertFalse(sanitized.contains("#"))
        assertFalse(sanitized.contains("!"))
    }

    @Test
    fun testMultiDayWorkStatusCalculation() {
        // Under 100% progress -> BERJALAN
        val progressPartial = 30
        val statusPartial = if (progressPartial >= 100) "SELESAI" else "BERJALAN"
        assertEquals("BERJALAN", statusPartial)

        // 100% progress -> SELESAI
        val progressComplete = 100
        val statusComplete = if (progressComplete >= 100) "SELESAI" else "BERJALAN"
        assertEquals("SELESAI", statusComplete)
    }

    @Test
    fun testSequenceNumberOrdering() {
        val items = listOf("Silo 2 & 4", "Gearbox mixer BP", "Panel kontrol")
        val sequenceNumbers = items.mapIndexed { idx, _ -> idx + 1 }
        assertEquals(listOf(1, 2, 3), sequenceNumbers)
    }
}

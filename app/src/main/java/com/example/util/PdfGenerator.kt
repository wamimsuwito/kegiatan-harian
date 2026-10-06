package com.example.util

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import com.example.data.entity.UserProfile
import com.example.data.model.DailyReportWithDetails
import com.example.data.model.WorkItemWithPhotos
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object PdfGenerator {

    // A4 Landscape in points (72 points per inch)
    private const val PAGE_WIDTH = 842
    private const val PAGE_HEIGHT = 595
    private const val MARGIN = 30f

    // Column widths summing to 782 (842 - 60)
    private const val COL_NO_W = 25f
    private const val COL_DESC_W = 155f
    private const val COL_TARGET_W = 85f
    private const val COL_DOC_W = 240f // 80f each for Before, Process, After
    private const val COL_WORKERS_W = 85f
    private const val COL_TOOLS_W = 75f
    private const val COL_PROG_W = 55f
    private const val COL_NOTES_W = 62f

    fun generatePdf(
        context: Context,
        reportWithDetails: DailyReportWithDetails,
        profile: UserProfile?
    ): File {
        val report = reportWithDetails.report
        val items = reportWithDetails.workItems

        val outputFile = FileUtils.getPdfOutputFile(context, report.date, report.location)
        val document = PdfDocument()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 8.5f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 9f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val headerTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 14f
            color = Color.BLACK
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val borderPaint = Paint().apply {
            color = Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        val headerBgPaint = Paint().apply {
            color = Color.rgb(238, 242, 246)
            style = Paint.Style.FILL
        }

        // Layout pagination calculation
        // Header occupies Y from 25 to 75 (height 50)
        // Table Header occupies Y 75 to 105 (height 30)
        // Table rows occupy Y 105 to (PAGE_HEIGHT - 35) on normal pages
        // On the final page, we need space for Footer (~110pt) -> Max Y is (PAGE_HEIGHT - 130)
        val headerH = 50f
        val tableHeaderH = 30f
        val startY = MARGIN + headerH + tableHeaderH
        val rowHeight = 75f // fixed row height that fits 3 photos of ~55pt height plus labels

        // Group items into pages
        val pagesItems = mutableListOf<List<WorkItemWithPhotos>>()
        var remainingItems = items.toMutableList()

        if (remainingItems.isEmpty()) {
            pagesItems.add(emptyList())
        } else {
            var currentPage = mutableListOf<WorkItemWithPhotos>()
            var currentY = startY

            for (item in remainingItems) {
                // Reserve 120pt for footer if this is the last batch
                val neededHeight = rowHeight
                val availableH = PAGE_HEIGHT - MARGIN - 30f

                if (currentY + neededHeight > availableH) {
                    pagesItems.add(currentPage)
                    currentPage = mutableListOf(item)
                    currentY = startY + neededHeight
                } else {
                    currentPage.add(item)
                    currentY += neededHeight
                }
            }
            if (currentPage.isNotEmpty()) {
                pagesItems.add(currentPage)
            }
        }

        // Check if footer fits on the last page. If not, add a blank page for footer
        val lastPageItems = pagesItems.lastOrNull() ?: emptyList()
        val lastPageContentH = startY + (lastPageItems.size * rowHeight)
        val footerNeededH = 120f
        val needExtraPageForFooter = (lastPageContentH + footerNeededH > PAGE_HEIGHT - MARGIN)

        val totalPages = if (needExtraPageForFooter) pagesItems.size + 1 else pagesItems.size

        // Draw Pages
        for (pageIndex in 0 until pagesItems.size) {
            val pageNum = pageIndex + 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // 1. Draw Page Header
            drawPageHeader(
                canvas, report.date, report.location, pageNum, totalPages,
                headerTitlePaint, boldPaint, textPaint
            )

            // 2. Draw Table Header
            val tableTopY = MARGIN + headerH
            drawTableHeader(canvas, tableTopY, tableHeaderH, headerBgPaint, borderPaint, boldPaint)

            // 3. Draw Rows for this page
            val currentItems = pagesItems[pageIndex]
            var currentY = tableTopY + tableHeaderH

            currentItems.forEach { itemWithPhotos ->
                drawItemRow(
                    context, canvas, itemWithPhotos, currentY, rowHeight,
                    borderPaint, textPaint, boldPaint
                )
                currentY += rowHeight
            }

            // 4. Draw Footer if this is the last items page and fits
            val isFinalPage = (pageNum == totalPages)
            if (isFinalPage && !needExtraPageForFooter) {
                drawPageFooter(
                    context, canvas, report.location, report.date, report.workPlanNext,
                    profile, currentY + 12f, borderPaint, textPaint, boldPaint
                )
            }

            document.finishPage(page)
        }

        // If extra page needed for footer
        if (needExtraPageForFooter) {
            val pageNum = totalPages
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNum).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            drawPageHeader(
                canvas, report.date, report.location, pageNum, totalPages,
                headerTitlePaint, boldPaint, textPaint
            )

            drawPageFooter(
                context, canvas, report.location, report.date, report.workPlanNext,
                profile, MARGIN + headerH + 20f, borderPaint, textPaint, boldPaint
            )

            document.finishPage(page)
        }

        // Write to output file
        val fos = FileOutputStream(outputFile)
        document.writeTo(fos)
        fos.flush()
        fos.close()
        document.close()

        return outputFile
    }

    private fun drawPageHeader(
        canvas: Canvas,
        dateIso: String,
        location: String,
        pageNum: Int,
        totalPages: Int,
        titlePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint
    ) {
        // Centered Title
        canvas.drawText("LAPORAN KEGIATAN HARIAN", PAGE_WIDTH / 2f, MARGIN + 14f, titlePaint)

        // Metadata Left
        val formattedDate = DateUtils.formatToIndonesianDate(dateIso)
        canvas.drawText("TANGGAL : $formattedDate", MARGIN, MARGIN + 32f, boldPaint)
        canvas.drawText("LOKASI   : $location", MARGIN, MARGIN + 45f, boldPaint)

        // Page Number Right
        val pageText = "Halaman $pageNum dari $totalPages"
        val pageTextW = textPaint.measureText(pageText)
        canvas.drawText(pageText, PAGE_WIDTH - MARGIN - pageTextW, MARGIN + 45f, textPaint)
    }

    private fun drawTableHeader(
        canvas: Canvas,
        topY: Float,
        height: Float,
        bgPaint: Paint,
        borderPaint: Paint,
        boldPaint: Paint
    ) {
        val bottomY = topY + height
        val rightX = PAGE_WIDTH - MARGIN

        // Background
        canvas.drawRect(MARGIN, topY, rightX, bottomY, bgPaint)
        canvas.drawRect(MARGIN, topY, rightX, bottomY, borderPaint)

        val cols = getColumnXCoordinates()

        // Vertical lines
        for (i in 1 until cols.size - 1) {
            canvas.drawLine(cols[i], topY, cols[i], bottomY, borderPaint)
        }

        // Column Titles
        val centerH = topY + (height / 2f) + 3f
        val alignCenterPaint = Paint(boldPaint).apply { textAlign = Paint.Align.CENTER }

        canvas.drawText("NO", cols[0] + (COL_NO_W / 2), centerH, alignCenterPaint)
        canvas.drawText("DESKRIPSI KEGIATAN", cols[1] + (COL_DESC_W / 2), centerH, alignCenterPaint)
        canvas.drawText("TARGET", cols[2] + (COL_TARGET_W / 2), centerH, alignCenterPaint)

        // Documentation Header with subcolumns
        val docLeft = cols[3]
        val docMidY = topY + (height / 2f)
        canvas.drawText("DOKUMENTASI PEKERJAAN", docLeft + (COL_DOC_W / 2), topY + 12f, alignCenterPaint)
        canvas.drawLine(docLeft, docMidY, docLeft + COL_DOC_W, docMidY, borderPaint)

        // Subheaders: SEBELUM | PROSES | SESUDAH
        val subW = COL_DOC_W / 3f
        val subPaint = Paint(boldPaint).apply {
            textSize = 7.5f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("SEBELUM", docLeft + (subW * 0.5f), bottomY - 3f, subPaint)
        canvas.drawLine(docLeft + subW, docMidY, docLeft + subW, bottomY, borderPaint)
        canvas.drawText("PROSES", docLeft + (subW * 1.5f), bottomY - 3f, subPaint)
        canvas.drawLine(docLeft + (subW * 2), docMidY, docLeft + (subW * 2), bottomY, borderPaint)
        canvas.drawText("SESUDAH", docLeft + (subW * 2.5f), bottomY - 3f, subPaint)

        canvas.drawText("PEKERJA", cols[4] + (COL_WORKERS_W / 2), centerH, alignCenterPaint)
        canvas.drawText("ALAT KERJA", cols[5] + (COL_TOOLS_W / 2), centerH, alignCenterPaint)
        canvas.drawText("PROGRESS", cols[6] + (COL_PROG_W / 2), centerH, alignCenterPaint)
        canvas.drawText("KETERANGAN", cols[7] + (COL_NOTES_W / 2), centerH, alignCenterPaint)
    }

    private fun getColumnXCoordinates(): FloatArray {
        var x = MARGIN
        val x0 = x
        x += COL_NO_W
        val x1 = x
        x += COL_DESC_W
        val x2 = x
        x += COL_TARGET_W
        val x3 = x
        x += COL_DOC_W
        val x4 = x
        x += COL_WORKERS_W
        val x5 = x
        x += COL_TOOLS_W
        val x6 = x
        x += COL_PROG_W
        val x7 = x
        x += COL_NOTES_W
        val x8 = x
        return floatArrayOf(x0, x1, x2, x3, x4, x5, x6, x7, x8)
    }

    private fun drawItemRow(
        context: Context,
        canvas: Canvas,
        itemWithPhotos: WorkItemWithPhotos,
        topY: Float,
        height: Float,
        borderPaint: Paint,
        textPaint: Paint,
        boldPaint: Paint
    ) {
        val bottomY = topY + height
        val rightX = PAGE_WIDTH - MARGIN
        val cols = getColumnXCoordinates()

        // Outer box & column dividers
        canvas.drawRect(MARGIN, topY, rightX, bottomY, borderPaint)
        for (i in 1 until cols.size - 1) {
            canvas.drawLine(cols[i], topY, cols[i], bottomY, borderPaint)
        }

        val item = itemWithPhotos.workItem

        // Col 0: NO
        val alignCenterPaint = Paint(boldPaint).apply { textAlign = Paint.Align.CENTER }
        canvas.drawText("${item.sequenceNumber}", cols[0] + (COL_NO_W / 2), topY + (height / 2) + 4f, alignCenterPaint)

        // Col 1: DESKRIPSI KEGIATAN (Multi-line text wrap)
        drawWrappedText(canvas, item.description, cols[1] + 4f, topY + 12f, COL_DESC_W - 8f, 10f, textPaint)

        // Col 2: TARGET
        val targetTextPaint = Paint(textPaint).apply { textSize = 7.5f }
        val targetBoldPaint = Paint(boldPaint).apply { textSize = 7.5f }
        var targetY = topY + 10f
        canvas.drawText("TARGET MULAI:", cols[2] + 4f, targetY, targetBoldPaint)
        targetY += 9f
        canvas.drawText(item.targetStartDate, cols[2] + 4f, targetY, targetTextPaint)
        targetY += 12f
        canvas.drawText("TARGET SELESAI:", cols[2] + 4f, targetY, targetBoldPaint)
        targetY += 9f
        canvas.drawText(item.targetEndDate, cols[2] + 4f, targetY, targetTextPaint)
        if (item.realizationDate.isNotBlank()) {
            targetY += 12f
            canvas.drawText("REALISASI:", cols[2] + 4f, targetY, targetBoldPaint)
            targetY += 9f
            canvas.drawText(item.realizationDate, cols[2] + 4f, targetY, targetTextPaint)
        }

        // Col 3: DOKUMENTASI (SEBELUM, PROSES, SESUDAH)
        val docLeft = cols[3]
        val subW = COL_DOC_W / 3f
        val photoPadding = 3f
        val photoW = subW - (photoPadding * 2)
        val photoH = height - 10f
        val photoY = topY + 5f

        // Sub-column dividers
        canvas.drawLine(docLeft + subW, topY, docLeft + subW, bottomY, borderPaint)
        canvas.drawLine(docLeft + (subW * 2), topY, docLeft + (subW * 2), bottomY, borderPaint)

        // Draw Before Photo
        drawPhotoCell(canvas, itemWithPhotos.beforePhoto?.filePath, docLeft + photoPadding, photoY, photoW, photoH, "SEBELUM")
        // Draw Process Photo
        drawPhotoCell(canvas, itemWithPhotos.processPhoto?.filePath, docLeft + subW + photoPadding, photoY, photoW, photoH, "PROSES")
        // Draw After Photo
        drawPhotoCell(canvas, itemWithPhotos.afterPhoto?.filePath, docLeft + (subW * 2) + photoPadding, photoY, photoW, photoH, "SESUDAH")

        // Col 4: PEKERJA
        drawWrappedText(canvas, item.workers, cols[4] + 4f, topY + 12f, COL_WORKERS_W - 8f, 10f, textPaint)

        // Col 5: ALAT KERJA
        drawWrappedText(canvas, item.tools, cols[5] + 4f, topY + 12f, COL_TOOLS_W - 8f, 10f, textPaint)

        // Col 6: PROGRESS
        val progText = "${item.currentProgress}%"
        val progColor = if (item.currentProgress >= 100) Color.rgb(22, 101, 52) else Color.rgb(30, 64, 175)
        val progPaint = Paint(boldPaint).apply {
            textSize = 11f
            color = progColor
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(progText, cols[6] + (COL_PROG_W / 2), topY + (height / 2) + 4f, progPaint)

        // Col 7: KETERANGAN
        drawWrappedText(canvas, item.notes, cols[7] + 4f, topY + 12f, COL_NOTES_W - 8f, 10f, textPaint)
    }

    private fun drawPhotoCell(
        canvas: Canvas,
        filePath: String?,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String
    ) {
        val cellRect = RectF(x, y, x + w, y + h)
        if (filePath != null && File(filePath).exists()) {
            try {
                // Decode scaled sample bitmap
                val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                val bmp = BitmapFactory.decodeFile(filePath, options)
                if (bmp != null) {
                    val destRect = RectF(x, y, x + w, y + h)
                    canvas.drawBitmap(bmp, null, destRect, null)
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Placeholder if no photo
        val bgPaint = Paint().apply {
            color = Color.rgb(245, 245, 245)
            style = Paint.Style.FILL
        }
        val borderP = Paint().apply {
            color = Color.LTGRAY
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }
        val labelP = Paint().apply {
            color = Color.GRAY
            textSize = 7f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawRect(cellRect, bgPaint)
        canvas.drawRect(cellRect, borderP)
        canvas.drawText("[$label]", x + (w / 2), y + (h / 2) + 3f, labelP)
    }

    private fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        lineHeight: Float,
        paint: Paint
    ) {
        if (text.isBlank()) return
        val words = text.split(" ")
        var currentLine = ""
        var currentY = startY

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val testWidth = paint.measureText(testLine)
            if (testWidth > maxWidth && currentLine.isNotEmpty()) {
                canvas.drawText(currentLine, x, currentY, paint)
                currentLine = word
                currentY += lineHeight
            } else {
                currentLine = testLine
            }
        }
        if (currentLine.isNotEmpty()) {
            canvas.drawText(currentLine, x, currentY, paint)
        }
    }

    private fun drawPageFooter(
        context: Context,
        canvas: Canvas,
        location: String,
        dateIso: String,
        workPlanNext: String,
        profile: UserProfile?,
        topY: Float,
        borderPaint: Paint,
        textPaint: Paint,
        boldPaint: Paint
    ) {
        // Left Box: Rencana kerja hari ini
        val leftW = 400f
        canvas.drawText("Rencana kerja hari ini / selanjutnya:", MARGIN, topY + 12f, boldPaint)
        val planText = if (workPlanNext.isNotBlank()) workPlanNext else "-"
        drawWrappedText(canvas, planText, MARGIN, topY + 26f, leftW, 11f, textPaint)

        // Right Box: Dilaporkan Oleh & Signature
        val rightCenterX = PAGE_WIDTH - MARGIN - 120f
        val formattedDate = DateUtils.formatToIndonesianDate(dateIso)
        val alignCenterBold = Paint(boldPaint).apply { textAlign = Paint.Align.CENTER }
        val alignCenterText = Paint(textPaint).apply { textAlign = Paint.Align.CENTER }

        val locDate = "${location.take(20)}, $formattedDate"
        canvas.drawText(locDate, rightCenterX, topY + 12f, alignCenterText)
        canvas.drawText("Dilaporkan Oleh,", rightCenterX, topY + 25f, alignCenterBold)

        // Signature Bitmap
        val sigPath = profile?.signaturePath
        var sigDrawn = false
        if (sigPath != null && File(sigPath).exists()) {
            try {
                val sigBmp = BitmapFactory.decodeFile(sigPath)
                if (sigBmp != null) {
                    val sigW = 90f
                    val sigH = 45f
                    val sigRect = RectF(rightCenterX - (sigW / 2), topY + 28f, rightCenterX + (sigW / 2), topY + 28f + sigH)
                    canvas.drawBitmap(sigBmp, null, sigRect, null)
                    sigDrawn = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val nameY = topY + if (sigDrawn) 78f else 65f
        val name = profile?.name?.ifBlank { "WAMIN" } ?: "WAMIN"
        val position = profile?.position?.ifBlank { "Pelaksana Lapangan" } ?: "Pelaksana Lapangan"

        val underlinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.8f
        }
        val nameWidth = alignCenterBold.measureText(name)
        canvas.drawText(name, rightCenterX, nameY, alignCenterBold)
        canvas.drawLine(rightCenterX - (nameWidth / 2), nameY + 2f, rightCenterX + (nameWidth / 2), nameY + 2f, underlinePaint)

        canvas.drawText(position, rightCenterX, nameY + 14f, alignCenterText)
    }
}

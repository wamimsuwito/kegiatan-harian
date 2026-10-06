package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.entity.PdfHistory
import com.example.data.model.DailyReportWithDetails
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReportViewModel
import com.example.util.DateUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    onSelectReport: (DailyReportWithDetails) -> Unit
) {
    val context = LocalContext.current
    val allReports by viewModel.allReports.collectAsState()
    val pdfHistory by viewModel.pdfHistory.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var reportToDelete by remember { mutableStateOf<DailyReportWithDetails?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfHistory?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button_history")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SlateSurface)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = NavyPrimary
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Laporan Harian (${allReports.size})", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("File PDF (${pdfHistory.size})", fontWeight = FontWeight.Bold) }
                )
            }

            if (selectedTabIndex == 0) {
                // Group reports by Year and Month
                val groupedReports = remember(allReports) {
                    allReports.groupBy { it.report.date.take(7) } // YYYY-MM
                }

                if (allReports.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada riwayat laporan tersimpan.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        groupedReports.forEach { (yearMonth, reportsInMonth) ->
                            item {
                                Surface(
                                    color = NavyPrimary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = DateUtils.getMonthYearLabel("$yearMonth-01"),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = NavyPrimary,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            items(reportsInMonth) { reportWithDetails ->
                                HistoryReportItem(
                                    reportWithDetails = reportWithDetails,
                                    onClick = { onSelectReport(reportWithDetails) },
                                    onGeneratePdf = {
                                        viewModel.generatePdfForReportId(reportWithDetails.report.id) { file ->
                                            if (file != null) {
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/pdf")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                try {
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Toast.makeText(context, "PDF berhasil dibuat: ${file.name}", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    },
                                    onDelete = { reportToDelete = reportWithDetails }
                                )
                            }
                        }
                    }
                }
            } else {
                // PDF History
                if (pdfHistory.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Belum ada file PDF yang dibuat.", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(pdfHistory) { pdf ->
                            PdfHistoryItem(
                                pdf = pdf,
                                onOpen = {
                                    val file = File(pdf.filePath)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, "application/pdf")
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Tidak ada penampil PDF terpasang", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "File PDF tidak ditemukan di perangkat", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onShare = {
                                    val file = File(pdf.filePath)
                                    if (file.exists()) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            putExtra(Intent.EXTRA_SUBJECT, pdf.fileName)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF"))
                                    }
                                },
                                onDelete = { pdfToDelete = pdf }
                            )
                        }
                    }
                }
            }
        }
    }

    if (reportToDelete != null) {
        val rep = reportToDelete!!
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Hapus Laporan?") },
            text = { Text("Laporan tanggal ${DateUtils.formatToIndonesianDate(rep.report.date)} akan dihapus permanen.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rep.report.id
                        reportToDelete = null
                        viewModel.deleteReport(id) {
                            Toast.makeText(context, "Laporan dihapus", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { reportToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (pdfToDelete != null) {
        val pdf = pdfToDelete!!
        AlertDialog(
            onDismissRequest = { pdfToDelete = null },
            title = { Text("Hapus Riwayat PDF?") },
            text = { Text("File ${pdf.fileName} akan dihapus dari riwayat.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePdfHistory(pdf.id)
                        pdfToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { pdfToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun HistoryReportItem(
    reportWithDetails: DailyReportWithDetails,
    onClick: () -> Unit,
    onGeneratePdf: () -> Unit,
    onDelete: () -> Unit
) {
    val report = reportWithDetails.report
    val items = reportWithDetails.workItems
    val allCompleted = items.isNotEmpty() && items.all { it.workItem.currentProgress >= 100 }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = DateUtils.formatToIndonesianDate(report.date),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${report.location} • ${items.size} kegiatan",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = if (allCompleted) "SELESAI" else "BERJALAN")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onGeneratePdf) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = "PDF", tint = NavyPrimary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun PdfHistoryItem(
    pdf: PdfHistory,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = pdf.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${DateUtils.formatTimestamp(pdf.createdAt)} • ${pdf.fileSize / 1024} KB",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                IconButton(onClick = onOpen) {
                    Icon(Icons.Default.Visibility, contentDescription = "Buka PDF")
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Default.Share, contentDescription = "Bagikan")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

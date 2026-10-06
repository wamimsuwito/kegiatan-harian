package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.DailyReportWithDetails
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReportViewModel
import com.example.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ReportViewModel,
    onCreateReport: () -> Unit,
    onViewOngoing: () -> Unit,
    onViewHistory: () -> Unit,
    onViewSearch: () -> Unit,
    onViewProfile: () -> Unit,
    onViewSettings: () -> Unit,
    onEditReport: (DailyReportWithDetails) -> Unit
) {
    val context = LocalContext.current
    val allReports by viewModel.allReports.collectAsState()
    val todayWorkCount by viewModel.todayWorkCount.collectAsState()
    val todayCompletedCount by viewModel.todayCompletedCount.collectAsState()
    val todayOngoingCount by viewModel.todayOngoingCount.collectAsState()
    val ongoingItems by viewModel.ongoingWorkItems.collectAsState()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsState()

    var reportToDelete by remember { mutableStateOf<DailyReportWithDetails?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LAPORAN KEGIATAN HARIAN",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Pencatatan & PDF Pekerjaan Lapangan",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(
                        onClick = onViewProfile,
                        modifier = Modifier.testTag("nav_profile_button")
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profil Pelapor", tint = Color.White)
                    }
                    IconButton(
                        onClick = onViewSettings,
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Pengaturan", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.resetForm()
                    onCreateReport()
                },
                containerColor = AmberSecondary,
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Buat Laporan", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("fab_create_report")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SlateSurface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card (Hari Ini)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = NavyDark)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "RINGKASAN HARI INI",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberLight
                                )
                                Text(
                                    text = DateUtils.formatToIndonesianDate(DateUtils.getTodayIso()),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = AmberLight,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            StatBox(
                                title = "Total Kegiatan",
                                value = "$todayWorkCount",
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Selesai",
                                value = "$todayCompletedCount",
                                color = Color(0xFF4ADE80),
                                modifier = Modifier.weight(1f)
                            )
                            StatBox(
                                title = "Berjalan",
                                value = "${ongoingItems.size}",
                                color = Color(0xFF60A5FA),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Quick Menu Grid
            item {
                Text(
                    text = "MENU UTAMA",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuTile(
                        icon = Icons.Default.Edit,
                        title = "Buat Laporan Hari Ini",
                        subtitle = "Catat kegiatan & foto",
                        color = NavyPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_create_today"),
                        onClick = {
                            viewModel.resetForm()
                            onCreateReport()
                        }
                    )
                    MenuTile(
                        icon = Icons.Default.PendingActions,
                        title = "Pekerjaan Berjalan",
                        subtitle = "${ongoingItems.size} pekerjaan aktif",
                        color = AmberSecondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_ongoing_work"),
                        onClick = onViewOngoing
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuTile(
                        icon = Icons.Default.History,
                        title = "Riwayat Laporan",
                        subtitle = "${allReports.size} laporan tersimpan",
                        color = Color(0xFF0D9488),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_history"),
                        onClick = onViewHistory
                    )
                    MenuTile(
                        icon = Icons.Default.Search,
                        title = "Cari Kegiatan",
                        subtitle = "Filter nama, lokasi, alat",
                        color = Color(0xFF4F46E5),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("menu_search"),
                        onClick = onViewSearch
                    )
                }
            }

            // Recent Reports Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LAPORAN TERBARU",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = onViewHistory) {
                        Text("Lihat Semua")
                    }
                }
            }

            if (allReports.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AssignmentLate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Belum Ada Laporan",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Mulai buat laporan harian baru atau muat data contoh di Pengaturan.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            } else {
                items(allReports.take(5)) { reportWithDetails ->
                    ReportCardItem(
                        reportWithDetails = reportWithDetails,
                        onClick = { onEditReport(reportWithDetails) },
                        onGeneratePdf = {
                            viewModel.generatePdfForReportId(reportWithDetails.report.id) { pdfFile ->
                                if (pdfFile != null) {
                                    val uri = FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.fileprovider",
                                        pdfFile
                                    )
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "PDF berhasil dibuat: ${pdfFile.name}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        onDelete = { reportToDelete = reportWithDetails }
                    )
                }
            }
        }
    }

    // Confirm Delete Dialog
    if (reportToDelete != null) {
        val rep = reportToDelete!!
        AlertDialog(
            onDismissRequest = { reportToDelete = null },
            title = { Text("Hapus Laporan?") },
            text = {
                Text("Laporan tanggal ${DateUtils.formatToIndonesianDate(rep.report.date)} di ${rep.report.location} dan seluruh ${rep.workItems.size} kegiatan di dalamnya akan dihapus.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val id = rep.report.id
                        reportToDelete = null
                        viewModel.deleteReport(id) {
                            Toast.makeText(context, "Laporan berhasil dihapus", Toast.LENGTH_SHORT).show()
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
}

@Composable
fun StatBox(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.08f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun MenuTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(115.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ReportCardItem(
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
            .clickable(onClick = onClick)
            .testTag("report_card_${report.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = DateUtils.formatToIndonesianDate(report.date),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = AmberSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = report.location,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(status = if (allCompleted) "SELESAI" else "BERJALAN")
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "${items.size} Kegiatan:",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            items.take(3).forEach { itemWithPhotos ->
                val itm = itemWithPhotos.workItem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${itm.sequenceNumber}. ${itm.description}",
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${itm.currentProgress}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (itm.currentProgress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            if (items.size > 3) {
                Text(
                    text = "+ ${items.size - 3} kegiatan lainnya...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onGeneratePdf,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PDF", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onClick,
                    modifier = Modifier.height(36.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp)
                ) {
                    Text("Buka / Edit", fontSize = 12.sp)
                }
            }
        }
    }
}

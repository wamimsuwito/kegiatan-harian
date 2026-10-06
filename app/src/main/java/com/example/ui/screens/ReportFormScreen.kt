package com.example.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.WorkItemWithPhotos
import com.example.ui.components.PhotoPickerSlot
import com.example.ui.components.ProgressSlider
import com.example.ui.theme.*
import com.example.ui.viewmodel.ReportViewModel
import com.example.ui.viewmodel.WorkItemDraft
import com.example.util.DateUtils
import com.example.util.FileUtils
import java.io.File
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFormScreen(
    viewModel: ReportViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOngoing: () -> Unit
) {
    val context = LocalContext.current

    val reportId by viewModel.currentReportId.collectAsState()
    val reportDate by viewModel.reportDate.collectAsState()
    val reportLocation by viewModel.reportLocation.collectAsState()
    val workPlanNext by viewModel.workPlanNext.collectAsState()
    val items by viewModel.formWorkItems.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val ongoingItems by viewModel.ongoingWorkItems.collectAsState()
    val isGeneratingPdf by viewModel.isGeneratingPdf.collectAsState()

    var showDraftDialog by remember { mutableStateOf(viewModel.hasDraft() && reportId == 0L) }
    var showImportOngoingSheet by remember { mutableStateOf(false) }
    var itemToDeleteIndex by remember { mutableStateOf<Int?>(null) }
    var generatedPdfResult by remember { mutableStateOf<File?>(null) }

    // Camera capture URI holder
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var pendingPhotoItemIndex by remember { mutableIntStateOf(-1) }
    var pendingPhotoType by remember { mutableStateOf("") } // "BEFORE", "PROCESS", "AFTER"

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null && pendingPhotoItemIndex in items.indices) {
            val savedPath = FileUtils.saveImageFromUri(context, tempCameraUri!!)
            if (savedPath != null) {
                val item = items[pendingPhotoItemIndex]
                viewModel.updateWorkItem(item.tempId) {
                    when (pendingPhotoType) {
                        "BEFORE" -> it.copy(beforePhotoPath = savedPath)
                        "PROCESS" -> it.copy(processPhotoPath = savedPath)
                        "AFTER" -> it.copy(afterPhotoPath = savedPath)
                        else -> it
                    }
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && pendingPhotoItemIndex in items.indices) {
            val savedPath = FileUtils.saveImageFromUri(context, uri)
            if (savedPath != null) {
                val item = items[pendingPhotoItemIndex]
                viewModel.updateWorkItem(item.tempId) {
                    when (pendingPhotoType) {
                        "BEFORE" -> it.copy(beforePhotoPath = savedPath)
                        "PROCESS" -> it.copy(processPhotoPath = savedPath)
                        "AFTER" -> it.copy(afterPhotoPath = savedPath)
                        else -> it
                    }
                }
            }
        }
    }

    // Auto save draft on back navigation
    BackHandler {
        if (reportId == 0L) {
            viewModel.saveDraftLocally()
        }
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (reportId == 0L) "Buat Laporan Harian" else "Edit Laporan Harian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (reportId == 0L) viewModel.saveDraftLocally()
                            onNavigateBack()
                        },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.generatePdfForCurrentReport { file ->
                                generatedPdfResult = file
                            }
                        },
                        modifier = Modifier.testTag("direct_pdf_button")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Buat PDF Langsung", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.generatePdfForCurrentReport { file ->
                                generatedPdfResult = file
                            }
                        },
                        enabled = !isSaving && !isGeneratingPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_buat_pdf")
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isGeneratingPdf) "Proses..." else "Buat PDF", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            viewModel.saveReport { success ->
                                if (success) {
                                    Toast.makeText(context, "Laporan berhasil disimpan!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        enabled = !isSaving && !isGeneratingPdf,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("btn_simpan_laporan")
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSaving) "Menyimpan..." else "Simpan", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
            // Error banner
            if (errorMessage != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorMessage!!,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Header Section: Tanggal & Lokasi
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "INFORMASI LAPORAN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Tanggal Picker Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = DateUtils.formatToIndonesianDate(reportDate),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Tanggal Laporan") },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            val cal = Calendar.getInstance()
                                            DatePickerDialog(
                                                context,
                                                { _, year, month, dayOfMonth ->
                                                    val m = String.format("%02d", month + 1)
                                                    val d = String.format("%02d", dayOfMonth)
                                                    viewModel.reportDate.value = "$year-$m-$d"
                                                },
                                                cal.get(Calendar.YEAR),
                                                cal.get(Calendar.MONTH),
                                                cal.get(Calendar.DAY_OF_MONTH)
                                            ).show()
                                        },
                                        modifier = Modifier.testTag("pick_date_button")
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("field_report_date")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = reportLocation,
                            onValueChange = { viewModel.reportLocation.value = it },
                            label = { Text("Lokasi (wajib)") },
                            placeholder = { Text("Contoh: Dumai - Pekanbaru") },
                            leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = AmberSecondary) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("field_report_location")
                        )

                        // Import Ongoing Works button
                        if (ongoingItems.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { showImportOngoingSheet = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("btn_import_ongoing"),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AmberSecondary
                                )
                            ) {
                                Icon(Icons.Default.PendingActions, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lanjutkan Pekerjaan Berjalan (${ongoingItems.size})", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Section Header: Kegiatan
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DAFTAR KEGIATAN (${items.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { viewModel.addWorkItem() },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.testTag("btn_add_activity_top")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Tambah", fontSize = 12.sp)
                    }
                }
            }

            // Activities Cards
            itemsIndexed(items) { index, itemDraft ->
                WorkItemCardForm(
                    item = itemDraft,
                    index = index,
                    onUpdate = { updated ->
                        viewModel.updateWorkItem(itemDraft.tempId) { updated }
                    },
                    onDelete = {
                        itemToDeleteIndex = index
                    },
                    onTakePhoto = { type ->
                        pendingPhotoItemIndex = index
                        pendingPhotoType = type
                        val tempFile = FileUtils.createTempImageFile(context)
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            tempFile
                        )
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                    onPickGallery = { type ->
                        pendingPhotoItemIndex = index
                        pendingPhotoType = type
                        galleryLauncher.launch("image/*")
                    },
                    onDeletePhoto = { type ->
                        viewModel.updateWorkItem(itemDraft.tempId) {
                            when (type) {
                                "BEFORE" -> it.copy(beforePhotoPath = null)
                                "PROCESS" -> it.copy(processPhotoPath = null)
                                "AFTER" -> it.copy(afterPhotoPath = null)
                                else -> it
                            }
                        }
                    }
                )
            }

            // Add Activity Button Big
            item {
                OutlinedButton(
                    onClick = { viewModel.addWorkItem() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_add_activity_bottom")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+ TAMBAH KEGIATAN", fontWeight = FontWeight.Bold)
                }
            }

            // Rencana Kerja Selanjutnya (Footer Table Section)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "RENCANA KERJA HARI INI / BERIKUTNYA",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = workPlanNext,
                            onValueChange = { viewModel.workPlanNext.value = it },
                            placeholder = { Text("Contoh: Perbaikan chute untuk aliran beton dari mixer BP ke truck mixer...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 90.dp)
                                .testTag("field_work_plan_next"),
                            maxLines = 4
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }

    // Draft restore dialog
    if (showDraftDialog) {
        AlertDialog(
            onDismissRequest = { showDraftDialog = false },
            title = { Text("Lanjutkan Draft Sebelumnya?") },
            text = { Text("Ditemukan draf laporan yang belum disimpan. Anda ingin melanjutkan draf ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreDraft()
                        showDraftDialog = false
                    }
                ) {
                    Text("Lanjutkan")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.clearDraftLocally()
                        showDraftDialog = false
                    }
                ) {
                    Text("Hapus Draft")
                }
            }
        )
    }

    // Confirm Delete Activity Dialog
    if (itemToDeleteIndex != null) {
        val idx = itemToDeleteIndex!!
        AlertDialog(
            onDismissRequest = { itemToDeleteIndex = null },
            title = { Text("Hapus Kegiatan Nomor ${idx + 1}?") },
            text = { Text("Kegiatan ini dan dokumentasi fotonya akan dihapus dari laporan ini.") },
            confirmButton = {
                Button(
                    onClick = {
                        val itm = items.getOrNull(idx)
                        if (itm != null) {
                            viewModel.removeWorkItem(itm.tempId)
                        }
                        itemToDeleteIndex = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDeleteIndex = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Bottom Sheet / Dialog for Importing Ongoing Work
    if (showImportOngoingSheet) {
        AlertDialog(
            onDismissRequest = { showImportOngoingSheet = false },
            title = { Text("Pilih Pekerjaan Berjalan") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                ) {
                    Text(
                        text = "Pilih pekerjaan yang dilanjutkan pada hari ini:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ongoingItems.size) { i ->
                            val ongoing = ongoingItems[i]
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ongoing_item_choice_$i"),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = ongoing.workItem.description,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 2
                                        )
                                        Text(
                                            text = "Mulai: ${ongoing.workItem.targetStartDate} | Progress: ${ongoing.workItem.currentProgress}%",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = {
                                            viewModel.importOngoingWork(ongoing)
                                            showImportOngoingSheet = false
                                            Toast.makeText(context, "Pekerjaan ditambahkan ke laporan hari ini", Toast.LENGTH_SHORT).show()
                                        },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("Lanjutkan", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showImportOngoingSheet = false }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Success PDF Generated Dialog
    if (generatedPdfResult != null) {
        val file = generatedPdfResult!!
        AlertDialog(
            onDismissRequest = { generatedPdfResult = null },
            icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = NavyPrimary) },
            title = { Text("PDF Berhasil Dibuat!") },
            text = {
                Column {
                    Text("File: ${file.name}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Ukuran: ${file.length() / 1024} KB", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PDF dibuat sesuai kondisi data saat ini.", fontSize = 12.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
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
                        generatedPdfResult = null
                    }
                ) {
                    Text("Buka PDF")
                }
            },
            dismissButton = {
                Row {
                    OutlinedButton(
                        onClick = {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                putExtra(Intent.EXTRA_SUBJECT, "Laporan Kegiatan Harian")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan PDF Laporan"))
                            generatedPdfResult = null
                        }
                    ) {
                        Text("Bagikan")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    TextButton(onClick = { generatedPdfResult = null }) {
                        Text("Tutup")
                    }
                }
            }
        )
    }
}

@Composable
fun WorkItemCardForm(
    item: WorkItemDraft,
    index: Int,
    onUpdate: (WorkItemDraft) -> Unit,
    onDelete: () -> Unit,
    onTakePhoto: (String) -> Unit,
    onPickGallery: (String) -> Unit,
    onDeletePhoto: (String) -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("work_item_card_${index + 1}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with automatic sequence number
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = NavyPrimary,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "NO. ${index + 1}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (item.currentProgress >= 100) "SELESAI (100%)" else "PROGRESS ${item.currentProgress}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.currentProgress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Hapus Kegiatan", tint = MaterialTheme.colorScheme.error)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Deskripsi Kegiatan
            OutlinedTextField(
                value = item.description,
                onValueChange = { onUpdate(item.copy(description = it)) },
                label = { Text("Deskripsi Kegiatan (wajib)") },
                placeholder = { Text("Contoh: Perbaikan Screw semen silo 2 dan 4") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp)
                    .testTag("field_desc_${index + 1}"),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Dates: Target Mulai, Target Selesai, Realisasi
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = item.targetStartDate,
                    onValueChange = { onUpdate(item.copy(targetStartDate = it)) },
                    label = { Text("Target Mulai") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("field_target_start_${index + 1}"),
                    singleLine = true
                )

                OutlinedTextField(
                    value = item.targetEndDate,
                    onValueChange = { onUpdate(item.copy(targetEndDate = it)) },
                    label = { Text("Target Selesai") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("field_target_end_${index + 1}"),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = item.realizationDate,
                onValueChange = { onUpdate(item.copy(realizationDate = it)) },
                label = { Text("Realisasi Tanggal") },
                placeholder = { Text("03/06/2026") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_realization_${index + 1}"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Jumlah/Nama Pekerja
            OutlinedTextField(
                value = item.workers,
                onValueChange = { onUpdate(item.copy(workers = it)) },
                label = { Text("Jumlah / Nama Pekerja") },
                placeholder = { Text("Contoh: 3 orang: Hendra, Johanes, Rahmat") },
                leadingIcon = { Icon(Icons.Default.People, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_workers_${index + 1}"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Alat Kerja
            OutlinedTextField(
                value = item.tools,
                onValueChange = { onUpdate(item.copy(tools = it)) },
                label = { Text("Alat Kerja") },
                placeholder = { Text("Contoh: Chain block, kunci inggris, palu") },
                leadingIcon = { Icon(Icons.Default.Build, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_tools_${index + 1}"),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Progress Slider
            ProgressSlider(
                progress = item.currentProgress,
                onProgressChange = { p ->
                    onUpdate(item.copy(currentProgress = p))
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Keterangan
            OutlinedTextField(
                value = item.notes,
                onValueChange = { onUpdate(item.copy(notes = it)) },
                label = { Text("Keterangan") },
                placeholder = { Text("Catatan teknis / kendala lapangan...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("field_notes_${index + 1}"),
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3 Wajib Dokumentasi Foto: SEBELUM, PROSES, SESUDAH
            Text(
                text = "DOKUMENTASI FOTO (3 KATEGORI)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 1. SEBELUM
            PhotoPickerSlot(
                title = "SEBELUM",
                photoPath = item.beforePhotoPath,
                isRequired = false,
                onTakePhoto = { onTakePhoto("BEFORE") },
                onPickGallery = { onPickGallery("BEFORE") },
                onDeletePhoto = { onDeletePhoto("BEFORE") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. PROSES
            PhotoPickerSlot(
                title = "PROSES",
                photoPath = item.processPhotoPath,
                isRequired = false,
                onTakePhoto = { onTakePhoto("PROCESS") },
                onPickGallery = { onPickGallery("PROCESS") },
                onDeletePhoto = { onDeletePhoto("PROCESS") }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. SESUDAH
            PhotoPickerSlot(
                title = "SESUDAH",
                photoPath = item.afterPhotoPath,
                isRequired = item.currentProgress >= 100,
                onTakePhoto = { onTakePhoto("AFTER") },
                onPickGallery = { onPickGallery("AFTER") },
                onDeletePhoto = { onDeletePhoto("AFTER") }
            )
        }
    }
}

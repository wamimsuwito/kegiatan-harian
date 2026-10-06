package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.data.entity.WorkPhoto
import com.example.ui.components.ProgressSlider
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkItemViewModel
import com.example.util.DateUtils
import com.example.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkItemDetailScreen(
    workItemId: Long,
    viewModel: WorkItemViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val itemWithPhotos by viewModel.getWorkItemDetails(workItemId).collectAsState(initial = null)

    var showUpdateDialog by remember { mutableStateOf(false) }

    if (itemWithPhotos == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Detail Pekerjaan", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    val item = itemWithPhotos!!.workItem
    val photos = itemWithPhotos!!.photos
    val progressHistory by viewModel.getProgressHistory(item.permanentWorkId).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Pekerjaan", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
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
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showUpdateDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_open_update_dialog"),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("UPDATE PROGRESS & FOTO", fontWeight = FontWeight.Bold)
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // General info card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(status = item.status)
                            Text(
                                text = item.permanentWorkId,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Mulai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.targetStartDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Column {
                                Text("Target Selesai", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.targetEndDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AmberSecondary)
                            }
                            Column {
                                Text("Realisasi", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(item.realizationDate.ifBlank { "-" }, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Current progress
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Progress Saat Ini:", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.currentProgress}%",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.currentProgress >= 100) StatusSuccess else NavyPrimary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { item.currentProgress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = if (item.currentProgress >= 100) StatusSuccess else NavyPrimary
                        )

                        if (item.workers.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Pekerja:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.workers, fontSize = 13.sp)
                        }

                        if (item.tools.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Alat Kerja:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.tools, fontSize = 13.sp)
                        }

                        if (item.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Keterangan:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(item.notes, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Photos Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DOKUMENTASI FOTO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PhotoPreviewBox("SEBELUM", itemWithPhotos!!.beforePhoto?.filePath, Modifier.weight(1f))
                            PhotoPreviewBox("PROSES", itemWithPhotos!!.processPhoto?.filePath, Modifier.weight(1f))
                            PhotoPreviewBox("SESUDAH", itemWithPhotos!!.afterPhoto?.filePath, Modifier.weight(1f))
                        }
                    }
                }
            }

            // Progress Timeline Section (Multi-hari history)
            item {
                Text(
                    text = "RIWAYAT PROGRESS PER TANGGAL (${progressHistory.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(progressHistory) { hist ->
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
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (hist.progress >= 100) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "${hist.progress}%",
                                fontWeight = FontWeight.Bold,
                                color = if (hist.progress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = DateUtils.formatToIndonesianDate(hist.date),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            if (hist.notes.isNotBlank()) {
                                Text(
                                    text = hist.notes,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (hist.workers.isNotBlank()) {
                                Text(
                                    text = "Pekerja: ${hist.workers}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for Updating Progress & Notes
    if (showUpdateDialog) {
        var newProgress by remember { mutableIntStateOf(item.currentProgress) }
        var newNotes by remember { mutableStateOf(item.notes) }

        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Progress Pekerjaan") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProgressSlider(
                        progress = newProgress,
                        onProgressChange = { newProgress = it }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = newNotes,
                        onValueChange = { newNotes = it },
                        label = { Text("Catatan Perkembangan Hari Ini") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateProgress(
                            workItemId = item.id,
                            newProgress = newProgress,
                            notes = newNotes,
                            newPhotos = emptyList()
                        ) {
                            showUpdateDialog = false
                            Toast.makeText(context, "Progress berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PhotoPreviewBox(label: String, filePath: String?, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(95.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (filePath != null && File(filePath).exists()) {
                AsyncImage(
                    model = File(filePath),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text("Tidak Ada", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }
}

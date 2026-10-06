package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProfileViewModel
import com.example.ui.viewmodel.ReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    profileViewModel: ProfileViewModel,
    reportViewModel: ReportViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isBackingUp by profileViewModel.isBackingUp.collectAsState()
    val backupStatus by profileViewModel.backupStatus.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            profileViewModel.restoreData(uri) { success ->
                if (success) {
                    Toast.makeText(context, "Data berhasil direstore!", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Gagal merestore data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan & Backup", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button_settings")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(SlateSurface),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Backup & Restore Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "CADANGAN & PEMULIHAN (BACKUP & RESTORE)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ekspor seluruh laporan, riwayat progress, foto kegiatan, profil, dan tanda tangan ke dalam berkas ZIP lokal tanpa memerlukan koneksi internet.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    profileViewModel.backupData { backupFile ->
                                        if (backupFile != null) {
                                            val uri = FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                backupFile
                                            )
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/zip"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                putExtra(Intent.EXTRA_SUBJECT, backupFile.name)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan / Simpan File Backup ZIP"))
                                        }
                                    }
                                },
                                enabled = !isBackingUp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_backup_data"),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isBackingUp) "Proses..." else "Backup Data")
                            }

                            OutlinedButton(
                                onClick = { filePickerLauncher.launch("application/zip") },
                                enabled = !isBackingUp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_restore_data")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Data")
                            }
                        }

                        if (backupStatus != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = backupStatus!!,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Demo Data Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "DATA TESTING & DEMO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Memuat data contoh laporan (03 Juni 2026 Dumai - Pekanbaru) serta contoh pekerjaan multi-hari (08 Juli 2026) untuk mencoba fitur aplikasi.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                reportViewModel.seedDemoData {
                                    Toast.makeText(context, "Data contoh berhasil dimuat!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_seed_demo_data")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Muat Data Contoh (Demo)")
                        }
                    }
                }
            }

            // App info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "INFORMASI SISTEM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Aplikasi: Laporan Kegiatan Harian", fontSize = 13.sp)
                        Text(text = "Versi: 1.0 (Native Android Offline-First)", fontSize = 13.sp)
                        Text(text = "Penyimpanan: Room SQLite Local Database", fontSize = 13.sp)
                        Text(text = "PDF Generator: Android Native PdfDocument (A4 Landscape)", fontSize = 13.sp)
                        Text(text = "Privasi: 100% Data disimpan di dalam perangkat Anda", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

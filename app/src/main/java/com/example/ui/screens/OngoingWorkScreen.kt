package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WorkItemWithPhotos
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*
import com.example.ui.viewmodel.WorkItemViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OngoingWorkScreen(
    viewModel: WorkItemViewModel,
    onNavigateBack: () -> Unit,
    onContinueWork: (WorkItemWithPhotos) -> Unit,
    onViewDetail: (Long) -> Unit
) {
    val ongoingItems by viewModel.ongoingWorkItems.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Pekerjaan Berjalan (${ongoingItems.size})",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("back_button_ongoing")
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
        if (ongoingItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SlateSurface),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = StatusSuccess,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Semua Pekerjaan Selesai",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tidak ada pekerjaan berjalan yang belum terselesaikan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(SlateSurface),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ongoingItems) { itemWithPhotos ->
                    OngoingWorkCard(
                        itemWithPhotos = itemWithPhotos,
                        onContinue = { onContinueWork(itemWithPhotos) },
                        onDetail = { onViewDetail(itemWithPhotos.workItem.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun OngoingWorkCard(
    itemWithPhotos: WorkItemWithPhotos,
    onContinue: () -> Unit,
    onDetail: () -> Unit
) {
    val item = itemWithPhotos.workItem

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDetail)
            .testTag("ongoing_card_${item.id}"),
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
                StatusBadge(status = "BERJALAN")
                Text(
                    text = "ID: ${item.permanentWorkId}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.description,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text(
                        text = "Target Mulai",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.targetStartDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column {
                    Text(
                        text = "Target Selesai",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = item.targetEndDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AmberSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progress Terakhir:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${item.currentProgress}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { item.currentProgress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = if (item.currentProgress >= 100) StatusSuccess else NavyPrimary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            )

            if (item.workers.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Pekerja: ${item.workers}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDetail,
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Riwayat & Detail", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .height(36.dp)
                        .testTag("btn_continue_work_${item.id}"),
                    colors = ButtonDefaults.buttonColors(containerColor = AmberSecondary)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("LANJUTKAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun SignaturePad(
    existingSignaturePath: String?,
    onSaveSignature: (Bitmap) -> Unit,
    onClearSignature: () -> Unit,
    modifier: Modifier = Modifier
) {
    val paths = remember { mutableStateListOf<Path>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var canvasWidth by remember { mutableIntStateOf(0) }
    var canvasHeight by remember { mutableIntStateOf(0) }
    var hasNewDrawing by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Area Tanda Tangan (Gunakan Jari):",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .background(Color.White)
        ) {
            // If existing signature exists and no new drawing started
            if (existingSignaturePath != null && File(existingSignaturePath).exists() && paths.isEmpty() && currentPath == null) {
                AsyncImage(
                    model = File(existingSignaturePath),
                    contentDescription = "Tanda Tangan Tersimpan",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                )
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("signature_canvas")
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                val p = Path().apply { moveTo(offset.x, offset.y) }
                                currentPath = p
                                hasNewDrawing = true
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath?.lineTo(change.position.x, change.position.y)
                            },
                            onDragEnd = {
                                currentPath?.let { paths.add(it) }
                                currentPath = null
                            }
                        )
                    }
            ) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()

                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
                currentPath?.let { path ->
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 5f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            if (paths.isEmpty() && currentPath == null && (existingSignaturePath == null || !File(existingSignaturePath).exists())) {
                Text(
                    text = "Goreskan tanda tangan di sini...",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    paths.clear()
                    currentPath = null
                    hasNewDrawing = false
                    onClearSignature()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("clear_signature_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Hapus")
            }

            Button(
                onClick = {
                    if (paths.isNotEmpty() && canvasWidth > 0 && canvasHeight > 0) {
                        // Render to transparent Bitmap
                        val bitmap = Bitmap.createBitmap(canvasWidth, canvasHeight, Bitmap.Config.ARGB_8888)
                        val androidCanvas = android.graphics.Canvas(bitmap)
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.BLACK
                            strokeWidth = 5f
                            style = android.graphics.Paint.Style.STROKE
                            strokeCap = android.graphics.Paint.Cap.ROUND
                            strokeJoin = android.graphics.Paint.Join.ROUND
                            isAntiAlias = true
                        }
                        paths.forEach { composePath ->
                            androidCanvas.drawPath(composePath.asAndroidPath(), paint)
                        }
                        onSaveSignature(bitmap)
                        paths.clear()
                    }
                },
                enabled = paths.isNotEmpty(),
                modifier = Modifier
                    .weight(1f)
                    .testTag("save_signature_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Simpan TTD")
            }
        }
    }
}

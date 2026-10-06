package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusSuccess

@Composable
fun ProgressSlider(
    progress: Int,
    onProgressChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Progress Pekerjaan:",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (progress >= 100) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "$progress%",
                    color = if (progress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        Slider(
            value = progress.toFloat(),
            onValueChange = { onProgressChange(it.toInt()) },
            valueRange = 0f..100f,
            steps = 19, // increments of 5%
            modifier = Modifier
                .fillMaxWidth()
                .testTag("progress_slider"),
            colors = SliderDefaults.colors(
                thumbColor = if (progress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary,
                activeTrackColor = if (progress >= 100) StatusSuccess else MaterialTheme.colorScheme.primary
            )
        )

        // Quick Preset Chips (0%, 25%, 50%, 75%, 100%)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(0, 25, 50, 75, 100).forEach { preset ->
                val isSelected = progress == preset
                OutlinedButton(
                    onClick = { onProgressChange(preset) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("preset_$preset"),
                    contentPadding = PaddingValues(0.dp),
                    colors = if (isSelected) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    }
                ) {
                    Text(
                        text = "$preset%",
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StatusAlert
import com.example.ui.theme.StatusOngoing
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusSuccess

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, label) = when (status.uppercase()) {
        "SELESAI", "COMPLETED" -> Triple(Color(0xFFDCFCE7), StatusSuccess, "SELESAI")
        "BERJALAN", "IN_PROGRESS" -> Triple(Color(0xFFDBEAFE), StatusOngoing, "BERJALAN")
        "BELUM_MULAI", "PENDING" -> Triple(Color(0xFFFEF3C7), StatusPending, "BELUM MULAI")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), status)
    }

    Text(
        text = label,
        color = textColor,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

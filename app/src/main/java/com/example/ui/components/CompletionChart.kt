package com.example.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.PrimaryRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

data class BarChartItem(
    val label: String,
    val value: Int
)

@Composable
fun CompletionBarChart(
    title: String,
    subtitle: String,
    items: List<BarChartItem>,
    modifier: Modifier = Modifier,
    accentGradient: List<Color> = listOf(PrimaryRed, Color(0xFFFF5252))
) {
    val maxValue = remember(items) { (items.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1) }
    val totalInChart = remember(items) { items.sumOf { it.value } }

    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, DarkCardBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = subtitle,
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                Text(
                    text = "$totalInChart Completed",
                    color = PrimaryRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            val density = LocalDensity.current
            val textPaint = remember(density) {
                Paint().apply {
                    color = android.graphics.Color.argb(180, 148, 163, 184)
                    textSize = with(density) { 10.sp.toPx() }
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }
            }
            val valuePaint = remember(density) {
                Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = with(density) { 11.sp.toPx() }
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }
            }

            // Canvas Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
            ) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height
                    val bottomPadding = 24.dp.toPx()
                    val chartHeight = h - bottomPadding
                    val count = items.size
                    if (count == 0) return@Canvas

                    val barSpacing = w / count
                    val barWidth = (barSpacing * 0.55f).coerceAtMost(36.dp.toPx())

                    // Draw subtle grid lines
                    for (i in 0..3) {
                        val y = chartHeight * (i / 3f)
                        drawLine(
                            color = Color.White.copy(alpha = 0.05f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f
                        )
                    }

                    items.forEachIndexed { index, item ->
                        val centerX = (index + 0.5f) * barSpacing
                        val barLeft = centerX - (barWidth / 2f)
                        val barFraction = (item.value.toFloat() / maxValue).coerceIn(0f, 1f)
                        val barTop = chartHeight - (chartHeight * 0.85f * barFraction) - 4.dp.toPx()
                        val barHeight = (chartHeight - barTop).coerceAtLeast(4.dp.toPx())

                        // Bar background track
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.03f),
                            topLeft = Offset(barLeft, 0f),
                            size = Size(barWidth, chartHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )

                        if (item.value > 0) {
                            // Active bar with gradient
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = accentGradient,
                                    startY = barTop,
                                    endY = chartHeight
                                ),
                                topLeft = Offset(barLeft, barTop),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )

                            // Top value label
                            drawContext.canvas.nativeCanvas.drawText(
                                item.value.toString(),
                                centerX,
                                barTop - 4.dp.toPx(),
                                valuePaint
                            )
                        }

                        // Bottom X-axis label
                        drawContext.canvas.nativeCanvas.drawText(
                            item.label,
                            centerX,
                            h - 4.dp.toPx(),
                            textPaint
                        )
                    }
                }
            }
        }
    }
}

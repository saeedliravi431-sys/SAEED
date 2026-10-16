package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.min

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Double>,
    val strokeWidth: Float = 3f,
    val isDashed: Boolean = false
)

@Composable
fun TimeSeriesChart(
    title: String,
    seriesList: List<ChartSeries>,
    yAxisUnit: String,
    modifier: Modifier = Modifier,
    horizontalZeroLine: Boolean = false,
    horizontalThresholdLine: Double? = null,
    thresholdLabel: String = "Limit",
    thresholdColor: Color = Color(0xFFEF4444),
    cursorIndex: Int? = null,
    onScrub: ((Int) -> Unit)? = null
) {
    val totalPoints = seriesList.firstOrNull()?.values?.size ?: 0

    // Compute Y min & max
    val allValues = seriesList.flatMap { it.values }
    val rawMinY = allValues.minOrNull() ?: 0.0
    val rawMaxY = allValues.maxOrNull() ?: 1.0

    val effectiveMinY = if (horizontalZeroLine) min(0.0, rawMinY) else rawMinY
    val effectiveMaxY = if (horizontalThresholdLine != null) max(horizontalThresholdLine, rawMaxY) else rawMaxY
    val rangeY = max(0.001, effectiveMaxY - effectiveMinY)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "[ $yAxisUnit ]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Legends
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                seriesList.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(s.color)
                        )
                        Text(
                            text = s.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Canvas Plot
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(totalPoints) {
                        if (onScrub != null && totalPoints > 0) {
                            detectDragGestures { change, _ ->
                                val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                                onScrub((fraction * (totalPoints - 1)).toInt())
                            }
                        }
                    }
                    .pointerInput(totalPoints) {
                        if (onScrub != null && totalPoints > 0) {
                            detectTapGestures { offset ->
                                val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                                onScrub((fraction * (totalPoints - 1)).toInt())
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    val paddingBottom = 4f
                    val paddingTop = 4f
                    val chartHeight = height - paddingTop - paddingBottom

                    // Draw subtle grid lines
                    val gridColor = Color(0xFF334155).copy(alpha = 0.35f)
                    for (i in 0..4) {
                        val y = paddingTop + chartHeight * (i / 4f)
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1f
                        )
                    }

                    // Zero Baseline
                    if (horizontalZeroLine && effectiveMinY < 0.0 && effectiveMaxY > 0.0) {
                        val zeroY = paddingTop + chartHeight * (1f - ((0.0 - effectiveMinY) / rangeY).toFloat())
                        drawLine(
                            color = Color.White.copy(alpha = 0.45f),
                            start = Offset(0f, zeroY),
                            end = Offset(width, zeroY),
                            strokeWidth = 1.5f
                        )
                    }

                    // Threshold Line (e.g. Critical Magnet Limit)
                    if (horizontalThresholdLine != null) {
                        val threshY = paddingTop + chartHeight * (1f - ((horizontalThresholdLine - effectiveMinY) / rangeY).toFloat())
                        drawLine(
                            color = thresholdColor,
                            start = Offset(0f, threshY),
                            end = Offset(width, threshY),
                            strokeWidth = 2f
                        )
                    }

                    // Plot each series
                    if (totalPoints > 1) {
                        seriesList.forEach { s ->
                            val path = Path()
                            val stepX = width / (totalPoints - 1).toFloat()

                            s.values.forEachIndexed { idx, v ->
                                val normY = ((v - effectiveMinY) / rangeY).toFloat().coerceIn(0f, 1f)
                                val x = idx * stepX
                                val y = paddingTop + chartHeight * (1f - normY)
                                if (idx == 0) {
                                    path.moveTo(x, y)
                                } else {
                                    path.lineTo(x, y)
                                }
                            }

                            drawPath(
                                path = path,
                                color = s.color,
                                style = Stroke(width = s.strokeWidth, cap = StrokeCap.Round)
                            )
                        }
                    }

                    // Cursor Scrubber
                    if (cursorIndex != null && totalPoints > 0) {
                        val curX = (cursorIndex.toFloat() / (totalPoints - 1).coerceAtLeast(1)) * width
                        drawLine(
                            color = Color(0xFF00D2FF),
                            start = Offset(curX, 0f),
                            end = Offset(curX, height),
                            strokeWidth = 2f
                        )
                        drawCircle(
                            color = Color(0xFF00D2FF),
                            radius = 5f,
                            center = Offset(curX, height / 2f)
                        )
                    }
                }
            }

            // Min and Max scale markers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min: ${String.format("%.1f", effectiveMinY)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = "Max: ${String.format("%.1f", effectiveMaxY)}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.EfficiencyMapData
import kotlin.math.max

@Composable
fun EfficiencyMapCanvas(
    mapData: EfficiencyMapData,
    contTorqueNm: Double,
    peakTorqueNm: Double,
    maxRpm: Double,
    modifier: Modifier = Modifier,
    isFarsi: Boolean = true
) {
    var inspectedPoint by remember { mutableStateOf<Triple<Double, Double, Double>?>(null) }

    val numTorque = mapData.torqueValues.size
    val numRpm = mapData.rpmValues.size

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (isFarsi) "نقشه کانتور راندمان موتور (BLDC/PMSM)" else "Motor Electrical Efficiency Map",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isFarsi) "انطباق نقاط کاری سیکل و محدوده گشتاور" else "Operating Points & Torque Limits Distribution",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Colorbar preview
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("50%", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp)
                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(10.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    listOf(Color(0xFF003F5C), Color(0xFF2F4B7C), Color(0xFF665191), Color(0xFFA05195), Color(0xFFD45087), Color(0xFFF95D6A), Color(0xFFFF7C43), Color(0xFFFFA600))
                                )
                            )
                    )
                    Text("95%", style = MaterialTheme.typography.bodySmall, fontSize = 9.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Heatmap
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .pointerInput(mapData) {
                        detectTapGestures { offset ->
                            val normX = (offset.x / size.width).coerceIn(0f, 1f)
                            val normY = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            val rpm = normX * maxRpm
                            val torque = normY * (peakTorqueNm * 1.15)

                            val tIdx = ((normY * (numTorque - 1)).toInt()).coerceIn(0, numTorque - 1)
                            val rIdx = ((normX * (numRpm - 1)).toInt()).coerceIn(0, numRpm - 1)
                            val eff = mapData.efficiencyMatrix.getOrNull(tIdx)?.getOrNull(rIdx) ?: 0.0

                            inspectedPoint = Triple(rpm, torque, eff)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height

                    val maxPlotTorque = peakTorqueNm * 1.15
                    val cellWidth = width / max(1, numRpm)
                    val cellHeight = height / max(1, numTorque)

                    // Draw 2D cells
                    for (tIdx in 0 until numTorque) {
                        val tVal = mapData.torqueValues[tIdx]
                        val normT = (tVal / maxPlotTorque).toFloat()
                        val y = height * (1f - normT)

                        for (rIdx in 0 until numRpm) {
                            val eff = mapData.efficiencyMatrix.getOrNull(tIdx)?.getOrNull(rIdx) ?: 0.0
                            val normR = (mapData.rpmValues[rIdx] / maxRpm).toFloat()
                            val x = normR * width

                            val cellColor = getEfficiencyColor(eff)
                            drawRect(
                                color = cellColor,
                                topLeft = Offset(x, y - cellHeight),
                                size = Size(cellWidth * 1.05f, cellHeight * 1.05f)
                            )
                        }
                    }

                    // Continuous Torque limit line (Green solid)
                    val contY = height * (1f - (contTorqueNm / maxPlotTorque).toFloat())
                    drawLine(
                        color = Color(0xFF10B981),
                        start = Offset(0f, contY),
                        end = Offset(width, contY),
                        strokeWidth = 3f
                    )

                    // Peak Torque limit line (Red dashed)
                    val peakY = height * (1f - (peakTorqueNm / maxPlotTorque).toFloat())
                    drawLine(
                        color = Color(0xFFEF4444),
                        start = Offset(0f, peakY),
                        end = Offset(width, peakY),
                        strokeWidth = 3f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                    )

                    // Operating Points Scatter
                    mapData.operatingScatter.forEach { (rpm, torque) ->
                        val px = (rpm / maxRpm).toFloat().coerceIn(0f, 1f) * width
                        val py = height * (1f - (torque / maxPlotTorque).toFloat().coerceIn(0f, 1f))
                        drawCircle(
                            color = Color.White.copy(alpha = 0.55f),
                            radius = 3.5f,
                            center = Offset(px, py)
                        )
                    }

                    // Inspected target marker
                    inspectedPoint?.let { (rpm, torque, _) ->
                        val ix = (rpm / maxRpm).toFloat().coerceIn(0f, 1f) * width
                        val iy = height * (1f - (torque / maxPlotTorque).toFloat().coerceIn(0f, 1f))
                        drawCircle(
                            color = Color(0xFF00D2FF),
                            radius = 8f,
                            center = Offset(ix, iy)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3f,
                            center = Offset(ix, iy)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Limits & Axis Legends
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFF10B981)))
                        Text("Cont. ${contTorqueNm.toInt()} Nm", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(8.dp).background(Color(0xFFEF4444)))
                        Text("Peak ${peakTorqueNm.toInt()} Nm", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(modifier = Modifier.size(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White))
                        Text(if (isFarsi) "نقاط کاری" else "Scatter", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                    }
                }

                Text(
                    text = "Max ${maxRpm.toInt()} RPM",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Inspection bar
            if (inspectedPoint != null) {
                val (r, t, e) = inspectedPoint!!
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RPM: ${r.toInt()}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Torque: ${String.format("%.1f", t)} Nm",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Efficiency: ${String.format("%.1f", e)}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}

private fun getEfficiencyColor(eff: Double): Color {
    val norm = ((eff - 45.0) / 45.0).coerceIn(0.0, 1.0)
    return when {
        norm < 0.2 -> Color(0xFF0F172A)
        norm < 0.4 -> Color(0xFF1E3A8A)
        norm < 0.6 -> Color(0xFF0284C7)
        norm < 0.75 -> Color(0xFF10B981)
        norm < 0.88 -> Color(0xFFF59E0B)
        else -> Color(0xFFEF4444)
    }
}

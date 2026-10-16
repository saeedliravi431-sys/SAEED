package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.ChartSeries
import com.example.ui.components.EfficiencyMapCanvas
import com.example.ui.components.TimeSeriesChart
import com.example.ui.theme.*
import com.example.viewmodel.EVLabUiState

@Composable
fun MotorOptimizationScreen(
    uiState: EVLabUiState,
    onRunOptimization: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.isFarsiPreferred
    val effMap = uiState.efficiencyMap
    val opt = uiState.optimizationResult
    val cfg = uiState.config

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("motor_optimization_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Motor 2D Efficiency Map Section (Section 4 & Figure 2)
        item {
            Column {
                Text(
                    text = if (isFa) "نقشه تلفات و راندمان موتور الکتریکی (Section 4)" else "Motor Loss & Efficiency Mapping",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFa) "موتور هیبرید 12S14P هالباخ با در نظر گرفتن تلفات مس، آهن و اصطکاک" else "2D efficiency mesh with Halbach Array PMSM loss model",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (effMap != null) {
            item {
                EfficiencyMapCanvas(
                    mapData = effMap,
                    contTorqueNm = cfg.motor.contTorqueNm,
                    peakTorqueNm = cfg.motor.peakTorqueNm,
                    maxRpm = cfg.motor.maxRpm,
                    isFarsi = isFa
                )
            }
        }

        // 2. Parametric Gear Ratio Optimization Section (Section 6 & Figure 4)
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFa) "بهینه‌سازی نسبت دنده و شتاب (Section 6)" else "Gear Ratio Multi-Objective Optimization",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = if (isFa)
                                "بررسی مصالحه شتاب ۰ تا ۶۰، مصرف انرژی و دمای مگنت"
                            else
                                "Trade-off analysis: 0-60 km/h acceleration vs Wh/km vs magnet temp",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = onRunOptimization,
                        enabled = !uiState.isOptimizing,
                        colors = ButtonDefaults.buttonColors(containerColor = CopperOrange),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("btn_run_optimization")
                    ) {
                        if (uiState.isOptimizing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isFa) "پویش بهینه" else "Run Sweep", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Recommended Ratio Highlight
        if (opt != null) {
            val best = opt.points.getOrNull(opt.optimalIndex)
            if (best != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                androidx.compose.ui.graphics.Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surfaceVariant)
                                )
                            )
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Column {
                                Text(
                                    text = if (isFa) "نسبت دنده بهینه پارتو (Pareto-Optimal Ratio)" else "Recommended Pareto-Optimal Gear Ratio",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Nc = ${String.format("%.2f", best.gearRatioNc)}  |  0-60: ${String.format("%.2f", best.accelTime0to60S)} s  |  ${String.format("%.1f", best.specEnergyWhKm)} Wh/km  |  T_mag: ${String.format("%.1f", best.estPeakMagnetTempC)}°C",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Figure 4.1: 0-60 km/h Acceleration Time vs Gear Ratio
            item {
                TimeSeriesChart(
                    title = if (isFa) "زمان شتاب‌گیری ۰ تا ۶۰ کیلومتر بر ساعت" else "0-60 km/h WOT Acceleration Time vs Gear Ratio",
                    seriesList = listOf(
                        ChartSeries(
                            name = "0-60 Time",
                            color = Color(0xFF00D2FF),
                            values = opt.points.map { it.accelTime0to60S }
                        )
                    ),
                    yAxisUnit = "seconds",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Figure 4.2: Specific Energy & Peak Magnet Temp vs Gear Ratio
            item {
                TimeSeriesChart(
                    title = if (isFa) "مصرف ویژه انرژی و دمای بیشینه مگنت" else "Specific Energy & Peak Magnet Temperature vs Gear Ratio",
                    seriesList = listOf(
                        ChartSeries(
                            name = "Energy (Wh/km)",
                            color = Color(0xFFF77F00),
                            values = opt.points.map { it.specEnergyWhKm }
                        ),
                        ChartSeries(
                            name = "Magnet Temp (°C)",
                            color = Color(0xFF10B981),
                            values = opt.points.map { it.estPeakMagnetTempC }
                        )
                    ),
                    yAxisUnit = "Wh/km / °C",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Sweep Table Preview
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = if (isFa) "جدول نتایج اسکن پارامتریک نسبت دنده" else "Parametric Sweep Output Data",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Nc", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("0-60 (s)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("Wh/km", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            Text("T_mag (°C)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        opt.points.take(8).forEachIndexed { index, p ->
                            val isBest = index == opt.optimalIndex
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = String.format("%.2f", p.gearRatioNc) + (if (isBest) " ★" else ""),
                                    color = if (isBest) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontWeight = if (isBest) FontWeight.Bold else FontWeight.Normal,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(String.format("%.2f", p.accelTime0to60S), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", p.specEnergyWhKm), style = MaterialTheme.typography.bodySmall)
                                Text(String.format("%.1f", p.estPeakMagnetTempC), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

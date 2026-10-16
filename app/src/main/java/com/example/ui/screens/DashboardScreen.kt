package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.model.SimulationResult
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.EVLabUiState

@Composable
fun DashboardScreen(
    uiState: EVLabUiState,
    onSelectPreset: (String) -> Unit,
    onRunSimulation: () -> Unit,
    onScrubIndex: (Int) -> Unit,
    onNavigateToInverter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.isFarsiPreferred
    val sim = uiState.simulationResult
    val curStep = sim?.telemetry?.getOrNull(uiState.selectedTimeIndex)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Presets Header Selector
        item {
            Column {
                Text(
                    text = if (isFa) "انتخاب پلتفرم و وسیله نقلیه (Preset)" else "Select Powertrain Platform",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFa) "تنظیمات پیش‌فرض بخش ۱ متلب به همراه مدل‌های مختلف" else "Quickly switch between configured powertrain architectures",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.presets.forEach { preset ->
                        val isSelected = uiState.selectedPresetId == preset.id
                        val bg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        val border = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        val textCol = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .testTag("preset_pill_${preset.id}")
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .border(1.5.dp, border, RoundedCornerShape(12.dp))
                                .clickable { onSelectPreset(preset.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = if (isFa) preset.nameFa else preset.nameEn,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = textCol
                                )
                                Text(
                                    text = preset.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 10.sp,
                                    color = textCol.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Action Card / Hero Simulation Trigger
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        androidx.compose.ui.graphics.Brush.linearGradient(
                            listOf(ElectricBlue, DeepNavy)
                        )
                    )
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFa) "موتور شبیه‌سازی دیجیتال تویین پیشرانه EV" else "EV Powertrain Digital Twin Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = if (isFa)
                                    "چرخه: ${uiState.config.sim.cycleType.labelFa} | نسبت تبدیل: ${uiState.config.drivetrain.gearRatioNc}"
                                else
                                    "Cycle: ${uiState.config.sim.cycleType.labelEn} | Gear: ${uiState.config.drivetrain.gearRatioNc}",
                                style = MaterialTheme.typography.bodySmall,
                                color = ElectricCyan
                            )
                        }

                        Button(
                            onClick = onRunSimulation,
                            enabled = !uiState.isSimulating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ElectricCyan,
                                contentColor = DeepNavy
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("btn_run_simulation")
                        ) {
                            if (uiState.isSimulating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = DeepNavy
                                )
                            } else {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isFa) "محاسبه مجدد" else "Simulate", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (sim != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusBadge(
                                isSafe = sim.isMagnetSafe,
                                safeText = if (isFa) "وضعیت مغناطیس: ایمن (< ${sim.magnetTempLimitC}°C)" else "Magnet Demagnetization: SAFE",
                                warningText = if (isFa) "هشدار: دمای مگنت بحرانی (> ${sim.magnetTempLimitC}°C)" else "WARNING: Magnet Overheated!"
                            )

                            Text(
                                text = "${String.format("%.1f", sim.totalDistKm)} km | ${sim.telemetry.size} steps",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        }

        // 3. Primary Performance Audit Cards Grid
        if (sim != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFa) "شاخص‌های کلیدی عملکرد و انرژی (Section 7 & 10)" else "Performance & Energy Audit (Section 7)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "Travel Range (DoD)",
                            titleFa = "پیمایش تخمینی (DoD)",
                            value = String.format("%.1f", sim.estAutonomyKm),
                            unit = "km",
                            icon = Icons.Default.DirectionsCar,
                            accentColor = ElectricCyan,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = if (isFa) "تا تخلیه مجاز باتری" else "Based on cycle drain"
                        )
                        MetricCard(
                            titleEn = "Specific Energy",
                            titleFa = "مصرف ویژه انرژی",
                            value = String.format("%.1f", sim.specEnergyWhKm),
                            unit = "Wh/km",
                            icon = Icons.Default.Bolt,
                            accentColor = CopperOrange,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = if (isFa) "خالص با کسر بازیافت" else "Net specific energy"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "Regen Braking Share",
                            titleFa = "سهم بازیافت ترمز",
                            value = String.format("%.1f", sim.regenPct),
                            unit = "%",
                            icon = Icons.Default.EnergySavingsLeaf,
                            accentColor = RegenGreen,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "${String.format("%.1f", sim.regenEnergyWh)} Wh recovered"
                        )
                        MetricCard(
                            titleEn = "Peak Magnet Temp",
                            titleFa = "بیشینه دمای آهنربا",
                            value = String.format("%.1f", sim.peakMagnetTempC),
                            unit = "°C",
                            icon = Icons.Default.Thermostat,
                            accentColor = if (sim.isMagnetSafe) ThermalYellow else ThermalRed,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Limit: ${sim.magnetTempLimitC}°C"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "End-of-Run SOC",
                            titleFa = "درصد شارژ نهایی",
                            value = String.format("%.1f", sim.endSocPct),
                            unit = "%",
                            icon = Icons.Default.BatteryChargingFull,
                            accentColor = ElectricBlue,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Min V: ${String.format("%.1f", sim.minTerminalVoltageV)} V"
                        )
                        MetricCard(
                            titleEn = "Peak Inverter Temp",
                            titleFa = "بیشینه دمای اینورتر",
                            value = String.format("%.1f", sim.peakInverterTempC),
                            unit = "°C",
                            icon = Icons.Default.Memory,
                            accentColor = InverterPurple,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Winding: ${String.format("%.1f", sim.peakWindingTempC)}°C"
                        )
                    }
                }
            }

            // 4. Interactive Scrubber & Dynamic Telemetry Gauges
            if (curStep != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                                Text(
                                    text = if (isFa) "تلِمتری لحظه‌ای دینامیک (Scrubber)" else "Dynamic Instantaneous Telemetry",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "t = ${String.format("%.1f", curStep.timeS)} s",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Slider(
                                value = uiState.selectedTimeIndex.toFloat(),
                                onValueChange = { onScrubIndex(it.toInt()) },
                                valueRange = 0f..(sim.telemetry.size - 1).toFloat(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("telemetry_scrubber")
                            )

                            // Telemetry gauges row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isFa) "سرعت" else "Speed", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    Text("${String.format("%.1f", curStep.speedKmh)} km/h", fontWeight = FontWeight.Bold, color = ElectricCyan)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isFa) "دور موتور" else "RPM", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    Text("${curStep.motorRpm.toInt()} RPM", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isFa) "گشتاور شفت" else "Torque", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    Text("${String.format("%.1f", curStep.shaftTorqueNm)} Nm", fontWeight = FontWeight.Bold, color = CopperOrange)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isFa) "توان باتری" else "Power", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    val pColor = if (curStep.batteryPowerKw < 0) RegenGreen else CopperAmber
                                    Text("${String.format("%.2f", curStep.batteryPowerKw)} kW", fontWeight = FontWeight.Bold, color = pColor)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(if (isFa) "دمای مگنت" else "T_mag", style = MaterialTheme.typography.bodySmall, fontSize = 10.sp)
                                    Text("${String.format("%.1f", curStep.magnetTempC)}°C", fontWeight = FontWeight.Bold, color = ThermalRed)
                                }
                            }

                            if (curStep.derateFlag) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(ThermalRed.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = ThermalRed, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isFa) "کاهش حرارتی گشتاور موتور فعال است (Derating Active)" else "Thermal Derate Flag Active!",
                                        color = ThermalRed,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Engineering Time-Series Charts (Section 8 Figures)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = if (isFa) "نمودارهای دینامیکی سیکل (Figures 1 & 3)" else "Dynamic Cycle Telemetry Plots",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Figure 1.1: Speed vs Time
                    TimeSeriesChart(
                        title = if (isFa) "پروفایل سرعت حرکت وسیله (Speed)" else "Drive Schedule Speed Profile",
                        seriesList = listOf(
                            ChartSeries(
                                name = "Speed",
                                color = Color(0xFF00D2FF),
                                values = sim.telemetry.map { it.speedKmh }
                            )
                        ),
                        yAxisUnit = "km/h",
                        cursorIndex = uiState.selectedTimeIndex,
                        onScrub = onScrubIndex
                    )

                    // Figure 1.2: Battery Power Flow (kW)
                    TimeSeriesChart(
                        title = if (isFa) "جریان توان باتری (+ پیشرانش | - ترمز بازیافتی)" else "Battery Power Flow (Traction vs Smart Regen)",
                        seriesList = listOf(
                            ChartSeries(
                                name = "P_batt",
                                color = Color(0xFFF77F00),
                                values = sim.telemetry.map { it.batteryPowerKw }
                            )
                        ),
                        yAxisUnit = "kW",
                        horizontalZeroLine = true,
                        cursorIndex = uiState.selectedTimeIndex,
                        onScrub = onScrubIndex
                    )

                    // Figure 1.3: Terminal Voltage & SOC
                    TimeSeriesChart(
                        title = if (isFa) "ولتاژ ترمینال باتری و درصد شارژ (1-RC Thevenin)" else "Terminal Voltage V_t & SOC Response",
                        seriesList = listOf(
                            ChartSeries(
                                name = "Voltage (V)",
                                color = Color(0xFF10B981),
                                values = sim.telemetry.map { it.terminalVoltageV }
                            ),
                            ChartSeries(
                                name = "SOC (%)",
                                color = Color(0xFFA855F7),
                                values = sim.telemetry.map { it.batterySocPct }
                            )
                        ),
                        yAxisUnit = "V / %",
                        cursorIndex = uiState.selectedTimeIndex,
                        onScrub = onScrubIndex
                    )

                    // Figure 3: 4-Node LPTN Thermal Response
                    TimeSeriesChart(
                        title = if (isFa) "پاسخ دمایی شبکه حرارتی ۴ گره‌ای (4-Node LPTN)" else "4-Node LPTN Thermal Dynamic Response",
                        seriesList = listOf(
                            ChartSeries(name = "Winding", color = Color(0xFFF77F00), values = sim.telemetry.map { it.windingTempC }),
                            ChartSeries(name = "Core", color = Color(0xFF0077B6), values = sim.telemetry.map { it.coreTempC }),
                            ChartSeries(name = "Magnet", color = Color(0xFFFBBF24), values = sim.telemetry.map { it.magnetTempC }, strokeWidth = 4f),
                            ChartSeries(name = "Inverter", color = Color(0xFFA855F7), values = sim.telemetry.map { it.inverterTempC })
                        ),
                        yAxisUnit = "°C",
                        horizontalThresholdLine = sim.magnetTempLimitC,
                        thresholdLabel = "Magnet Critical Limit",
                        thresholdColor = Color(0xFFEF4444),
                        cursorIndex = uiState.selectedTimeIndex,
                        onScrub = onScrubIndex
                    )
                }
            }
        }
    }
}

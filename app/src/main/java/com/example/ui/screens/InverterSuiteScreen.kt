package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.InverterSizingInput
import com.example.ui.components.MetricCard
import com.example.ui.components.NumericConfigItem
import com.example.ui.theme.*
import com.example.viewmodel.EVLabUiState

@Composable
fun InverterSuiteScreen(
    uiState: EVLabUiState,
    onUpdateInverterInput: ((InverterSizingInput) -> InverterSizingInput) -> Unit,
    onUpdateTargetRange: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.isFarsiPreferred
    val invResult = uiState.inverterResult
    val topSpeed = uiState.topSpeedResult
    val packEst = uiState.batteryPackEstimate
    val invInput = uiState.inverterInput

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("inverter_suite_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section Header
        item {
            Column {
                Text(
                    text = if (isFa) "محاسبات تخصصی اینورتر و توان پیشرانه" else "Inverter & Power Electronics Engineering Suite",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFa) "محاسبه خازن باس، تنش سوییچ‌ها، تلفات، سرعت نهایی و شیب‌روی" else "Bus capacitor, MOSFET stress, inverter losses, top speed and gradeability",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Inverter Sizing Metric Summary Cards
        if (invResult != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isFa) "مشخصات بهینه خازن لینک و سوییچ‌های اینورتر" else "DC Link & Semiconductor Sizing Outputs",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "Required DC Bus Cap",
                            titleFa = "ظرفیت خازن لینک DC",
                            value = String.format("%.0f", invResult.requiredBusCapacitanceMicroFarad),
                            unit = "μF",
                            icon = Icons.Default.Memory,
                            accentColor = ElectricCyan,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "ΔV < ${String.format("%.2f", invResult.maxAllowedDcRippleVoltageV)} V"
                        )
                        MetricCard(
                            titleEn = "Cap RMS Ripple Current",
                            titleFa = "جریان ریپل RMS خازن",
                            value = String.format("%.1f", invResult.capacitorRmsRippleCurrentA),
                            unit = "A",
                            icon = Icons.Default.ElectricMeter,
                            accentColor = CopperOrange,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Capacitor thermal load"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "Rec. MOSFET Vds Rating",
                            titleFa = "ولتاژ شکست مجاز ماسفت",
                            value = "≥ ${invResult.recommendedMosfetVoltageRatingV.toInt()}",
                            unit = "V",
                            icon = Icons.Default.Shield,
                            accentColor = RegenGreen,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "With 1.5x surge margin"
                        )
                        MetricCard(
                            titleEn = "Rec. Drain Current Id",
                            titleFa = "جریان نامی هر شاخه",
                            value = "≥ ${invResult.recommendedMosfetCurrentRatingA.toInt()}",
                            unit = "A",
                            icon = Icons.Default.Bolt,
                            accentColor = ThermalYellow,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "I_peak = ${String.format("%.1f", invResult.ratedPhaseCurrentPeakA)} A"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricCard(
                            titleEn = "Total Inverter Heat",
                            titleFa = "کل تلفات حرارتی اینورتر",
                            value = String.format("%.1f", invResult.totalInverterHeatLossesW),
                            unit = "W",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = ThermalRed,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Cond: ${invResult.conductionLossesW.toInt()}W | Sw: ${invResult.estimatedSwitchingLossesW.toInt()}W"
                        )
                        MetricCard(
                            titleEn = "Max Heatsink R_th",
                            titleFa = "حداکثر مقاومت هیت‌سینک",
                            value = String.format("%.3f", invResult.requiredHeatSinkThermalResistanceKW),
                            unit = "K/W",
                            icon = Icons.Default.Thermostat,
                            accentColor = InverterPurple,
                            modifier = Modifier.weight(1f),
                            isFarsi = isFa,
                            subText = "Target Tj < 105°C"
                        )
                    }
                }
            }
        }

        // 2. Vehicle Top Speed & Climbing Gradeability
        if (topSpeed != null) {
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
                        Text(
                            text = if (isFa) "قابلیت‌های دینامیکی وسیله (سرعت نهایی و شیب‌روی)" else "Dynamic Vehicle Limits & Capabilities",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (isFa) "حداکثر سرعت کفی" else "Max Flat Speed", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.1f", topSpeed.maxSpeedKmh)} km/h", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ElectricCyan)
                            }
                            Column {
                                Text(if (isFa) "شیب‌روی پیک لحظه‌ای" else "Peak Gradeability", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.1f", topSpeed.maxPeakGradePct)} %", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = CopperOrange)
                            }
                            Column {
                                Text(if (isFa) "شیب‌روی دائم‌پیوسته" else "Cont. Gradeability", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.1f", topSpeed.maxContinuousGradePct)} %", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RegenGreen)
                            }
                            Column {
                                Text(if (isFa) "گشتاور استال چرخ" else "Wheel Stall Torque", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${topSpeed.stallWheelTorqueNm.toInt()} Nm", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // 3. Battery Pack Range & Sizing Planner
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
                            text = if (isFa) "محاسبه‌گر ظرفیت پک باتری بر مبنای پیمایش هدف" else "Battery Pack Sizing for Target Range",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${uiState.targetRangeKm.toInt()} km",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = uiState.targetRangeKm.toFloat(),
                        onValueChange = { onUpdateTargetRange(it.toDouble()) },
                        valueRange = 20f..300f,
                        steps = 27,
                        modifier = Modifier.fillMaxWidth().testTag("range_slider")
                    )

                    if (packEst != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(if (isFa) "انرژی ناخالص پک" else "Gross Energy", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.2f", packEst.requiredGrossEnergyKwh)} kWh", fontWeight = FontWeight.Bold, color = ElectricCyan)
                            }
                            Column {
                                Text(if (isFa) "ظرفیت نامی لازم" else "Capacity (Ah)", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.1f", packEst.requiredCapacityAh)} Ah", fontWeight = FontWeight.Bold, color = CopperOrange)
                            }
                            Column {
                                Text(if (isFa) "وزن تخمینی سلول‌ها" else "Est. Weight", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("${String.format("%.1f", packEst.estimatedPackWeightKg)} kg", fontWeight = FontWeight.Bold, color = RegenGreen)
                            }
                            Column {
                                Text(if (isFa) "تعداد سلول 18650" else "18650 Cells", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp)
                                Text("~${packEst.cellCount18650Estimate}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }

        // 4. Inverter Input Controls
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isFa) "تنظیمات ورودی اینورتر توان" else "Inverter Power Stage Parameters",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )

                NumericConfigItem(
                    titleEn = "Switching Frequency",
                    titleFa = "فرکانس سویچینگ اینورتر (f_sw)",
                    value = invInput.switchingFreqKhz,
                    unit = "kHz",
                    step = 1.0,
                    minVal = 5.0,
                    maxVal = 40.0,
                    isFarsi = isFa,
                    description = "معمولاً ۱۰ الی ۲۰ کیلوهرتز در خودرو و موتورسیکلت برقی",
                    onValueChange = { v -> onUpdateInverterInput { inp -> inp.copy(switchingFreqKhz = v) } }
                )

                NumericConfigItem(
                    titleEn = "Allowed DC Voltage Ripple",
                    titleFa = "حداکثر ریپل مجاز ولتاژ باس DC",
                    value = invInput.allowedVoltageRipplePct,
                    unit = "%",
                    step = 0.5,
                    minVal = 0.5,
                    maxVal = 10.0,
                    isFarsi = isFa,
                    description = "معمولاً ۱ الی ۳ درصد ولتاژ نامی برای سلامت خازن و باتری",
                    onValueChange = { v -> onUpdateInverterInput { inp -> inp.copy(allowedVoltageRipplePct = v) } }
                )

                NumericConfigItem(
                    titleEn = "MOSFET On-Resistance (Rds,on)",
                    titleFa = "مقاومت وصل هر ماسفت (Rds,on)",
                    value = invInput.rdsOnMilliOhm,
                    unit = "mΩ",
                    step = 0.2,
                    minVal = 0.5,
                    maxVal = 20.0,
                    isFarsi = isFa,
                    description = "مقاومت استاتیک هدایت ماسفت‌های قدرت برای محاسبه تلفات هدایت",
                    onValueChange = { v -> onUpdateInverterInput { inp -> inp.copy(rdsOnMilliOhm = v) } }
                )

                NumericConfigItem(
                    titleEn = "Parallel MOSFETs per Leg",
                    titleFa = "تعداد ماسفت موازی در هر بازو",
                    value = invInput.numParallelMosfets.toDouble(),
                    unit = "count",
                    step = 1.0,
                    minVal = 1.0,
                    maxVal = 6.0,
                    isFarsi = isFa,
                    description = "تقسیم جریان بین سوییچ‌های موازی",
                    onValueChange = { v -> onUpdateInverterInput { inp -> inp.copy(numParallelMosfets = v.toInt()) } }
                )
            }
        }
    }
}

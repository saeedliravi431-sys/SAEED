package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CycleType
import com.example.model.PowertrainConfig
import com.example.ui.components.NumericConfigItem
import com.example.viewmodel.EVLabUiState

enum class ConfigCategory(val labelEn: String, val labelFa: String) {
    VEHICLE("1.1 Vehicle", "۱.۱ بدنه و شاسی"),
    DRIVETRAIN("1.2 Transmission", "۱.۲ سیستم انتقال قدرت"),
    MOTOR("1.3 Motor", "۱.۳ الکتروموتور BLDC"),
    BATTERY("1.4 Battery Pack", "۱.۴ باتری و سلول‌ها"),
    THERMAL("1.5 Thermal LPTN", "۱.۵ شبکه حرارتی"),
    CYCLE("1.6 Drive Cycle", "۱.۶ سیکل رانندگی"),
    OPTIMIZATION("1.7 Optimization", "۱.۷ بهینه‌سازی")
}

@Composable
fun ConfigurationScreen(
    uiState: EVLabUiState,
    onUpdateConfig: ((PowertrainConfig) -> PowertrainConfig) -> Unit,
    onApplyAndSimulate: () -> Unit,
    onResetToDefault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFa = uiState.isFarsiPreferred
    var selectedCategory by remember { mutableStateOf(ConfigCategory.VEHICLE) }
    val cfg = uiState.config

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("configuration_screen")
    ) {
        // Top action row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isFa) "داشبورد تنظیمات کاربر (Section 1)" else "User Configuration Dashboard",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isFa) "تمام متغیرهای فایل متلب به صورت بلادرنگ" else "Parametric isolated digital twin setup",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onResetToDefault,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_reset_config")
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFa) "بازنشانی" else "Reset", fontSize = 12.sp)
                }

                Button(
                    onClick = onApplyAndSimulate,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("btn_apply_simulate")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isFa) "اعمال و اجرا" else "Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Horizontal Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConfigCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text(if (isFa) cat.labelFa else cat.labelEn, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.testTag("tab_${cat.name.lowercase()}")
                )
            }
        }

        Divider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

        // Parameters list per category
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedCategory) {
                ConfigCategory.VEHICLE -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Total Vehicle Mass",
                            titleFa = "جرم کل خودرو + سرنشین",
                            value = cfg.vehicle.totalMassKg,
                            unit = "kg",
                            step = 5.0,
                            minVal = 50.0,
                            maxVal = 3500.0,
                            isFarsi = isFa,
                            description = "مجموع وزن بدنه، باتری، موتور و راکب (total_mass_kg)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(totalMassKg = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Dynamic Wheel Radius",
                            titleFa = "شعاع داینامیک چرخ محرک",
                            value = cfg.vehicle.wheelRadiusM,
                            unit = "m",
                            step = 0.01,
                            minVal = 0.10,
                            maxVal = 0.60,
                            isFarsi = isFa,
                            description = "شعاع چرخ بارگذاری شده بر روی جاده (wheel_radius_m)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(wheelRadiusM = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Frontal Drag Area (A)",
                            titleFa = "مساحت تصویر جلوی وسیله",
                            value = cfg.vehicle.frontalAreaM2,
                            unit = "m²",
                            step = 0.05,
                            minVal = 0.20,
                            maxVal = 4.0,
                            isFarsi = isFa,
                            description = "سطح مقطع بادگیر بدنه و سرنشین (frontal_area_m2)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(frontalAreaM2 = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Aerodynamic Drag Coeff (Cd)",
                            titleFa = "ضریب درگ آیرودینامیک",
                            value = cfg.vehicle.dragCoeffCd,
                            unit = "-",
                            step = 0.02,
                            minVal = 0.20,
                            maxVal = 1.20,
                            isFarsi = isFa,
                            description = "ضریب مقاومت هوا متناسب با آیرودینامیک (drag_coeff_Cd)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(dragCoeffCd = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Rotational Inertia Coeff (Ci)",
                            titleFa = "ضریب اینرسی دورانی معادل",
                            value = cfg.vehicle.rotInertiaCi,
                            unit = "-",
                            step = 0.01,
                            minVal = 1.00,
                            maxVal = 1.30,
                            isFarsi = isFa,
                            description = "اثر اینرسی دورانی چرخ‌ها، روتور و زنجیر (rot_inertia_Ci)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(rotInertiaCi = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Rolling Resistance Coeff (Cr)",
                            titleFa = "ضریب پایه مقاومت غلتشی لاستیک",
                            value = cfg.vehicle.crBase,
                            unit = "-",
                            step = 0.001,
                            minVal = 0.005,
                            maxVal = 0.050,
                            isFarsi = isFa,
                            description = "ضریب اصطکاک غلتشی لاستیک روی آسفالت (cr_base)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(vehicle = c.vehicle.copy(crBase = v)) } }
                        )
                    }
                }

                ConfigCategory.DRIVETRAIN -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Gear Ratio (Nc)",
                            titleFa = "نسبت تبدیل دنده / زنجیر / تسمه",
                            value = cfg.drivetrain.gearRatioNc,
                            unit = "-",
                            step = 0.1,
                            minVal = 1.0,
                            maxVal = 15.0,
                            isFarsi = isFa,
                            description = "نسبت دور موتور به دور چرخ عقب (gear_ratio_Nc)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(drivetrain = c.drivetrain.copy(gearRatioNc = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Transmission Efficiency",
                            titleFa = "راندمان مکانیکی انتقال قدرت",
                            value = cfg.drivetrain.etaTrans,
                            unit = "-",
                            step = 0.01,
                            minVal = 0.70,
                            maxVal = 1.00,
                            isFarsi = isFa,
                            description = "راندمان زنجیر، تسمه یا گیربکس (eta_trans)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(drivetrain = c.drivetrain.copy(etaTrans = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Inverter Efficiency",
                            titleFa = "راندمان پایه اینورتر قدرت",
                            value = cfg.drivetrain.etaInverter,
                            unit = "-",
                            step = 0.01,
                            minVal = 0.80,
                            maxVal = 1.00,
                            isFarsi = isFa,
                            description = "راندمان تبدیل الکترونیک قدرت و سویچینگ (eta_inverter)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(drivetrain = c.drivetrain.copy(etaInverter = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Rear Brake Distribution",
                            titleFa = "سهم چرخ عقب از نیروی ترمز",
                            value = cfg.drivetrain.rearBrakeDist,
                            unit = "-",
                            step = 0.05,
                            minVal = 0.10,
                            maxVal = 1.00,
                            isFarsi = isFa,
                            description = "سهم ترمز متصل به موتور برای بازیافت انرژی (rear_brake_dist)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(drivetrain = c.drivetrain.copy(rearBrakeDist = v)) } }
                        )
                    }
                }

                ConfigCategory.MOTOR -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Continuous Torque",
                            titleFa = "گشتاور پیوسته دائم‌کار",
                            value = cfg.motor.contTorqueNm,
                            unit = "N.m",
                            step = 1.0,
                            minVal = 5.0,
                            maxVal = 300.0,
                            isFarsi = isFa,
                            description = "حداکثر گشتاور پایدار بدون داغ شدن بیش از حد (cont_torque_Nm)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(contTorqueNm = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Peak Torque",
                            titleFa = "حداکثر گشتاور پیک لحظه‌ای",
                            value = cfg.motor.peakTorqueNm,
                            unit = "N.m",
                            step = 1.0,
                            minVal = 5.0,
                            maxVal = 500.0,
                            isFarsi = isFa,
                            description = "بیشینه گشتاور در شتاب‌گیری و ترمز شدید (peak_torque_Nm)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(peakTorqueNm = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Max Motor Speed (RPM)",
                            titleFa = "حداکثر دور مجاز شفت موتور",
                            value = cfg.motor.maxRpm,
                            unit = "RPM",
                            step = 100.0,
                            minVal = 1000.0,
                            maxVal = 15000.0,
                            isFarsi = isFa,
                            description = "محدوده قطع دور موتور الکتریکی (max_rpm)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(maxRpm = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Torque Constant (Kt)",
                            titleFa = "ثابت گشتاور موتور (Kt)",
                            value = cfg.motor.torqueConstantKt,
                            unit = "N.m/A",
                            step = 0.01,
                            minVal = 0.05,
                            maxVal = 1.50,
                            isFarsi = isFa,
                            description = "نسبت گشتاور به جریان RMS فاز موتور (torque_constant_Kt)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(torqueConstantKt = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Phase Resistance (Rs @20°C)",
                            titleFa = "مقاومت اهمی هر فاز در ۲۰ درجه",
                            value = cfg.motor.phaseResistanceRs,
                            unit = "Ω",
                            step = 0.005,
                            minVal = 0.005,
                            maxVal = 0.50,
                            isFarsi = isFa,
                            description = "مقاومت فاز استاتور برای محاسبه تلفات مس (phase_resistance_Rs)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(phaseResistanceRs = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Magnet Critical Temp Limit",
                            titleFa = "آستانه دمای دیمغناطیس شدن آهنربا",
                            value = cfg.motor.magnetTempLimitC,
                            unit = "°C",
                            step = 5.0,
                            minVal = 60.0,
                            maxVal = 180.0,
                            isFarsi = isFa,
                            description = "آستانه خطر کاهش خواص مغناطیسی و اعمال Derating (magnet_temp_limit_C)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(motor = c.motor.copy(magnetTempLimitC = v)) } }
                        )
                    }
                }

                ConfigCategory.BATTERY -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Nominal Voltage",
                            titleFa = "ولتاژ نامی پک باتری",
                            value = cfg.battery.nominalVoltageV,
                            unit = "V",
                            step = 4.0,
                            minVal = 24.0,
                            maxVal = 800.0,
                            isFarsi = isFa,
                            description = "ولتاژ پایانه نامی پک لیتیومی (nominal_voltage_V)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(nominalVoltageV = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Pack Capacity",
                            titleFa = "ظرفیت نامی پک باتری",
                            value = cfg.battery.capacityAh,
                            unit = "Ah",
                            step = 1.0,
                            minVal = 5.0,
                            maxVal = 300.0,
                            isFarsi = isFa,
                            description = "ظرفیت کل پک به آمپرساعت (capacity_Ah)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(capacityAh = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Initial SOC",
                            titleFa = "درصد شارژ اولیه باتری",
                            value = cfg.battery.initialSoc,
                            unit = "0-1",
                            step = 0.05,
                            minVal = 0.25,
                            maxVal = 1.00,
                            isFarsi = isFa,
                            description = "وضعیت شارژ در آغاز سیکل شبیه‌سازی (initial_soc)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(initialSoc = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Cutoff SOC (DoD Limit)",
                            titleFa = "حداقل مجاز شارژ باتری (عمق تخلیه)",
                            value = cfg.battery.cutoffSoc,
                            unit = "0-1",
                            step = 0.05,
                            minVal = 0.05,
                            maxVal = 0.40,
                            isFarsi = isFa,
                            description = "مرز تخلیه مجاز جهت حفظ طول عمر باتری (cutoff_soc)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(cutoffSoc = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Internal Resistance (R0)",
                            titleFa = "مقاومت اهمی درونی خالص پک",
                            value = cfg.battery.r0OhmicOhm,
                            unit = "Ω",
                            step = 0.005,
                            minVal = 0.01,
                            maxVal = 0.30,
                            isFarsi = isFa,
                            description = "مدل Thevenin مقاومت داخلی R0 (R0_ohmic_Ohm)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(r0OhmicOhm = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Max Regen Charge Rate",
                            titleFa = "حداکثر ضریب جریان شارژ ترمز",
                            value = cfg.battery.maxRegenCrate,
                            unit = "C-rate",
                            step = 0.2,
                            minVal = 0.2,
                            maxVal = 3.0,
                            isFarsi = isFa,
                            description = "حداکثر ضریب شارژ در ترمز بازیافتی هوشمند (max_regen_crate)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(battery = c.battery.copy(maxRegenCrate = v)) } }
                        )
                    }
                }

                ConfigCategory.THERMAL -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Ambient Temperature",
                            titleFa = "دمای محیط اطراف",
                            value = cfg.thermal.ambientTempC,
                            unit = "°C",
                            step = 1.0,
                            minVal = -10.0,
                            maxVal = 55.0,
                            isFarsi = isFa,
                            description = "دمای اولیه هوای خنک‌کاری (ambient_temp_C)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(thermal = c.thermal.copy(ambientTempC = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Winding Heat Capacity (Cw)",
                            titleFa = "ظرفیت حرارتی سیم‌پیچ استاتور",
                            value = cfg.thermal.cwWindingJK,
                            unit = "J/K",
                            step = 10.0,
                            minVal = 50.0,
                            maxVal = 800.0,
                            isFarsi = isFa,
                            description = "ظرفیت گرمایی مس سیم‌پیچ (Cw_winding_J_K)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(thermal = c.thermal.copy(cwWindingJK = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Rotor Magnet Capacity (Cm)",
                            titleFa = "ظرفیت حرارتی مگنت‌های روتور",
                            value = cfg.thermal.cmMagnetJK,
                            unit = "J/K",
                            step = 10.0,
                            minVal = 50.0,
                            maxVal = 1000.0,
                            isFarsi = isFa,
                            description = "ظرفیت گرمایی آهنرباهای دائم (Cm_magnet_J_K)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(thermal = c.thermal.copy(cmMagnetJK = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Inverter Stage Capacity (Cinv)",
                            titleFa = "ظرفیت حرارتی اینورتر قدرت",
                            value = cfg.thermal.cinvInverterJK,
                            unit = "J/K",
                            step = 10.0,
                            minVal = 50.0,
                            maxVal = 1000.0,
                            isFarsi = isFa,
                            description = "ظرفیت گرمایی ماسفت‌ها و هیت‌سینک (Cinv_inverter_J_K)",
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(thermal = c.thermal.copy(cinvInverterJK = v)) } }
                        )
                    }
                }

                ConfigCategory.CYCLE -> {
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
                                    text = if (isFa) "نوع سیکل شبیه‌سازی (cycle_type)" else "Drive Schedule Type",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CycleType.values().forEach { cType ->
                                        val isSel = cfg.sim.cycleType == cType
                                        FilterChip(
                                            selected = isSel,
                                            onClick = { onUpdateConfig { c -> c.copy(sim = c.sim.copy(cycleType = cType)) } },
                                            label = { Text(if (isFa) cType.labelFa else cType.labelEn) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (cfg.sim.cycleType == CycleType.CRUISE) {
                        item {
                            NumericConfigItem(
                                titleEn = "Cruise Speed",
                                titleFa = "سرعت ثابت خودرو در حرکت",
                                value = cfg.sim.cruiseSpeedKmh,
                                unit = "km/h",
                                step = 5.0,
                                minVal = 10.0,
                                maxVal = 150.0,
                                isFarsi = isFa,
                                onValueChange = { v -> onUpdateConfig { c -> c.copy(sim = c.sim.copy(cruiseSpeedKmh = v)) } }
                            )
                        }
                        item {
                            NumericConfigItem(
                                titleEn = "Road Grade",
                                titleFa = "شیب جاده",
                                value = cfg.sim.cruiseGradePct,
                                unit = "%",
                                step = 1.0,
                                minVal = -15.0,
                                maxVal = 35.0,
                                isFarsi = isFa,
                                onValueChange = { v -> onUpdateConfig { c -> c.copy(sim = c.sim.copy(cruiseGradePct = v)) } }
                            )
                        }
                        item {
                            NumericConfigItem(
                                titleEn = "Cruise Duration",
                                titleFa = "مدت زمان حرکت",
                                value = cfg.sim.cruiseDurationS,
                                unit = "s",
                                step = 30.0,
                                minVal = 30.0,
                                maxVal = 3600.0,
                                isFarsi = isFa,
                                onValueChange = { v -> onUpdateConfig { c -> c.copy(sim = c.sim.copy(cruiseDurationS = v)) } }
                            )
                        }
                    }
                }

                ConfigCategory.OPTIMIZATION -> {
                    item {
                        NumericConfigItem(
                            titleEn = "Min Gear Ratio (Nc Min)",
                            titleFa = "حداقل نسبت دنده مورد ارزیابی",
                            value = cfg.opt.gearRatioMin,
                            unit = "-",
                            step = 0.5,
                            minVal = 1.0,
                            maxVal = 10.0,
                            isFarsi = isFa,
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(opt = c.opt.copy(gearRatioMin = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Max Gear Ratio (Nc Max)",
                            titleFa = "حداکثر نسبت دنده مورد ارزیابی",
                            value = cfg.opt.gearRatioMax,
                            unit = "-",
                            step = 0.5,
                            minVal = 2.0,
                            maxVal = 20.0,
                            isFarsi = isFa,
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(opt = c.opt.copy(gearRatioMax = v)) } }
                        )
                    }
                    item {
                        NumericConfigItem(
                            titleEn = "Sweep Points Count",
                            titleFa = "تعداد نقاط پویش در بازه",
                            value = cfg.opt.numSweepPoints.toDouble(),
                            unit = "steps",
                            step = 2.0,
                            minVal = 5.0,
                            maxVal = 40.0,
                            isFarsi = isFa,
                            onValueChange = { v -> onUpdateConfig { c -> c.copy(opt = c.opt.copy(numSweepPoints = v.toInt())) } }
                        )
                    }
                }
            }
        }
    }
}

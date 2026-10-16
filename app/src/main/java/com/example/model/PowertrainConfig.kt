package com.example.model

enum class CycleType(val labelEn: String, val labelFa: String) {
    WMTC("WMTC Dynamic Cycle (600s)", "سیکل دینامیک WMTC (۶۰۰ ثانیه)"),
    CRUISE("Constant Cruise & Grade", "سرعت و شیب جاده ثابت")
}

data class VehicleParams(
    val totalMassKg: Double = 195.0,
    val wheelRadiusM: Double = 0.28,
    val frontalAreaM2: Double = 0.60,
    val dragCoeffCd: Double = 0.65,
    val rotInertiaCi: Double = 1.08,
    val crBase: Double = 0.012
)

data class DrivetrainParams(
    val gearRatioNc: Double = 4.5,
    val etaTrans: Double = 0.93,
    val etaInverter: Double = 0.95,
    val rearBrakeDist: Double = 0.40
)

data class MotorParams(
    val contTorqueNm: Double = 20.5,
    val peakTorqueNm: Double = 30.0,
    val maxRpm: Double = 3500.0,
    val torqueConstantKt: Double = 0.229,
    val phaseResistanceRs: Double = 0.045,
    val alphaCopper: Double = 0.00393,
    val ironLossFactor: Double = 0.00042,
    val fricLossFactor: Double = 0.0035,
    val magnetTempLimitC: Double = 85.0
)

data class BatteryParams(
    val nominalVoltageV: Double = 72.0,
    val capacityAh: Double = 28.0,
    val initialSoc: Double = 0.95,
    val cutoffSoc: Double = 0.20,
    val r0OhmicOhm: Double = 0.075,
    val r1PolarOhm: Double = 0.040,
    val c1PolarFarad: Double = 2500.0,
    val maxRegenCrate: Double = 1.0
) {
    val grossEnergyWh: Double get() = nominalVoltageV * capacityAh
    val usableEnergyWh: Double get() = grossEnergyWh * (initialSoc - cutoffSoc)
    val capacityAs: Double get() = capacityAh * 3600.0
    val maxRegenPower: Double get() = nominalVoltageV * (capacityAh * maxRegenCrate)
}

data class ThermalParams(
    val ambientTempC: Double = 25.0,
    val cwWindingJK: Double = 180.0,
    val ccCoreJK: Double = 550.0,
    val cmMagnetJK: Double = 220.0,
    val cinvInverterJK: Double = 260.0,
    val rWcKW: Double = 0.42,
    val rCmKW: Double = 1.15,
    val rCa0KW: Double = 1.80,
    val rMaKW: Double = 2.40,
    val rInvaKW: Double = 1.50
)

data class SimSettings(
    val cycleType: CycleType = CycleType.WMTC,
    val timeStepDt: Double = 0.1,
    val cruiseSpeedKmh: Double = 50.0,
    val cruiseGradePct: Double = 3.0,
    val cruiseDurationS: Double = 600.0
)

data class OptimizationSettings(
    val runOptimization: Boolean = true,
    val gearRatioMin: Double = 3.0,
    val gearRatioMax: Double = 6.5,
    val numSweepPoints: Int = 20
)

data class PowertrainPreset(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val description: String,
    val config: PowertrainConfig
)

data class PowertrainConfig(
    val vehicle: VehicleParams = VehicleParams(),
    val drivetrain: DrivetrainParams = DrivetrainParams(),
    val motor: MotorParams = MotorParams(),
    val battery: BatteryParams = BatteryParams(),
    val thermal: ThermalParams = ThermalParams(),
    val sim: SimSettings = SimSettings(),
    val opt: OptimizationSettings = OptimizationSettings()
) {
    companion object {
        fun defaultPresets(): List<PowertrainPreset> = listOf(
            PowertrainPreset(
                id = "emoto_12s14p",
                nameEn = "Electric Motorcycle (12S14P)",
                nameFa = "موتورسیکلت الکتریکی (پیش‌فرض متلب)",
                description = "195 kg, 72V 28Ah Li-Ion, 12S14P PMSM motor with Halbach Array (20.5/30 Nm)",
                config = PowertrainConfig()
            ),
            PowertrainPreset(
                id = "escooter_urban",
                nameEn = "Urban E-Scooter",
                nameFa = "اسکوتر برقی شهری",
                description = "110 kg, 48V 20Ah, 12 Nm cont / 18 Nm peak, Nc = 5.2",
                config = PowertrainConfig(
                    vehicle = VehicleParams(
                        totalMassKg = 110.0,
                        wheelRadiusM = 0.15,
                        frontalAreaM2 = 0.45,
                        dragCoeffCd = 0.70,
                        crBase = 0.015
                    ),
                    drivetrain = DrivetrainParams(gearRatioNc = 5.2, etaTrans = 0.95),
                    motor = MotorParams(
                        contTorqueNm = 12.0,
                        peakTorqueNm = 18.0,
                        maxRpm = 2800.0,
                        torqueConstantKt = 0.18,
                        phaseResistanceRs = 0.055
                    ),
                    battery = BatteryParams(
                        nominalVoltageV = 48.0,
                        capacityAh = 20.0,
                        r0OhmicOhm = 0.09,
                        r1PolarOhm = 0.05
                    )
                )
            ),
            PowertrainPreset(
                id = "light_ev_quad",
                nameEn = "Light EV / Quadricycle",
                nameFa = "خودروی برقی سبک / چهارچرخ",
                description = "450 kg, 96V 100Ah, 55 Nm cont / 95 Nm peak, Nc = 7.2",
                config = PowertrainConfig(
                    vehicle = VehicleParams(
                        totalMassKg = 450.0,
                        wheelRadiusM = 0.30,
                        frontalAreaM2 = 1.2,
                        dragCoeffCd = 0.45,
                        crBase = 0.014
                    ),
                    drivetrain = DrivetrainParams(gearRatioNc = 7.2, etaTrans = 0.92),
                    motor = MotorParams(
                        contTorqueNm = 55.0,
                        peakTorqueNm = 95.0,
                        maxRpm = 4500.0,
                        torqueConstantKt = 0.35,
                        phaseResistanceRs = 0.025
                    ),
                    battery = BatteryParams(
                        nominalVoltageV = 96.0,
                        capacityAh = 100.0,
                        r0OhmicOhm = 0.04,
                        r1PolarOhm = 0.02
                    )
                )
            ),
            PowertrainPreset(
                id = "sport_moto",
                nameEn = "High-Performance Moto",
                nameFa = "موتور الکتریکی اسپرت پرقدرت",
                description = "225 kg, 96V 45Ah, 45 Nm cont / 75 Nm peak, Nc = 3.8",
                config = PowertrainConfig(
                    vehicle = VehicleParams(
                        totalMassKg = 225.0,
                        wheelRadiusM = 0.31,
                        frontalAreaM2 = 0.65,
                        dragCoeffCd = 0.58
                    ),
                    drivetrain = DrivetrainParams(gearRatioNc = 3.8, etaTrans = 0.94),
                    motor = MotorParams(
                        contTorqueNm = 45.0,
                        peakTorqueNm = 75.0,
                        maxRpm = 5000.0,
                        torqueConstantKt = 0.28,
                        phaseResistanceRs = 0.028
                    ),
                    battery = BatteryParams(
                        nominalVoltageV = 96.0,
                        capacityAh = 45.0,
                        r0OhmicOhm = 0.045
                    )
                )
            )
        )
    }
}

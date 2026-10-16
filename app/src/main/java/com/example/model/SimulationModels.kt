package com.example.model

data class TelemetryStep(
    val timeS: Double,
    val speedKmh: Double,
    val accelMs2: Double,
    val tractionForceN: Double,
    val motorRpm: Double,
    val shaftTorqueNm: Double,
    val batteryPowerKw: Double,
    val terminalVoltageV: Double,
    val batteryCurrentA: Double,
    val batterySocPct: Double,
    val windingTempC: Double,
    val coreTempC: Double,
    val magnetTempC: Double,
    val inverterTempC: Double,
    val derateFlag: Boolean
)

data class SimulationResult(
    val telemetry: List<TelemetryStep>,
    val totalDistKm: Double,
    val totalTimeS: Double,
    val posEnergyWh: Double,
    val regenEnergyWh: Double,
    val netEnergyWh: Double,
    val regenPct: Double,
    val specEnergyWhKm: Double,
    val estAutonomyKm: Double,
    val endSocPct: Double,
    val minTerminalVoltageV: Double,
    val peakWindingTempC: Double,
    val peakCoreTempC: Double,
    val peakMagnetTempC: Double,
    val peakInverterTempC: Double,
    val isMagnetSafe: Boolean,
    val magnetTempLimitC: Double
)

data class EfficiencyMapPoint(
    val rpm: Double,
    val torqueNm: Double,
    val efficiencyPct: Double
)

data class EfficiencyMapData(
    val rpmValues: List<Double>,
    val torqueValues: List<Double>,
    val efficiencyMatrix: List<List<Double>>, // [torqueIndex][rpmIndex]
    val operatingScatter: List<Pair<Double, Double>> // list of (RPM, Torque) from drive cycle
)

data class OptimizationPoint(
    val gearRatioNc: Double,
    val accelTime0to60S: Double,
    val specEnergyWhKm: Double,
    val estPeakMagnetTempC: Double
)

data class OptimizationResult(
    val points: List<OptimizationPoint>,
    val optimalIndex: Int
)

data class InverterSizingInput(
    val peakPhaseCurrentArms: Double = 131.0,  // T_peak / K_t
    val dcBusVoltageV: Double = 72.0,
    val switchingFreqKhz: Double = 16.0,
    val allowedVoltageRipplePct: Double = 2.0, // % of DC Bus
    val ambientTempC: Double = 25.0,
    val rdsOnMilliOhm: Double = 1.8,           // per MOSFET in bridge
    val numParallelMosfets: Int = 2
)

data class InverterSizingResult(
    val ratedPhaseCurrentPeakA: Double,
    val recommendedMosfetVoltageRatingV: Double,
    val recommendedMosfetCurrentRatingA: Double,
    val maxAllowedDcRippleVoltageV: Double,
    val requiredBusCapacitanceMicroFarad: Double,
    val capacitorRmsRippleCurrentA: Double,
    val conductionLossesW: Double,
    val estimatedSwitchingLossesW: Double,
    val totalInverterHeatLossesW: Double,
    val requiredHeatSinkThermalResistanceKW: Double
)

data class TopSpeedGradeabilityResult(
    val maxSpeedKmh: Double,
    val maxContinuousGradePct: Double,
    val maxPeakGradePct: Double,
    val stallWheelTorqueNm: Double,
    val mechanicalPowerAtContTorqueKw: Double,
    val mechanicalPowerAtPeakTorqueKw: Double
)

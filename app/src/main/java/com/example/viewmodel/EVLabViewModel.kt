package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.engine.EVPowertrainEngine
import com.example.engine.InverterCalculator
import com.example.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class EVLabUiState(
    val config: PowertrainConfig = PowertrainConfig(),
    val presets: List<PowertrainPreset> = PowertrainConfig.defaultPresets(),
    val selectedPresetId: String = "emoto_12s14p",
    val isSimulating: Boolean = false,
    val simulationResult: SimulationResult? = null,
    val selectedTimeIndex: Int = 0,
    val efficiencyMap: EfficiencyMapData? = null,
    val optimizationResult: OptimizationResult? = null,
    val isOptimizing: Boolean = false,
    val inverterInput: InverterSizingInput = InverterSizingInput(),
    val inverterResult: InverterSizingResult? = null,
    val topSpeedResult: TopSpeedGradeabilityResult? = null,
    val targetRangeKm: Double = 100.0,
    val batteryPackEstimate: InverterCalculator.BatteryPackEstimate? = null,
    val statusMessage: String? = null,
    val isFarsiPreferred: Boolean = true
)

class EVLabViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(EVLabUiState())
    val uiState: StateFlow<EVLabUiState> = _uiState.asStateFlow()

    init {
        // Run initial simulation and calculations on startup
        runFullSimulation()
    }

    fun selectPreset(presetId: String) {
        val preset = _uiState.value.presets.find { it.id == presetId } ?: return
        val currentCfg = preset.config
        _uiState.value = _uiState.value.copy(
            selectedPresetId = presetId,
            config = currentCfg,
            inverterInput = _uiState.value.inverterInput.copy(
                dcBusVoltageV = currentCfg.battery.nominalVoltageV,
                peakPhaseCurrentArms = currentCfg.motor.peakTorqueNm / currentCfg.motor.torqueConstantKt
            )
        )
        runFullSimulation()
    }

    fun updateConfig(updater: (PowertrainConfig) -> PowertrainConfig) {
        val newCfg = updater(_uiState.value.config)
        _uiState.value = _uiState.value.copy(
            config = newCfg,
            selectedPresetId = "custom",
            inverterInput = _uiState.value.inverterInput.copy(
                dcBusVoltageV = newCfg.battery.nominalVoltageV,
                peakPhaseCurrentArms = newCfg.motor.peakTorqueNm / newCfg.motor.torqueConstantKt
            )
        )
    }

    fun updateInverterInput(updater: (InverterSizingInput) -> InverterSizingInput) {
        val newInput = updater(_uiState.value.inverterInput)
        val newResult = InverterCalculator.calculateInverterSizing(newInput)
        _uiState.value = _uiState.value.copy(
            inverterInput = newInput,
            inverterResult = newResult
        )
    }

    fun updateTargetRange(km: Double) {
        val simRes = _uiState.value.simulationResult
        val specEnergy = simRes?.specEnergyWhKm ?: 35.0
        val estimate = InverterCalculator.estimateBatteryPack(
            targetDistanceKm = km,
            specEnergyWhKm = specEnergy,
            nominalVoltageV = _uiState.value.config.battery.nominalVoltageV
        )
        _uiState.value = _uiState.value.copy(
            targetRangeKm = km,
            batteryPackEstimate = estimate
        )
    }

    fun setScrubberIndex(index: Int) {
        val total = _uiState.value.simulationResult?.telemetry?.size ?: 1
        _uiState.value = _uiState.value.copy(
            selectedTimeIndex = index.coerceIn(0, total - 1)
        )
    }

    fun toggleLanguage() {
        _uiState.value = _uiState.value.copy(
            isFarsiPreferred = !_uiState.value.isFarsiPreferred
        )
    }

    fun clearStatusMessage() {
        _uiState.value = _uiState.value.copy(statusMessage = null)
    }

    fun runFullSimulation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSimulating = true)
            val cfg = _uiState.value.config

            val result = withContext(Dispatchers.Default) {
                EVPowertrainEngine.runSimulation(cfg)
            }

            val effMap = withContext(Dispatchers.Default) {
                EVPowertrainEngine.generateEfficiencyMap(cfg, result)
            }

            val topSpeed = withContext(Dispatchers.Default) {
                InverterCalculator.calculateTopSpeedAndGradeability(cfg)
            }

            val invResult = withContext(Dispatchers.Default) {
                InverterCalculator.calculateInverterSizing(_uiState.value.inverterInput)
            }

            val rangeEstimate = withContext(Dispatchers.Default) {
                InverterCalculator.estimateBatteryPack(
                    targetDistanceKm = _uiState.value.targetRangeKm,
                    specEnergyWhKm = result.specEnergyWhKm,
                    nominalVoltageV = cfg.battery.nominalVoltageV
                )
            }

            _uiState.value = _uiState.value.copy(
                isSimulating = false,
                simulationResult = result,
                efficiencyMap = effMap,
                topSpeedResult = topSpeed,
                inverterResult = invResult,
                batteryPackEstimate = rangeEstimate,
                selectedTimeIndex = (result.telemetry.size / 2).coerceAtLeast(0),
                statusMessage = if (_uiState.value.isFarsiPreferred)
                    "شبیه‌سازی کامل با موفقیت انجام شد."
                else
                    "Simulation successfully executed."
            )
        }
    }

    fun runGearOptimizationSweep() {
        val baseSim = _uiState.value.simulationResult ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isOptimizing = true)
            val cfg = _uiState.value.config
            val optResult = withContext(Dispatchers.Default) {
                EVPowertrainEngine.runOptimizationSweep(cfg, baseSim)
            }
            _uiState.value = _uiState.value.copy(
                isOptimizing = false,
                optimizationResult = optResult,
                statusMessage = if (_uiState.value.isFarsiPreferred)
                    "بهینه‌سازی نسبت دنده با موفقیت انجام شد."
                else
                    "Gear ratio optimization sweep complete."
            )
        }
    }

    fun generateAuditReport(): String {
        val sim = _uiState.value.simulationResult ?: return "No simulation data available."
        val cfg = _uiState.value.config
        val top = _uiState.value.topSpeedResult
        val inv = _uiState.value.inverterResult

        return buildString {
            appendLine("========================================================================================")
            appendLine("               ELECTRIC VEHICLE POWERTRAIN DIGITAL TWIN AUDIT REPORT                    ")
            appendLine("========================================================================================")
            appendLine("Platform Target           : Light EV / Electric Motorcycle Conversion")
            appendLine("Vehicle Operational Mass  : ${String.format("%.1f", cfg.vehicle.totalMassKg)} kg")
            appendLine("Installed Battery Pack    : ${String.format("%.1f", cfg.battery.nominalVoltageV)} V, ${String.format("%.1f", cfg.battery.capacityAh)} Ah (${String.format("%.2f", cfg.battery.grossEnergyWh)} Wh Gross)")
            appendLine("Drive Schedule Executed   : ${cfg.sim.cycleType.labelEn}")
            appendLine("Selected Gear Ratio (Nc)  : ${String.format("%.2f", cfg.drivetrain.gearRatioNc)}")
            appendLine("----------------------------------------------------------------------------------------")
            appendLine("1. ENERGY DYNAMICS & AUTONOMY")
            appendLine("   - Traveled Distance             : ${String.format("%8.3f", sim.totalDistKm)} km")
            appendLine("   - Gross Traction Energy         : ${String.format("%8.3f", sim.posEnergyWh)} Wh")
            appendLine("   - Regenerated Braking Energy    : ${String.format("%8.3f", sim.regenEnergyWh)} Wh (${String.format("%.2f", sim.regenPct)} % Recovery)")
            appendLine("   - Net Consumed Energy           : ${String.format("%8.3f", sim.netEnergyWh)} Wh")
            appendLine("   - Specific Energy Consumption   : ${String.format("%8.2f", sim.specEnergyWhKm)} Wh/km")
            appendLine("   - Usable Battery Energy (DoD)   : ${String.format("%8.2f", cfg.battery.usableEnergyWh)} Wh")
            appendLine("   - Estimated Full Travel Range   : ${String.format("%8.2f", sim.estAutonomyKm)} km")
            appendLine("   - End-of-Run Battery SOC        : ${String.format("%8.2f", sim.endSocPct)} %")
            appendLine("   - Minimum Terminal Voltage      : ${String.format("%8.2f", sim.minTerminalVoltageV)} V")
            appendLine("----------------------------------------------------------------------------------------")
            appendLine("2. THERMAL PERFORMANCE (4-NODE LPTN)")
            appendLine("   - Peak Stator Winding Temp      : ${String.format("%8.2f", sim.peakWindingTempC)} deg C")
            appendLine("   - Peak Stator Iron Core Temp    : ${String.format("%8.2f", sim.peakCoreTempC)} deg C")
            appendLine("   - Peak Rotor Magnet Temp        : ${String.format("%8.2f", sim.peakMagnetTempC)} deg C")
            appendLine("   - Peak Power Inverter Temp      : ${String.format("%8.2f", sim.peakInverterTempC)} deg C")
            if (sim.isMagnetSafe) {
                appendLine("   - Magnet Demagnetization Status : SAFE (Within allowable thermal limits < ${sim.magnetTempLimitC}°C)")
            } else {
                appendLine("   - Magnet Demagnetization Status : WARNING (Exceeded critical threshold of ${sim.magnetTempLimitC}°C!)")
            }
            appendLine("----------------------------------------------------------------------------------------")
            appendLine("3. INVERTER & VEHICLE CAPABILITIES")
            if (top != null) {
                appendLine("   - Max Achievable Flat Speed     : ${String.format("%8.1f", top.maxSpeedKmh)} km/h")
                appendLine("   - Max Peak Gradeability         : ${String.format("%8.1f", top.maxPeakGradePct)} %")
                appendLine("   - Max Continuous Gradeability   : ${String.format("%8.1f", top.maxContinuousGradePct)} %")
                appendLine("   - Wheel Stall Torque            : ${String.format("%8.1f", top.stallWheelTorqueNm)} N.m")
            }
            if (inv != null) {
                appendLine("   - Recommended MOSFET Vds Rating : >= ${String.format("%8.1f", inv.recommendedMosfetVoltageRatingV)} V")
                appendLine("   - Required Bus Link Capacitance : ${String.format("%8.1f", inv.requiredBusCapacitanceMicroFarad)} uF")
                appendLine("   - Total Inverter Heat Losses    : ${String.format("%8.1f", inv.totalInverterHeatLossesW)} W")
                appendLine("   - Max Heatsink Thermal Res R_th : <= ${String.format("%8.3f", inv.requiredHeatSinkThermalResistanceKW)} K/W")
            }
            appendLine("========================================================================================")
        }
    }

    fun exportTelemetryCsv(): String {
        val sim = _uiState.value.simulationResult ?: return ""
        return buildString {
            appendLine("Time_s,Speed_kmh,Acceleration_ms2,TractionForce_N,Motor_RPM,ShaftTorque_Nm,BatteryPower_kW,TerminalVoltage_V,BatteryCurrent_A,Battery_SOC_Pct,WindingTemp_C,CoreTemp_C,MagnetTemp_C,InverterTemp_C,DerateFlag")
            sim.telemetry.forEach { step ->
                appendLine(
                    "${step.timeS},${step.speedKmh},${step.accelMs2},${step.tractionForceN}," +
                            "${step.motorRpm},${step.shaftTorqueNm},${step.batteryPowerKw}," +
                            "${step.terminalVoltageV},${step.batteryCurrentA},${step.batterySocPct}," +
                            "${step.windingTempC},${step.coreTempC},${step.magnetTempC},${step.inverterTempC}," +
                            "${if (step.derateFlag) 1 else 0}"
                )
            }
        }
    }
}

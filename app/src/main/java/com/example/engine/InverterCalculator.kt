package com.example.engine

import com.example.model.*
import kotlin.math.*

object InverterCalculator {

    fun calculateInverterSizing(input: InverterSizingInput): InverterSizingResult {
        val iRms = input.peakPhaseCurrentArms
        val iPeak = iRms * sqrt(2.0)
        val vDc = input.dcBusVoltageV
        val fSwHz = input.switchingFreqKhz * 1000.0

        // 1. Voltage & Current Ratings with Automotive Safety Factors
        val recommendedVds = (vDc * 1.25) * 1.5 // 1.25 for max charge, 1.5 safety margin
        val recommendedId = (iRms * 1.5) / input.numParallelMosfets

        // 2. DC Bus Link Capacitor Sizing
        val allowedDeltaV = vDc * (input.allowedVoltageRipplePct / 100.0)
        // Standard formula: C_dc = (I_rms * sqrt(2)) / (8 * f_sw * Delta_V)
        val cDcF = if (allowedDeltaV > 0.0 && fSwHz > 0.0) {
            (iRms * sqrt(2.0)) / (8.0 * fSwHz * allowedDeltaV)
        } else {
            1000e-6
        }
        val cDcMicroFarad = cDcF * 1e6

        // Capacitor RMS Ripple current approx ~ 0.55 * I_phase_rms
        val iCapRms = iRms * 0.55

        // 3. Loss Calculations
        val rdsEffectiveOhm = (input.rdsOnMilliOhm / 1000.0) / input.numParallelMosfets.coerceAtLeast(1)
        // 3-phase full bridge conduction loss: 6 switches, average duty 0.5
        val pCond = 3.0 * (iRms * iRms) * rdsEffectiveOhm

        // Switching loss estimation: 6 switches * f_sw * 0.5 * V_dc * I_avg * (t_rise + t_fall)
        val tRiseFallSec = 85e-9 // typical 85ns for automotive Si MOSFETs
        val pSwitching = 6.0 * fSwHz * (0.5 * vDc * (iPeak / PI) * tRiseFallSec)

        val pTotalLoss = pCond + pSwitching

        // Heat Sink Thermal Resistance (Target max junction 105°C, R_jc ~ 0.5 K/W)
        val tJunctionMax = 105.0
        val rJunctionCase = 0.4
        val totalAllowedThermalRes = if (pTotalLoss > 0.0) (tJunctionMax - input.ambientTempC) / pTotalLoss else 5.0
        val rHeatSinkKW = max(0.05, totalAllowedThermalRes - rJunctionCase)

        return InverterSizingResult(
            ratedPhaseCurrentPeakA = iPeak,
            recommendedMosfetVoltageRatingV = recommendedVds,
            recommendedMosfetCurrentRatingA = recommendedId,
            maxAllowedDcRippleVoltageV = allowedDeltaV,
            requiredBusCapacitanceMicroFarad = cDcMicroFarad,
            capacitorRmsRippleCurrentA = iCapRms,
            conductionLossesW = pCond,
            estimatedSwitchingLossesW = pSwitching,
            totalInverterHeatLossesW = pTotalLoss,
            requiredHeatSinkThermalResistanceKW = rHeatSinkKW
        )
    }

    fun calculateTopSpeedAndGradeability(cfg: PowertrainConfig): TopSpeedGradeabilityResult {
        val mass = cfg.vehicle.totalMassKg
        val g = EVPowertrainEngine.GRAVITY
        val rho = EVPowertrainEngine.AIR_DENSITY
        val cd = cfg.vehicle.dragCoeffCd
        val area = cfg.vehicle.frontalAreaM2
        val cr = cfg.vehicle.crBase
        val rw = cfg.vehicle.wheelRadiusM
        val nc = cfg.drivetrain.gearRatioNc
        val eta = cfg.drivetrain.etaTrans

        // 1. Wheel Torque
        val tContWheel = cfg.motor.contTorqueNm * nc * eta
        val tPeakWheel = cfg.motor.peakTorqueNm * nc * eta

        // 2. Maximum Gradeability at Low Speed / Stall
        val fTracCont = tContWheel / rw
        val fTracPeak = tPeakWheel / rw

        val fRollingBase = cr * mass * g

        val sinCont = ((fTracCont - fRollingBase) / (mass * g)).coerceIn(0.0, 0.99)
        val sinPeak = ((fTracPeak - fRollingBase) / (mass * g)).coerceIn(0.0, 0.99)

        val gradeContPct = tan(asin(sinCont)) * 100.0
        val gradePeakPct = tan(asin(sinPeak)) * 100.0

        // 3. Top Speed on Flat Ground (binary search up to max RPM limit)
        val maxSpeedFromRpm = (cfg.motor.maxRpm / nc) * (2.0 * PI / 60.0) * rw * 3.6

        var lowV = 0.0
        var highV = maxSpeedFromRpm / 3.6
        for (iter in 0 until 60) {
            val midV = (lowV + highV) / 2.0
            val rpmMot = (midV / rw) * (60.0 / (2.0 * PI)) * nc
            if (rpmMot > cfg.motor.maxRpm) {
                highV = midV
                continue
            }
            // Continuous torque reduction above base speed
            val tAvail = cfg.motor.contTorqueNm * (1.0 - 0.25 * max(0.0, (rpmMot - 2200.0) / 1300.0).coerceAtMost(0.8))
            val fTracMid = (tAvail * nc * eta) / rw

            val crDyn = cr * (1.0 + (midV * 2.23694) / 100.0)
            val fResMid = (crDyn * mass * g) + (0.5 * rho * area * cd * (midV * midV))

            if (fTracMid >= fResMid) {
                lowV = midV
            } else {
                highV = midV
            }
        }
        val topSpeedKmh = min(maxSpeedFromRpm, lowV * 3.6)

        val pMechContKw = (cfg.motor.contTorqueNm * (cfg.motor.maxRpm * 0.7 * 2.0 * PI / 60.0)) / 1000.0
        val pMechPeakKw = (cfg.motor.peakTorqueNm * (cfg.motor.maxRpm * 0.7 * 2.0 * PI / 60.0)) / 1000.0

        return TopSpeedGradeabilityResult(
            maxSpeedKmh = topSpeedKmh,
            maxContinuousGradePct = gradeContPct,
            maxPeakGradePct = gradePeakPct,
            stallWheelTorqueNm = tPeakWheel,
            mechanicalPowerAtContTorqueKw = pMechContKw,
            mechanicalPowerAtPeakTorqueKw = pMechPeakKw
        )
    }

    data class BatteryPackEstimate(
        val targetDistanceKm: Double,
        val avgConsumptionWhKm: Double,
        val requiredGrossEnergyKwh: Double,
        val requiredCapacityAh: Double,
        val estimatedPackWeightKg: Double,
        val cellCount18650Estimate: Int
    )

    fun estimateBatteryPack(
        targetDistanceKm: Double,
        specEnergyWhKm: Double,
        nominalVoltageV: Double,
        depthOfDischarge: Double = 0.80,
        cellWhPerKg: Double = 220.0
    ): BatteryPackEstimate {
        val usableKwh = (targetDistanceKm * specEnergyWhKm) / 1000.0
        val grossKwh = usableKwh / depthOfDischarge.coerceIn(0.5, 0.95)
        val grossWh = grossKwh * 1000.0
        val capacityAh = grossWh / nominalVoltageV
        val packWeightKg = grossWh / cellWhPerKg
        // Approx 3.6V 3.0Ah (10.8 Wh) cell
        val cellCount = ceil(grossWh / 10.8).toInt()

        return BatteryPackEstimate(
            targetDistanceKm = targetDistanceKm,
            avgConsumptionWhKm = specEnergyWhKm,
            requiredGrossEnergyKwh = grossKwh,
            requiredCapacityAh = capacityAh,
            estimatedPackWeightKg = packWeightKg,
            cellCount18650Estimate = cellCount
        )
    }
}

package com.example.engine

import com.example.model.*
import kotlin.math.*

object EVPowertrainEngine {

    const val GRAVITY = 9.80665
    const val AIR_DENSITY = 1.225

    fun runSimulation(cfg: PowertrainConfig): SimulationResult {
        val dt = cfg.sim.timeStepDt.coerceIn(0.05, 0.5)
        val driveSchedule = generateDriveSchedule(cfg.sim, dt)
        val stepsCount = driveSchedule.size

        val telemetry = ArrayList<TelemetryStep>(stepsCount)

        var soc = cfg.battery.initialSoc.coerceIn(0.05, 1.0)
        var v1 = 0.0
        var tw = cfg.thermal.ambientTempC
        var tc = cfg.thermal.ambientTempC
        var tm = cfg.thermal.ambientTempC
        var tinv = cfg.thermal.ambientTempC

        var posEnergyWs = 0.0
        var regenEnergyWs = 0.0
        var totalDistM = 0.0

        var peakTw = tw
        var peakTc = tc
        var peakTm = tm
        var peakTinv = tinv
        var minVterm = Double.MAX_VALUE

        val voltageScale = cfg.battery.nominalVoltageV / 72.0

        for (k in 0 until stepsCount) {
            val point = driveSchedule[k]
            val vk = point.speedMs
            val ak = point.accelMs2
            val gradePct = point.gradePct
            val theta = atan(gradePct / 100.0)

            totalDistM += vk * dt

            // 1. Resistive Forces
            val crDyn = cfg.vehicle.crBase * (1.0 + (vk * 2.23694) / 100.0)
            val fRr = crDyn * cfg.vehicle.totalMassKg * GRAVITY * cos(theta)
            val fDrag = 0.5 * AIR_DENSITY * cfg.vehicle.frontalAreaM2 * cfg.vehicle.dragCoeffCd * (vk * vk)
            val fGrd = cfg.vehicle.totalMassKg * GRAVITY * sin(theta)
            val fIner = cfg.vehicle.totalMassKg * cfg.vehicle.rotInertiaCi * ak
            val fDem = fRr + fDrag + fGrd + fIner

            // 2. Drivetrain Rotational Kinematics
            val wWheel = vk / cfg.vehicle.wheelRadiusM
            val rpmMot = (wWheel * 60.0 / (2.0 * PI)) * cfg.drivetrain.gearRatioNc
            val wMot = rpmMot * (2.0 * PI / 60.0)

            // 3. Power Distribution & Smart Regen
            val tSh: Double
            val derate: Boolean
            val pBatt: Double
            val pElecMot: Double
            val pCu: Double
            val pFe = cfg.motor.ironLossFactor * (rpmMot * rpmMot)

            val rsT = cfg.motor.phaseResistanceRs * (1.0 + cfg.motor.alphaCopper * (tw - 20.0))

            if (fDem >= 0.0) {
                // Traction mode
                val tWheel = fDem * cfg.vehicle.wheelRadiusM
                val tReq = tWheel / (cfg.drivetrain.gearRatioNc * cfg.drivetrain.etaTrans)

                if (tm >= cfg.motor.magnetTempLimitC) {
                    tSh = min(tReq, cfg.motor.contTorqueNm * 0.70)
                    derate = true
                } else {
                    tSh = min(tReq, cfg.motor.peakTorqueNm)
                    derate = false
                }

                val iPh = tSh / cfg.motor.torqueConstantKt
                pCu = 3.0 * (iPh * iPh) * rsT
                pElecMot = (tSh * wMot) + pCu + pFe
                pBatt = pElecMot / cfg.drivetrain.etaInverter
                posEnergyWs += pBatt * dt
            } else {
                // Smart Regenerative Braking
                derate = false
                val fBrakeRear = abs(fDem) * cfg.drivetrain.rearBrakeDist
                val tRegenReq = (fBrakeRear * cfg.vehicle.wheelRadiusM * cfg.drivetrain.etaTrans) / cfg.drivetrain.gearRatioNc
                tSh = -min(tRegenReq, cfg.motor.peakTorqueNm * 0.85)

                val iPh = abs(tSh) / cfg.motor.torqueConstantKt
                pCu = 3.0 * (iPh * iPh) * rsT
                val pElecGen = max(0.0, (abs(tSh) * wMot) - pCu - pFe)

                pBatt = -min(pElecGen * cfg.drivetrain.etaInverter, cfg.battery.maxRegenPower)
                pElecMot = -pElecGen
                regenEnergyWs += abs(pBatt) * dt
            }

            // 4. Li-Ion 1-RC Thevenin ECM Battery Model
            val vocRaw = 64.0 + 14.8 * soc - 4.5 * (soc * soc) + 3.7 * (soc * soc * soc)
            val voc = vocRaw * voltageScale

            val disc = max(0.0, (voc - v1).pow(2.0) - 4.0 * cfg.battery.r0OhmicOhm * pBatt)
            val iBatt = ((voc - v1) - sqrt(disc)) / (2.0 * cfg.battery.r0OhmicOhm)
            val vTerm = voc - v1 - iBatt * cfg.battery.r0OhmicOhm

            minVterm = min(minVterm, vTerm)

            soc = (soc - (iBatt * dt) / cfg.battery.capacityAs).coerceIn(0.0, 1.0)
            val tauPolar = cfg.battery.r1PolarOhm * cfg.battery.c1PolarFarad
            val expDecay = exp(-dt / tauPolar)
            v1 = v1 * expDecay + cfg.battery.r1PolarOhm * (1.0 - expDecay) * iBatt

            // 5. 4-Node LPTN Thermal Differential Model
            val rCaDyn = cfg.thermal.rCa0KW / (1.0 + 0.06 * vk)
            val pLossCu = max(0.0, pCu)
            val pLossFe = max(0.0, pFe)
            val pLossMag = 0.05 * pLossFe
            val pLossInv = abs(pBatt - pElecMot)

            val dTw = (pLossCu - (tw - tc) / cfg.thermal.rWcKW) / cfg.thermal.cwWindingJK
            val dTc = (pLossFe + (tw - tc) / cfg.thermal.rWcKW - (tc - tm) / cfg.thermal.rCmKW - (tc - cfg.thermal.ambientTempC) / rCaDyn) / cfg.thermal.ccCoreJK
            val dTm = (pLossMag + (tc - tm) / cfg.thermal.rCmKW - (tm - cfg.thermal.ambientTempC) / cfg.thermal.rMaKW) / cfg.thermal.cmMagnetJK
            val dTinv = (pLossInv - (tinv - cfg.thermal.ambientTempC) / cfg.thermal.rInvaKW) / cfg.thermal.cinvInverterJK

            tw += dTw * dt
            tc += dTc * dt
            tm += dTm * dt
            tinv += dTinv * dt

            peakTw = max(peakTw, tw)
            peakTc = max(peakTc, tc)
            peakTm = max(peakTm, tm)
            peakTinv = max(peakTinv, tinv)

            telemetry.add(
                TelemetryStep(
                    timeS = point.timeS,
                    speedKmh = vk * 3.6,
                    accelMs2 = ak,
                    tractionForceN = fDem,
                    motorRpm = rpmMot,
                    shaftTorqueNm = tSh,
                    batteryPowerKw = pBatt / 1000.0,
                    terminalVoltageV = vTerm,
                    batteryCurrentA = iBatt,
                    batterySocPct = soc * 100.0,
                    windingTempC = tw,
                    coreTempC = tc,
                    magnetTempC = tm,
                    inverterTempC = tinv,
                    derateFlag = derate
                )
            )
        }

        val totalDistKm = max(0.001, totalDistM / 1000.0)
        val posEnergyWh = posEnergyWs / 3600.0
        val regenEnergyWh = regenEnergyWs / 3600.0
        val netEnergyWh = posEnergyWh - regenEnergyWh
        val regenPct = if (posEnergyWh > 0.0) (regenEnergyWh / posEnergyWh) * 100.0 else 0.0
        val specEnergyWhKm = netEnergyWh / totalDistKm
        val estAutonomyKm = if (specEnergyWhKm > 0.0) cfg.battery.usableEnergyWh / specEnergyWhKm else 0.0

        return SimulationResult(
            telemetry = telemetry,
            totalDistKm = totalDistKm,
            totalTimeS = driveSchedule.lastOrNull()?.timeS ?: 0.0,
            posEnergyWh = posEnergyWh,
            regenEnergyWh = regenEnergyWh,
            netEnergyWh = netEnergyWh,
            regenPct = regenPct,
            specEnergyWhKm = specEnergyWhKm,
            estAutonomyKm = estAutonomyKm,
            endSocPct = soc * 100.0,
            minTerminalVoltageV = if (minVterm == Double.MAX_VALUE) cfg.battery.nominalVoltageV else minVterm,
            peakWindingTempC = peakTw,
            peakCoreTempC = peakTc,
            peakMagnetTempC = peakTm,
            peakInverterTempC = peakTinv,
            isMagnetSafe = peakTm < cfg.motor.magnetTempLimitC,
            magnetTempLimitC = cfg.motor.magnetTempLimitC
        )
    }

    private data class SchedulePoint(
        val timeS: Double,
        val speedMs: Double,
        val accelMs2: Double,
        val gradePct: Double
    )

    private fun generateDriveSchedule(settings: SimSettings, dt: Double): List<SchedulePoint> {
        val points = mutableListOf<SchedulePoint>()
        if (settings.cycleType == CycleType.WMTC) {
            val totalTime = 600.0
            var t = 0.0
            val speedList = mutableListOf<Double>()
            val timeList = mutableListOf<Double>()

            while (t <= totalTime + 1e-6) {
                timeList.add(t)
                val vKmh = when {
                    t < 30.0 -> 0.0
                    t < 65.0 -> (30.0 / 35.0) * (t - 30.0)
                    t < 120.0 -> 30.0 + 3.0 * sin(0.15 * (t - 65.0))
                    t < 145.0 -> max(0.0, 30.0 - (30.0 / 25.0) * (t - 120.0))
                    t < 165.0 -> 0.0
                    t < 220.0 -> (45.0 / 55.0) * (t - 165.0)
                    t < 310.0 -> 45.0 + 4.0 * sin(0.1 * (t - 220.0)) - 2.0 * cos(0.2 * (t - 220.0))
                    t < 340.0 -> max(0.0, 45.0 - (45.0 / 30.0) * (t - 310.0))
                    t < 360.0 -> 0.0
                    t < 440.0 -> (65.0 / 80.0) * (t - 360.0)
                    t < 530.0 -> 65.0 + 5.0 * sin(0.08 * (t - 440.0))
                    t < 580.0 -> max(0.0, 65.0 - (65.0 / 50.0) * (t - 530.0))
                    else -> 0.0
                }
                speedList.add(vKmh / 3.6)
                t += dt
            }

            for (i in speedList.indices) {
                val accel = if (i == 0) 0.0 else (speedList[i] - speedList[i - 1]) / dt
                points.add(
                    SchedulePoint(
                        timeS = timeList[i],
                        speedMs = speedList[i],
                        accelMs2 = accel,
                        gradePct = 0.0
                    )
                )
            }
        } else {
            // Cruise Schedule
            val duration = settings.cruiseDurationS.coerceIn(30.0, 3600.0)
            val rampTime = 5.0
            val targetMs = settings.cruiseSpeedKmh / 3.6
            var t = 0.0

            var prevV = 0.0
            while (t <= duration + 1e-6) {
                val v = if (t < rampTime) (t / rampTime) * targetMs else targetMs
                val a = if (points.isEmpty()) 0.0 else (v - prevV) / dt
                points.add(
                    SchedulePoint(
                        timeS = t,
                        speedMs = v,
                        accelMs2 = a,
                        gradePct = settings.cruiseGradePct
                    )
                )
                prevV = v
                t += dt
            }
        }
        return points
    }

    fun generateEfficiencyMap(cfg: PowertrainConfig, simResult: SimulationResult?): EfficiencyMapData {
        val numRpm = 40
        val numTorque = 40

        val rpmList = (0 until numRpm).map { i ->
            100.0 + i * (cfg.motor.maxRpm - 100.0) / (numRpm - 1)
        }
        val torqueList = (0 until numTorque).map { i ->
            0.0 + i * cfg.motor.peakTorqueNm / (numTorque - 1)
        }

        val matrix = mutableListOf<List<Double>>()

        for (t in torqueList) {
            val row = mutableListOf<Double>()
            for (rpm in rpmList) {
                val w = rpm * 2.0 * PI / 60.0
                val pMech = t * w
                if (pMech <= 1.0) {
                    row.add(0.0)
                    continue
                }
                val iRms = t / cfg.motor.torqueConstantKt
                val pCu = 3.0 * (iRms * iRms) * cfg.motor.phaseResistanceRs
                val pFe = cfg.motor.ironLossFactor * (rpm * rpm)
                val pFric = cfg.motor.fricLossFactor * rpm
                val pLoss = pCu + pFe + pFric + 15.0
                val eff = (pMech / (pMech + pLoss)) * 100.0
                row.add(eff.coerceIn(0.0, 98.0))
            }
            matrix.add(row)
        }

        val scatter = mutableListOf<Pair<Double, Double>>()
        simResult?.telemetry?.forEach { step ->
            if (step.speedKmh > 1.5 && step.shaftTorqueNm > 0.0) {
                scatter.add(Pair(step.motorRpm, step.shaftTorqueNm))
            }
        }

        return EfficiencyMapData(
            rpmValues = rpmList,
            torqueValues = torqueList,
            efficiencyMatrix = matrix,
            operatingScatter = scatter
        )
    }

    fun runOptimizationSweep(cfg: PowertrainConfig, baseSim: SimulationResult): OptimizationResult {
        val nPoints = cfg.opt.numSweepPoints.coerceIn(5, 30)
        val minRatio = cfg.opt.gearRatioMin
        val maxRatio = cfg.opt.gearRatioMax

        val ratios = (0 until nPoints).map { i ->
            minRatio + i * (maxRatio - minRatio) / (nPoints - 1)
        }

        val points = mutableListOf<OptimizationPoint>()

        for (nc in ratios) {
            // Full-throttle acceleration 0-60 km/h simulation
            var vAcc = 0.0
            var tAcc = 0.0
            val dtAcc = 0.05
            val targetMs = 60.0 / 3.6

            while (vAcc < targetMs && tAcc < 30.0) {
                val wWh = vAcc / cfg.vehicle.wheelRadiusM
                val rpmM = (wWh * 60.0 / (2.0 * PI)) * nc
                // Field-weakening / torque reduction at high rpm
                val torqueDerate = 1.0 - 0.2 * max(0.0, (rpmM - 2200.0) / 1300.0)
                val tShAv = min(cfg.motor.peakTorqueNm, cfg.motor.peakTorqueNm * torqueDerate)
                val fTrac = (tShAv * nc * cfg.drivetrain.etaTrans) / cfg.vehicle.wheelRadiusM
                val fRes = (cfg.vehicle.crBase * cfg.vehicle.totalMassKg * GRAVITY) +
                        (0.5 * AIR_DENSITY * cfg.vehicle.frontalAreaM2 * cfg.vehicle.dragCoeffCd * (vAcc * vAcc))
                val aAcc = max(0.01, (fTrac - fRes) / (cfg.vehicle.totalMassKg * cfg.vehicle.rotInertiaCi))
                vAcc += aAcc * dtAcc
                tAcc += dtAcc
            }

            // Approximate cycle specific energy based on gear ratio
            val ratioFactor = nc / cfg.drivetrain.gearRatioNc
            val whKm = baseSim.specEnergyWhKm * (0.92 + 0.08 * ratioFactor.pow(1.1))

            // Peak magnet temperature estimation
            val tMagPeak = cfg.thermal.ambientTempC + 18.0 * ratioFactor.pow(1.2)

            points.add(
                OptimizationPoint(
                    gearRatioNc = nc,
                    accelTime0to60S = tAcc,
                    specEnergyWhKm = whKm,
                    estPeakMagnetTempC = tMagPeak
                )
            )
        }

        // Pareto compromise score: balance acceleration time and energy consumption
        var bestIdx = 0
        var bestScore = Double.MAX_VALUE
        val minAccel = points.minOf { it.accelTime0to60S }
        val maxAccel = max(minAccel + 0.01, points.maxOf { it.accelTime0to60S })
        val minEnergy = points.minOf { it.specEnergyWhKm }
        val maxEnergy = max(minEnergy + 0.01, points.maxOf { it.specEnergyWhKm })

        for (i in points.indices) {
            val p = points[i]
            val normAccel = (p.accelTime0to60S - minAccel) / (maxAccel - minAccel)
            val normEnergy = (p.specEnergyWhKm - minEnergy) / (maxEnergy - minEnergy)
            val score = 0.55 * normAccel + 0.45 * normEnergy
            if (score < bestScore) {
                bestScore = score
                bestIdx = i
            }
        }

        return OptimizationResult(points = points, optimalIndex = bestIdx)
    }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.EVPowertrainEngine
import com.example.engine.InverterCalculator
import com.example.model.InverterSizingInput
import com.example.model.PowertrainConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("EV Powertrain Lab", appName)
    }

    @Test
    fun `test powertrain simulation matches engineering physics`() {
        val cfg = PowertrainConfig()
        val result = EVPowertrainEngine.runSimulation(cfg)

        // WMTC 600s cycle should yield distance ~ 5.5 to 7.0 km
        assertTrue("Distance should be > 5.0 km", result.totalDistKm > 5.0)
        assertTrue("Gross traction energy should be > 0", result.posEnergyWh > 0.0)
        assertTrue("Regenerated energy should be > 0", result.regenEnergyWh > 0.0)
        assertTrue("Regen recovery share should be positive", result.regenPct > 0.0)
        assertTrue("End SOC should be between 20% and 95%", result.endSocPct in 20.0..95.0)
        assertTrue("Peak magnet temp should be positive", result.peakMagnetTempC > 0.0)
        assertEquals("Magnet status should correctly reflect temperature comparison", result.peakMagnetTempC < result.magnetTempLimitC, result.isMagnetSafe)
    }

    @Test
    fun `test inverter and power electronics sizing`() {
        val input = InverterSizingInput(
            peakPhaseCurrentArms = 131.0,
            dcBusVoltageV = 72.0,
            switchingFreqKhz = 16.0,
            allowedVoltageRipplePct = 2.0
        )
        val sizing = InverterCalculator.calculateInverterSizing(input)

        assertTrue("Required bus capacitance should be > 500 uF", sizing.requiredBusCapacitanceMicroFarad > 500.0)
        assertTrue("Recommended Vds should be >= 100V", sizing.recommendedMosfetVoltageRatingV >= 100.0)
        assertTrue("Inverter losses should be positive", sizing.totalInverterHeatLossesW > 0.0)
    }

    @Test
    fun `test top speed and gradeability calculation`() {
        val cfg = PowertrainConfig()
        val limits = InverterCalculator.calculateTopSpeedAndGradeability(cfg)

        assertTrue("Top speed should be > 50 km/h", limits.maxSpeedKmh > 50.0)
        assertTrue("Continuous gradeability should be > 5%", limits.maxContinuousGradePct > 5.0)
        assertTrue("Peak gradeability should be > continuous", limits.maxPeakGradePct >= limits.maxContinuousGradePct)
        assertTrue("Stall torque should be > 100 Nm", limits.stallWheelTorqueNm > 100.0)
    }
}

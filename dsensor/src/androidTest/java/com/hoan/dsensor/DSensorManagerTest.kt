package com.hoan.dsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.util.Log
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock

class DSensorManagerTest {
    private val appContext = InstrumentationRegistry.getInstrumentation().targetContext

    private val dSensorManager = DSensorManager(appContext)

    @Mock
    private lateinit var sensorManager: SensorManager

    @Before
    fun setUp() {
        sensorManager = mock(SensorManager::class.java)
        Mockito.`when`(appContext.getSystemService(Context.SENSOR_SERVICE)).thenReturn(sensorManager)
    }


    @Test
    fun testStartDSensor() {
        dSensorManager.startDSensor(TYPE_DEVICE_ACCELEROMETER)
        Mockito.`when`(sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)).thenReturn(null)
        val errors = dSensorManager.getErrors()
        Log.e("Test", "errors = $errors")
        assertEquals(errors.size, 1)
    }
}
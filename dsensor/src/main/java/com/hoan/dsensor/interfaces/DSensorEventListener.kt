package com.hoan.dsensor.interfaces

import com.hoan.dsensor.DProcessedSensorEvent

/**
 * Call back for SensorManager.startDSensor
 */
interface DSensorEventListener {
    fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent)
}
package com.hoan.dsensor.interfaces

import com.hoan.dsensor.DSensorEvent

/**
 * Call back for SensorManager.startDProcessedSensor
 */
interface DProcessedEventListener {
    fun onProcessedValueChanged(dSensorEvent: DSensorEvent)
}

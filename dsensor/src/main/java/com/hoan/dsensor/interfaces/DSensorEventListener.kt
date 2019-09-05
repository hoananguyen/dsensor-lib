package com.hoan.dsensor.interfaces

import androidx.collection.SparseArrayCompat
import com.hoan.dsensor.DSensorEvent

/**
 * Call back for SensorManager.startDSensor
 */
interface DSensorEventListener {
    fun onDSensorChanged(resultMap: SparseArrayCompat<DSensorEvent>)
}
package com.hoan.dsensor.interfaces

import android.util.SparseArray
import com.hoan.dsensor.DSensorEvent

/**
 * Call back for SensorManager.startDSensor
 */
interface DSensorEventListener {
    fun onDSensorChanged(changedDSensorTypes: Int, resultMap: SparseArray<DSensorEvent>)
}
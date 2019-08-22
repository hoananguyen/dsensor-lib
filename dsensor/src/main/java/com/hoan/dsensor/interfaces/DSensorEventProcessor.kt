package com.hoan.dsensor.interfaces

import com.hoan.dsensor.DSensorEvent

interface DSensorEventProcessor {
    fun finish()
    fun onDSensorChanged(dSensorEvent: DSensorEvent)
}
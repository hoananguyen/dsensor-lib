package com.hoan.dsensor

import android.hardware.SensorManager


class DSensorEvent(val sensorType: Int, valuesLength: Int) {
    var accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    var timestamp: Long = 0
    val values: FloatArray = FloatArray(valuesLength)

    constructor(sensorType: Int, accuracy: Int, timestamp: Long, values: FloatArray): this(sensorType, values.size){
        this.accuracy = accuracy
        this.timestamp = timestamp
        System.arraycopy(values, 0, this.values, 0, values.size)
    }

    constructor(dSensorEvent: DSensorEvent): this(dSensorEvent.sensorType, dSensorEvent.values.size) {
        this.accuracy = dSensorEvent.accuracy
        this.timestamp = dSensorEvent.timestamp
        System.arraycopy(dSensorEvent.values, 0, this.values, 0, values.size)
    }

    override fun toString(): String {
        val sb = StringBuilder(256)
        sb.append("Sensor type = ")
        sb.append(sensorType)
        sb.append("\naccuracy = ")
        sb.append(this.accuracy)
        sb.append("\ntimestamp = ")
        sb.append(this.timestamp)
        sb.append("\nvalues = ")
        sb.append(this.values.joinToString(prefix = "[", postfix = "]"))
        sb.append("\n")
        return sb.toString()
    }
}

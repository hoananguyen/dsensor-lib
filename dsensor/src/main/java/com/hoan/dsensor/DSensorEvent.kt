package com.hoan.dsensor

import android.hardware.SensorManager


class DSensorEvent(val sensorType: Int) {
    var accuracy: Int = SensorManager.SENSOR_STATUS_UNRELIABLE
    var timestamp: Long = 0
    lateinit var values: FloatArray

    constructor(sensorType: Int, accuracy: Int, timestamp: Long, values: FloatArray): this(sensorType){
        this.accuracy = accuracy
        this.timestamp = timestamp
        this.values = values.copyOf()
    }

    fun copyOf(): DSensorEvent {
        return DSensorEvent(sensorType, accuracy, timestamp, values.copyOf())
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

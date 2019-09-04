package com.hoan.dsensor.storage

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.hoan.dsensor.DSensorEvent

@Entity(tableName = "sensor_data_table",
    foreignKeys = arrayOf(ForeignKey(entity = Session::class, parentColumns = arrayOf("id"), childColumns = arrayOf("sessionId"), onDelete = CASCADE)))
class SensorData {
    @PrimaryKey(autoGenerate = true) val id: Long
    @ColumnInfo(name = "session_id") val sessionId: Long
    @ColumnInfo(name = "dsensor_type") val dSensorType: Int
    @ColumnInfo(name = "value_0") val value0: Float
    @ColumnInfo(name = "value_1") val value1: Float
    @ColumnInfo(name = "value_2") val value2: Float
    @ColumnInfo(name = "accuracy") val accuracy: Int
    @ColumnInfo(name = "timestamp") val timestamp: Long

    @Ignore
    constructor(id: Long, sessionId: Long, dSensorEvent: DSensorEvent) {
        this.id = id
        this.sessionId = sessionId
        this.dSensorType = dSensorEvent.sensorType
        this.value0 = dSensorEvent.values[0]
        this.value1 = dSensorEvent.values[1]
        this.value2 = dSensorEvent.values[2]
        this.accuracy = dSensorEvent.accuracy
        this.timestamp = dSensorEvent.timestamp
    }
}
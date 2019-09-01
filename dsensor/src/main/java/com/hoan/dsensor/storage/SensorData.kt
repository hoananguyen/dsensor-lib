package com.hoan.dsensor.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.hoan.dsensor.DSensorEvent

@Entity(tableName = "sensor_data_table")
class SensorData {
    @PrimaryKey(autoGenerate = true) val id: Long
    @ColumnInfo(name = "dsensor_type") val dSensorType: Int
    @ColumnInfo(name = "value_0") val value0: Float
    @ColumnInfo(name = "value_1") val value1: Float
    @ColumnInfo(name = "value_2") val value2: Float
    @ColumnInfo(name = "accuracy") val accuracy: Int
    @ColumnInfo(name = "timestamp") val timestamp: Long

    @Ignore
    constructor(id: Long, dSensorEvent: DSensorEvent) {
        this.id = id
        this.dSensorType = dSensorEvent.sensorType
        this.value0 = dSensorEvent.values[0]
        this.value1 = dSensorEvent.values[1]
        this.value2 = dSensorEvent.values[2]
        this.accuracy = dSensorEvent.accuracy
        this.timestamp = dSensorEvent.timestamp
    }
}
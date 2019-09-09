package com.hoan.dsensor.storage

import androidx.room.*
import androidx.room.ForeignKey.CASCADE
import com.hoan.dsensor.DSensorEvent

@Entity(tableName = "sensor_data_table", indices = [Index(value = ["session_id"])],
    foreignKeys = [ForeignKey(entity = Session::class, parentColumns = ["id"], childColumns = ["session_id"], onDelete = CASCADE)])
internal class SensorData(@ColumnInfo(name = "session_id") val sessionId: Long,
                          @ColumnInfo(name = "dsensor_type") val dSensorType: Int,
                          @ColumnInfo(name = "value_0") val value0: Float,
                          @ColumnInfo(name = "value_1") val value1: Float,
                          @ColumnInfo(name = "value_2") val value2: Float,
                          @ColumnInfo(name = "accuracy") val accuracy: Int,
                          @ColumnInfo(name = "timestamp") val timestamp: Long,
                          @PrimaryKey(autoGenerate = true) val id: Long = 0) {

    @Ignore
    constructor(sessionId: Long, dSensorEvent: DSensorEvent):
        this(sessionId, dSensorEvent.sensorType, dSensorEvent.values[0], dSensorEvent.values[1], dSensorEvent.values[2],
            dSensorEvent.accuracy, dSensorEvent.timestamp)

    fun getDSensorEvent(): DSensorEvent {
        return DSensorEvent(dSensorType, accuracy, timestamp, floatArrayOf(value0, value1, value2))
    }

}
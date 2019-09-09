package com.hoan.dsensor.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_table")
class Session(@PrimaryKey @ColumnInfo(name = "id") val id: Long,
                       @ColumnInfo(name = "types") val dSensorTypes: Int,
                       @ColumnInfo(name = "start_time") val timestamp: Long,
                       @ColumnInfo(name = "name") val name: String? = null,
                       @ColumnInfo(name = "has_gravity") val hasGravity: Boolean = true,
                       @ColumnInfo(name = "has_linear_acceleration") val hasLinearAcceleration: Boolean = true) {

    override fun toString(): String {
        return "id = $id, types = $dSensorTypes, timestamp = $timestamp, name = $name, " +
                "has gravity sensor = $hasGravity, has linear acceleration = $hasLinearAcceleration"
    }
}
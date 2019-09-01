package com.hoan.dsensor.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session_table")
class Session(@PrimaryKey(autoGenerate = true) val id: Long,
              @ColumnInfo(name = "types") val dSensorTypes: Int,
              @ColumnInfo(name = "start_time") val timestamp: Long,
              @ColumnInfo(name = "name") val name: String?) {
}
package com.hoan.dsensor.storage

import androidx.room.Dao
import androidx.room.Query

@Dao
internal abstract class DSensorDao : BaseDao<SensorData> {

    @Query("SELECT * from sensor_data_table WHERE session_id = :sessionId ORDER BY timestamp ASC")
    abstract fun getDataForSession(sessionId: Long): List<SensorData>
}
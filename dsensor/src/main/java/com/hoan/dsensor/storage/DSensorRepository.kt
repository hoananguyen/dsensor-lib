package com.hoan.dsensor.storage

import android.content.Context
import androidx.annotation.WorkerThread

internal class DSensorRepository(context: Context) {
    private val mSessionDao: SessionDao
    private val mDSensorDao: DSensorDao

    init {
        val db = DSensorRoomDatabase.getDatabase(context)
        mSessionDao = db.sessionDao()
        mDSensorDao = db.dSensorDao()
    }

    @WorkerThread
    internal suspend fun insert(session: Session): Long {
        return mSessionDao.insert(session)
    }

    @WorkerThread
    internal suspend fun insert(sensorData: SensorData): Long {
        return mDSensorDao.insert(sensorData)
    }

    @WorkerThread
    internal suspend fun insert(sensorDataList: List<SensorData>) {
        mDSensorDao.insert(sensorDataList)
    }

    suspend fun allSessions(): List<Session>? {
        return mSessionDao.getAllSessions()
    }

    suspend fun lastSession(): Session? {
        return mSessionDao.getLastSession()
    }

    suspend fun sensorDataForSession(sessionId: Long): List<SensorData> {
        return mDSensorDao.getDataForSession(sessionId)
    }
}
package com.hoan.dsensor.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Session::class, SensorData::class], version = 1)
internal abstract class DSensorRoomDatabase : RoomDatabase() {
    abstract fun dSensorDao() : DSensorDao
    abstract fun sessionDao() : SessionDao

    companion object {
        @Volatile
        private var INSTANCE: DSensorRoomDatabase? = null

        fun getDatabase(context: Context): DSensorRoomDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DSensorRoomDatabase::class.java,
                    "dsensor_database"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
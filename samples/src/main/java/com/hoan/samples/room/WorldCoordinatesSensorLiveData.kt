package com.hoan.samples.room

import android.app.Application
import androidx.collection.SparseArrayCompat
import com.hoan.dsensor.*
import com.hoan.dsensor.utils.logger

const val DEVICE_COORDINATES = 1
const val WORLD_COORDINATES = 3

class WorldCoordinatesSensorLiveData(application: Application, dSensorTypes: Int) : SensorLiveData(application, dSensorTypes) {
    private var mRegisteredWorldCoordinatesSensor: Pair<Int, Int>

    init {
        mRegisteredWorldCoordinatesSensor = getRegisteredSensor(dSensorTypes)
    }

    override fun onDataChanged(oldValue: SparseArrayCompat<DSensorEvent>, newValue: SparseArrayCompat<DSensorEvent>) {
        logger("WorldCoordinatesSensorLiveData", "onDSensorChanged $newValue")
        val map = SparseArrayCompat<List<String>>(2)
        synchronized(mRegisteredWorldCoordinatesSensor) {
            newValue[mRegisteredWorldCoordinatesSensor.first]?.apply {
                map.put(DEVICE_COORDINATES, values.map { it.toString() })
            }
            newValue[mRegisteredWorldCoordinatesSensor.second]?.apply {
                map.put(WORLD_COORDINATES, values.map { it.toString() })
            }
        }
        if (!map.isEmpty) {
            postValue(map)
        }
    }

    @UseExperimental(kotlinx.coroutines.ObsoleteCoroutinesApi::class)
    override fun onNewSensorSelected(newSensorTypes: Int, name: String) {
        synchronized(mRegisteredWorldCoordinatesSensor) {
            mRegisteredWorldCoordinatesSensor = getRegisteredSensor(newSensorTypes)
        }
        super.onNewSensorSelected(newSensorTypes, name)
    }

    private fun getRegisteredSensor(dSensorTypes: Int): Pair<Int, Int> {
        return when {
            dSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0-> Pair(TYPE_DEVICE_ACCELEROMETER, TYPE_WORLD_ACCELEROMETER)
            dSensorTypes and TYPE_DEVICE_GRAVITY != 0 -> Pair(TYPE_DEVICE_GRAVITY, TYPE_WORLD_GRAVITY)
            dSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0 -> Pair(TYPE_DEVICE_MAGNETIC_FIELD, TYPE_WORLD_MAGNETIC_FIELD)
            dSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 -> Pair(TYPE_DEVICE_LINEAR_ACCELERATION, TYPE_WORLD_LINEAR_ACCELERATION)
            else -> Pair(ERROR_UNSUPPORTED_TYPE, ERROR_UNSUPPORTED_TYPE)
        }
    }
}
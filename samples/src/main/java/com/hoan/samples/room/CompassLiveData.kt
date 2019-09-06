package com.hoan.samples.room

import android.app.Application
import androidx.collection.SparseArrayCompat
import com.hoan.dsensor.DSensorEvent
import com.hoan.dsensor.TYPE_DEPRECATED_ORIENTATION
import com.hoan.dsensor.TYPE_NEGATIVE_Z_AXIS_DIRECTION
import com.hoan.dsensor.getCompassSensorType
import com.hoan.dsensor.utils.convertToDegree
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R

class CompassLiveData(application: Application, dSensorTypes: Int) : SensorLiveData(application, dSensorTypes) {
    private val mDSensorList = ArrayList<Int>()

    init {
        setSensorList(dSensorTypes)
    }

    override fun onNewSensorSelected(newSensorTypes: Int, name: String) {
        setSensorList(newSensorTypes)
        super.onNewSensorSelected(newSensorTypes, name)
    }

    private fun setSensorList(dSensorTypes: Int) {
        synchronized(mDSensorList) {
            mDSensorList.add(getCompassSensorType(mApplication))

            if (dSensorTypes and TYPE_NEGATIVE_Z_AXIS_DIRECTION != 0) {
                mDSensorList.add(TYPE_NEGATIVE_Z_AXIS_DIRECTION)
            }

            if (dSensorTypes and TYPE_DEPRECATED_ORIENTATION != 0) {
                mDSensorList.add(TYPE_DEPRECATED_ORIENTATION)
            }
        }
    }

    override fun onDSensorChanged(resultMap: SparseArrayCompat<DSensorEvent>) {
        logger("CompassLiveData", "onDSensorChanged thread name = ${Thread.currentThread().name}")
        val map = SparseArrayCompat<List<String>>(2)
        synchronized(mDSensorList) {
            for (dSensorType in mDSensorList) {
                resultMap[dSensorType]?.let {
                    if (it.values[0].isFinite()) {
                        map.put(dSensorType, listOf(convertToDegree(it.values[0]).toString()))
                    } else if (resultMap[TYPE_NEGATIVE_Z_AXIS_DIRECTION] == null) {
                        map.put(ERROR, listOf(mApplication.getString(R.string.device_not_flat_no_compass_value)))
                    }
                }
            }
        }
        if (!map.isEmpty) {
            postValue(map)
        }
    }
}
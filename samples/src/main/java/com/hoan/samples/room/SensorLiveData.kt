package com.hoan.samples.room

import android.app.Application
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.MutableLiveData
import com.hoan.dsensor.*
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R

const val ERROR = -1
const val NAME = 0

abstract class SensorLiveData(application: Application, dSensorTypes: Int) : MutableLiveData<SparseArrayCompat<List<String>>>(), DSensorEventListener {
    protected val mApplication = application
    private val mDSensorManager = DSensorManager(application)
    private var mDSensorType = dSensorTypes

    override fun onInactive() {
        super.onInactive()
        logger("SensorLiveData", "onInactive")

        mDSensorManager.stopDSensor()
    }

    override fun onActive() {
        super.onActive()

        logger("SensorLiveData", "onActive")
        startSensor()
    }

    open fun onNewSensorSelected(newSensorTypes: Int, name: String) {
        logger("SensorLiveData", "onNewSensorSelected: name = $name")
        mDSensorManager.stopDSensor()
        mDSensorType = newSensorTypes
        startSensor()
        setName(name)
    }

    private fun startSensor() {
        logger("SensorLiveData", "startSensor")
        if (!mDSensorManager.startDSensor(mDSensorType, this)) {
            val map = SparseArrayCompat<List<String>>()
            map.put(ERROR, listOf(getErrorMessage(mDSensorManager.getErrors())))
            value = map
            mDSensorManager.stopDSensor()
        }
    }

    private fun setName(name: String) {
        val map = SparseArrayCompat<List<String>>()
        map.put(NAME, listOf(name))
        value = map
    }

    private fun getErrorMessage(errors: Set<Int>): String {
        logger("SensorLiveData", "getErrorMessage: $errors")
        if (errors.isEmpty()) return ""

        var errorMessage = ""
        if (errors.contains(TYPE_ACCELEROMETER_NOT_AVAILABLE)) {
            errorMessage += mApplication.getString(R.string.accelerometer) + " & "
        }

        if (errors.contains(TYPE_GRAVITY_NOT_AVAILABLE)) {
            errorMessage += mApplication.getString(R.string.gravity) + " & "
        }

        if (errors.contains(TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)) {
            errorMessage += mApplication.getString(R.string.linear_acceleration) + " & "
        }

        if (errors.contains(TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)) {
            errorMessage += mApplication.getString(R.string.magnetic_field) + " & "
        }

        errorMessage = errorMessage.substringBeforeLast(" & ")

        errorMessage = when {
            !errorMessage.contains("&") -> {
                when {
                    errors.contains(ERROR_UNSUPPORTED_TYPE) -> mApplication.getString(R.string.error_unsupported_sensor)
                    else -> mApplication.getString(R.string.error_no_sensor, errorMessage)
                }
            }
            else -> mApplication.getString(R.string.error_no_sensors, errorMessage)
        }

        return errorMessage
    }
}
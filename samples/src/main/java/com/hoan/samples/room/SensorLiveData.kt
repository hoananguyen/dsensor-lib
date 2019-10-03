package com.hoan.samples.room

import android.app.Application
import androidx.collection.SparseArrayCompat
import androidx.lifecycle.MutableLiveData
import com.hoan.dsensor.*
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R

const val ERROR = -1
const val NAME = 0

abstract class SensorLiveData(application: Application, dSensorTypes: Int) : MutableLiveData<SparseArrayCompat<List<String>>>() {
    protected val mApplication = application
    private val mDSensorManager = DSensorManager(application)
    private var mDSensorType = dSensorTypes
    private lateinit var mDSensorData: DSensorData

    protected abstract fun onDataChanged(oldValue: SparseArrayCompat<DSensorEvent>, newValue: SparseArrayCompat<DSensorEvent>)

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    override fun onInactive() {
        super.onInactive()
        logger("SensorLiveData", "onInactive")

        mDSensorManager.stopDSensor()
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    override fun onActive() {
        super.onActive()

        logger("SensorLiveData", "onActive")
        startSensor()
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    open fun onNewSensorSelected(newSensorTypes: Int, name: String) {
        logger("SensorLiveData", "onNewSensorSelected: name = $name")
        mDSensorManager.stopDSensor()
        mDSensorType = newSensorTypes
        startSensor()
        setName(name)
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    private fun startSensor() {
        logger("SensorLiveData", "startSensor")
        val sensorData = mDSensorManager.startDSensor(mDSensorType, NOT_SAVE, "trial")
        if (sensorData == null) {
            val map = SparseArrayCompat<List<String>>()
            map.put(ERROR, listOf(getErrorMessage(mDSensorManager.getErrors())))
            value = map
            mDSensorManager.stopDSensor()
        } else {
            mDSensorData = sensorData
            mDSensorData.onDataChanged = { oldValue, newValue -> onDataChanged(oldValue, newValue) }
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
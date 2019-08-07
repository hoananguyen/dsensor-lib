package com.hoan.samples


import android.support.v4.app.Fragment
import com.hoan.dsensor.*
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.logger

/**
 * A simple [Fragment] subclass.
 *
 */
abstract class BaseSensorFragment : Fragment(), DSensorEventListener {

    protected var mSensorType: Int = 0

    private var mDSensorManager: DSensorManager? = null

    open fun onSensorChanged(newSensorType: Int) {
        logger(BaseSensorFragment::class.java.simpleName, "onSensorChanged")
        if (newSensorType == mSensorType) return

        stopSensor()
        mSensorType = newSensorType

        startSensor()
    }

    fun stopSensor() {
        logger(BaseSensorFragment::class.java.simpleName, "stopSensor")
        mDSensorManager?.stopDSensor()
        mDSensorManager = null
    }

    fun startSensor() {
        logger(BaseSensorFragment::class.java.simpleName, "startSensor")
        mDSensorManager = DSensorManager(context!!)
        if (!mDSensorManager!!.startDSensor(mSensorType, this)) {
            showError(getErrorMessage(mDSensorManager?.getErrors() ?: HashSet()))
            stopSensor()
        }
    }

    abstract fun showError(errorMessage: String?)

    private fun getErrorMessage(errors: Set<Int>): String {
        if (errors.isEmpty()) return ""

        var errorMessage = ""
        if (errors.contains(TYPE_ACCELEROMETER_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.accelerometer).toLowerCase() + " & "
        }

        if (errors.contains(TYPE_GRAVITY_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.gravity).toLowerCase() + " & "
        }

        if (errors.contains(TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.linear_acceleration).toLowerCase() + " & "
        }

        if (errors.contains(TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.magnetic_field).toLowerCase() + " & "
        }

        errorMessage = errorMessage.substringBeforeLast(" & ")

        errorMessage = when {
            !errorMessage.contains("&") -> getString(R.string.error_no_sensor, errorMessage)
            else -> getString(R.string.error_no_sensors, errorMessage)
        }

        return errorMessage
    }
}

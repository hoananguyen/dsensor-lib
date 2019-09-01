package com.hoan.samples


import androidx.fragment.app.Fragment
import com.hoan.dsensor.*
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.logger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel


abstract class BaseSensorFragment : Fragment(), DSensorEventListener {
    protected val mCoroutineScope = MainScope()

    protected var mSensorType: Int = 0

    private var mDSensorManager: DSensorManager? = null

    open fun onSensorChanged(newSensorType: Int) {
        logger(BaseSensorFragment::class.java.simpleName, "onSensorChanged")
        if (newSensorType != mSensorType) {
            stopSensor()
            mSensorType = newSensorType
            startSensor()
        }
    }

    fun stopSensor() {
        logger(BaseSensorFragment::class.java.simpleName, "stopSensor")
        mDSensorManager?.apply { stopDSensor() }
        mDSensorManager = null
    }

    fun startSensor() {
        logger(BaseSensorFragment::class.java.simpleName, "startSensor")
        context?.let {
            mDSensorManager = DSensorManager(it)
        }

        mDSensorManager?.let {
            if (!it.startDSensor(mSensorType, this)) {
                showError(getErrorMessage(it.getErrors()))
                stopSensor()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mCoroutineScope.cancel()
    }

    abstract fun showError(errorMessage: String?)

    private fun getErrorMessage(errors: Set<Int>): String {
        logger(BaseSensorFragment::class.java.simpleName, "getErrorMessage: ${errors}")
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
            !errorMessage.contains("&") -> {
                when {
                    errors.contains(ERROR_UNSUPPORTED_TYPE) -> getString(R.string.error_unsupported_sensor)
                    else -> getString(R.string.error_no_sensor, errorMessage)
                }
            }
            else -> getString(R.string.error_no_sensors, errorMessage)
        }

        return errorMessage
    }
}

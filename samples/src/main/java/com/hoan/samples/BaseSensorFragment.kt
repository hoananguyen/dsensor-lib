package com.hoan.samples


import android.os.Bundle
import android.support.v4.app.Fragment
import com.hoan.dsensor.*
import com.hoan.dsensor.interfaces.DProcessedEventListener
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_compass.*
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.*

//const val COMPASS_TYPE = "compass type"
const val SENSOR_TYPE = "sensor type"
/**
 * A simple [Fragment] subclass.
 *
 */
abstract class BaseSensorFragment : Fragment(), DSensorEventListener {

    //protected var mCompassType: String? = null
    protected var mSensorType: Int? = null

    private var mDSensorManager: DSensorManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //mCompassType = arguments?.getString(COMPASS_TYPE)
        mSensorType = arguments?.getInt(SENSOR_TYPE)
    }

    /*open fun onSensorChanged(newSensorType: String) {
        if (newSensorType == mCompassType) return

        stopSensor()
        mCompassType = newSensorType

        //startDProcessedSensor()
    }*/

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

    /*fun startDProcessedSensor() {
        mDSensorManager = DSensorManager(context!!)
        if (!mDSensorManager!!.startDProcessedSensor(context!!, getDProcessedSensorType(mCompassType!!), this)) {
            val errors = mDSensorManager?.getErrors() ?: HashSet()
            if (errors.contains(TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)) {
                textview_compass_value.setText(R.string.error_no_magnetic_field_sensor)
            } else if (errors.contains(TYPE_GRAVITY_NOT_AVAILABLE)) {
                textview_compass_value.setText(R.string.error_no_accelerometer_sensor)
            }
            stopSensor()
        }
    }*/

    fun startSensor() {
        logger(BaseSensorFragment::class.java.simpleName, "startSensor")
        mDSensorManager = DSensorManager(context!!)
        if (!mDSensorManager!!.startDSensor(mSensorType!!, this)) {
            textview_error.text = getErrorMessage(mDSensorManager?.getErrors() ?: HashSet())
            stopSensor()
        }
    }

    private fun getErrorMessage(errors: Set<Int>): String {
        if (errors.isEmpty()) return ""

        var errorMessage = ""
        if (errors.contains(TYPE_ACCELEROMETER_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.accelerometer).toLowerCase() + " and "
        }

        if (errors.contains(TYPE_GRAVITY_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.gravity).toLowerCase() + " and "
        }

        if (errors.contains(TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.linear_acceleration).toLowerCase() + " and "
        }

        if (errors.contains(TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)) {
            errorMessage += getString(R.string.magnetic_field).toLowerCase() + " and "
        }

        errorMessage = errorMessage.substringBeforeLast(" and ")

        errorMessage = when {
            errors.size == 1 -> getString(R.string.error_no_sensor, errorMessage)
            else -> getString(R.string.error_no_sensors, errorMessage)
        }

        return errorMessage
    }

    /*private fun getDProcessedSensorType(compassType: String) =  when (compassType) {
        COMPASS -> DProcessedSensor.TYPE_COMPASS
        COMPASS_3D -> DProcessedSensor.TYPE_3D_COMPASS
        COMPASS_AND_DEPRECIATED_ORIENTATION -> DProcessedSensor.TYPE_COMPASS_AND_DEPRECIATED_ORIENTATION
        else -> DProcessedSensor.TYPE_3D_COMPASS_AND_DEPRECIATED_ORIENTATION
    }*/
}

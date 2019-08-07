package com.hoan.samples


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.DProcessedSensorEvent
import com.hoan.dsensor.DSensor.TYPE_DEVICE_ACCELEROMETER
import com.hoan.dsensor.DSensor.TYPE_DEVICE_GRAVITY
import com.hoan.dsensor.DSensor.TYPE_DEVICE_LINEAR_ACCELERATION
import com.hoan.dsensor.DSensor.TYPE_DEVICE_MAGNETIC_FIELD
import com.hoan.dsensor.DSensor.TYPE_WORLD_ACCELEROMETER
import com.hoan.dsensor.DSensor.TYPE_WORLD_GRAVITY
import com.hoan.dsensor.DSensor.TYPE_WORLD_LINEAR_ACCELERATION
import com.hoan.dsensor.DSensor.TYPE_WORLD_MAGNETIC_FIELD
import com.hoan.dsensor.DSensorEvent
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.*
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.view.*

private const val DEVICE_COORDINATES = "device coordinates"
private const val WORLD_COORDINATES = "world coordinates"


class FragmentSensorInWorldCoord : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(sensorType: Int) =
            FragmentSensorInWorldCoord().apply {
                mSensorType = sensorType
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_sensor_in_world_basis, container, false)
        val sensorName = getSensorName()
        if (sensorName != null) {
            v.textview_sensor.text = sensorName
        } else {
            v.textview_error.text = getString(R.string.error_unsupported_sensor)
        }

        return v
    }

    override fun onPause() {
        stopSensor()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        startSensor()
    }

    override fun onSensorChanged(newSensorType: Int) {
        super.onSensorChanged(newSensorType)

        textview_sensor.text = getSensorName()
    }

    override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
        val dSensorEventHashMap: HashMap<String, DSensorEvent?> = getDSensorEvent(processedSensorEvent)
        textview_sensor_x_value.text = dSensorEventHashMap[DEVICE_COORDINATES]?.values?.get(0).toString()
        textview_sensor_y_value.text = dSensorEventHashMap[DEVICE_COORDINATES]?.values?.get(1).toString()
        textview_sensor_z_value.text = dSensorEventHashMap[DEVICE_COORDINATES]?.values?.get(2).toString()
        textview_sensor_in_world_coord_x_value.text = dSensorEventHashMap[WORLD_COORDINATES]?.values?.get(0).toString()
        textview_sensor_in_world_coord_y_value.text = dSensorEventHashMap[WORLD_COORDINATES]?.values?.get(1).toString()
        textview_sensor_in_world_coord_z_value.text = dSensorEventHashMap[WORLD_COORDINATES]?.values?.get(2).toString()
    }

    private fun getSensorName(): String? {
        return when (mSensorType) {
            TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER -> getString(R.string.accelerometer)
            TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY -> getString(R.string.gravity)
            TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION -> getString(R.string.linear_acceleration)
            TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD -> getString(R.string.magnetic_field)
            else -> null
        }
    }

    private fun getDSensorEvent(dProcessedSensorEvent: DProcessedSensorEvent): HashMap<String, DSensorEvent?> {
        val resultHashMap: HashMap<String, DSensorEvent?> = HashMap()
        when (mSensorType) {
            TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.accelerometerInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.accelerometerInWorldBasis
            }
            TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.gravityInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.gravityInWorldBasis
            }
            TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.linearAccelerationInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.linearAccelerationInWorldBasis
            }
            TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.magneticFieldInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.magneticFieldInWorldBasis
            }
            else -> return resultHashMap
        }

        return resultHashMap
    }

    override fun showError(errorMessage: String?) {
        textview_error.text = errorMessage
    }
}

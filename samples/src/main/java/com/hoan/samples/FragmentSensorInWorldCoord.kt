package com.hoan.samples


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.DProcessedSensorEvent
import com.hoan.dsensor.DSensor
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
        v.textview_sensor.text = getSensorName() ?: getString(R.string.error_unsupported_sensor)

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

        textview_sensor.text = getSensorName() ?: getString(R.string.error_unsupported_sensor)
    }

    override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
        val dSensorEventHashMap: HashMap<String, DSensorEvent?> = getDSensorEvent(processedSensorEvent)

        dSensorEventHashMap[DEVICE_COORDINATES]?.apply {
            textview_sensor_x_value.text = values[0].toString()
            textview_sensor_y_value.text = values[1].toString()
            textview_sensor_z_value.text = values[2].toString()
        }

        dSensorEventHashMap[WORLD_COORDINATES]?.apply {
            textview_sensor_in_world_coord_x_value.text = values[0].toString()
            textview_sensor_in_world_coord_y_value.text = values[1].toString()
            textview_sensor_in_world_coord_z_value.text = values[2].toString()
        }
    }

    private fun getSensorName(): String? {
        return when (mSensorType) {
            DSensor.TYPE_DEVICE_ACCELEROMETER or DSensor.TYPE_WORLD_ACCELEROMETER -> getString(R.string.accelerometer)
            DSensor.TYPE_DEVICE_GRAVITY or DSensor.TYPE_WORLD_GRAVITY -> getString(R.string.gravity)
            DSensor.TYPE_DEVICE_LINEAR_ACCELERATION or DSensor.TYPE_WORLD_LINEAR_ACCELERATION -> getString(R.string.linear_acceleration)
            DSensor.TYPE_DEVICE_MAGNETIC_FIELD or DSensor.TYPE_WORLD_MAGNETIC_FIELD -> getString(R.string.magnetic_field)
            else -> null
        }
    }

    private fun getDSensorEvent(dProcessedSensorEvent: DProcessedSensorEvent): HashMap<String, DSensorEvent?> {
        val resultHashMap: HashMap<String, DSensorEvent?> = HashMap()
        when (mSensorType) {
            DSensor.TYPE_DEVICE_ACCELEROMETER or DSensor.TYPE_WORLD_ACCELEROMETER -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.accelerometerInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.accelerometerInWorldBasis
            }
            DSensor.TYPE_DEVICE_GRAVITY or DSensor.TYPE_WORLD_GRAVITY -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.gravityInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.gravityInWorldBasis
            }
            DSensor.TYPE_DEVICE_LINEAR_ACCELERATION or DSensor.TYPE_WORLD_LINEAR_ACCELERATION -> {
                resultHashMap[DEVICE_COORDINATES] = dProcessedSensorEvent.linearAccelerationInDeviceBasis
                resultHashMap[WORLD_COORDINATES] = dProcessedSensorEvent.linearAccelerationInWorldBasis
            }
            DSensor.TYPE_DEVICE_MAGNETIC_FIELD or DSensor.TYPE_WORLD_MAGNETIC_FIELD -> {
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

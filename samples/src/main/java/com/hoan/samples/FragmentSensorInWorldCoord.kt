package com.hoan.samples


import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.*
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.*
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.view.*
import kotlinx.coroutines.launch


class FragmentSensorInWorldCoord : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(sensorType: Int) =
            FragmentSensorInWorldCoord().apply {
                mSensorType = sensorType
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_in_world_basis, container, false)
        v.textview_sensor.text = getSensorName() ?: getString(R.string.error_unsupported_sensor)

        return v
    }

    override fun onPause() {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onPause")
        stopSensor()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onResume")

        startSensor()
    }

    override fun onSensorChanged(newSensorType: Int) {
        super.onSensorChanged(newSensorType)
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onSensorChanged")

        textview_sensor.text = getSensorName() ?: getString(R.string.error_unsupported_sensor)
    }

    override fun onDSensorChanged(changedDSensorTypes: Int, resultMap: SparseArray<DSensorEvent>) {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onDSensorChanged: thread = ${Thread.currentThread()}")
        mCoroutineScope.launch {

            logger("FragmentSensorInWorldCoord", "onDSensorChanged: timestamp = ${resultMap[0].timestamp}, thread = ${Thread.currentThread()}")
            textview_sensor_x_value.text = resultMap[0].values[0].toString()
            textview_sensor_y_value.text = resultMap[0].values[1].toString()
            textview_sensor_z_value.text = resultMap[0].values[2].toString()

            textview_sensor_in_world_coord_x_value.text = resultMap[1].values[0].toString()
            textview_sensor_in_world_coord_y_value.text = resultMap[1].values[1].toString()
            textview_sensor_in_world_coord_z_value.text = resultMap[1].values[2].toString()
        }
    }

    private fun getSensorName(): String? {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "getSensorName: mSensorType = $mSensorType")
        return when (mSensorType) {
            TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER -> getString(R.string.accelerometer)
            TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY -> getString(R.string.gravity)
            TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION -> getString(R.string.linear_acceleration)
            TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD -> getString(R.string.magnetic_field)
            else -> null
        }
    }

    override fun showError(errorMessage: String?) {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "showError: $errorMessage")
        textview_error.text = errorMessage
    }
}

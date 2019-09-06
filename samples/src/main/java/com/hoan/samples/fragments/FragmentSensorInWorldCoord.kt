package com.hoan.samples.fragments


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R
import com.hoan.samples.room.*
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.*
import kotlinx.android.synthetic.main.fragment_sensor_in_world_basis.view.*


class FragmentSensorInWorldCoord : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(sensorType: Int, name: String, group: Int) =
            FragmentSensorInWorldCoord().apply {
                mSensorType = sensorType
                mName = name
                mGroup = group
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_in_world_basis, container, false)
        v.textview_sensor.text = mName
        setupViewModel()

        return v
    }

    private fun setupViewModel() {
        mSensorViewModel = ViewModelProvider(this, SensorViewModelProviderFactory(activity!!.application, mSensorType, mGroup))
            .get(SensorViewModel::class.java)
        mSensorViewModel.sensorData?.observe(this, Observer {
            logger(FragmentSensorInWorldCoord::class.java.simpleName, "Observer $it")
            when {
                it[ERROR] != null -> showError(it[ERROR]!![0])
                it[NAME] != null -> setTitle(it[NAME]!![0])
                else -> {
                    it[DEVICE_COORDINATES]?.apply {
                        setDeviceCoordinatesTextviewsText(this)
                    }
                    it[WORLD_COORDINATES]?.apply {
                        setWorldCoordinatesTextviewsText(this)
                    }
                }
            }
        })
    }

    override fun showError(errorMessage: String?) {
        logger(FragmentSensorInWorldCoord::class.java.simpleName, "showError: $errorMessage")
        textview_error.text = errorMessage
    }

    private fun setTitle(title: String) {
        textview_sensor.text = title
    }

    private fun setDeviceCoordinatesTextviewsText(values: List<String>) {
        textview_sensor_x_value.text = values[0]
        textview_sensor_y_value.text = values[1]
        textview_sensor_z_value.text = values[2]
    }

    private fun setWorldCoordinatesTextviewsText(values: List<String>) {
        textview_sensor_in_world_coord_x_value.text = values[0]
        textview_sensor_in_world_coord_y_value.text = values[1]
        textview_sensor_in_world_coord_z_value.text = values[2]
    }
}

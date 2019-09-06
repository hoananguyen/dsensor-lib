package com.hoan.samples.room

import android.app.Application
import androidx.lifecycle.ViewModel
import com.hoan.samples.R


class SensorViewModel(application: Application, dSensorTypes: Int, group: Int) : ViewModel() {
    val sensorData = when (group) {
        R.string.sensor_in_world_coord -> WorldCoordinatesSensorLiveData(application, dSensorTypes)
        R.string.compass -> CompassLiveData(application, dSensorTypes)
        else -> null
    }
}
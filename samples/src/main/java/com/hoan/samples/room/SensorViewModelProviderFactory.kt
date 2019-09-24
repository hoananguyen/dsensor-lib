package com.hoan.samples.room

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.hoan.dsensor.utils.logger

class SensorViewModelProviderFactory(private val application: Application, private val dSensorTypes: Int, private val group: Int) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        logger("SensorViewModelProviderFactory", "create")
        return modelClass.getConstructor(Application::class.java, Int::class.java, Int::class.java).newInstance(application, dSensorTypes, group)
    }

}
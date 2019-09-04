package com.hoan.samples


import androidx.fragment.app.Fragment
import com.hoan.dsensor.utils.logger


abstract class BaseSensorFragment : Fragment() {
    protected lateinit var mSensorViewModel: SensorViewModel

    protected var mSensorType: Int = 0

    protected var mGroup: Int = 0

    protected lateinit var mName: String

    fun onNewSensorSelected(newSensorType: Int, name: String) {
        logger(BaseSensorFragment::class.java.simpleName, "onNewSensorSelected")
        if (newSensorType != mSensorType) {
            mSensorType = newSensorType
            mName = name
            if (::mSensorViewModel.isInitialized) {
                mSensorViewModel.sensorData?.onNewSensorSelected(newSensorType, name)
            }
        }
    }

    abstract fun showError(errorMessage: String?)
}

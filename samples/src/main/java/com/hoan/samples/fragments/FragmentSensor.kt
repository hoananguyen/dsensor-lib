package com.hoan.samples.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.samples.R
import kotlinx.android.synthetic.main.fragment_sensor.*
import kotlinx.android.synthetic.main.fragment_sensor.view.*

class FragmentSensor : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(dSensorType: Int, childName: String, group: Int) =
            FragmentSensor().apply {
                mSensorType = dSensorType
                mName = childName
                mGroup = group
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_compass, container, false)
        v.textview_sensor.text = mName

        return v
    }

    override fun showError(errorMessage: String?) {
        errorMessage?.let {
            textview_error.text = it
        }
    }
}
package com.hoan.samples


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.hoan.dsensor.TYPE_DEPRECATED_ORIENTATION
import com.hoan.dsensor.TYPE_NEGATIVE_Z_AXIS_DIRECTION
import com.hoan.dsensor.getCompassSensorType
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_compass.*
import kotlinx.android.synthetic.main.fragment_compass.view.*


class FragmentCompass : BaseSensorFragment() {
    private var mCompassDirectionAxis: Int = 0

    companion object {
        @JvmStatic
        fun newInstance(dSensorType: Int, childName: String, group: Int) =
            FragmentCompass().apply {
                mSensorType = dSensorType
                mName = childName
                mGroup = group
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_compass, container, false)
        v.textview_title.text = mName
        setupViewModel()
        setCompassDirectionAxis()

        return v
    }

    private fun setupViewModel() {
        mSensorViewModel = ViewModelProvider(this,
            SensorViewModelProviderFactory(activity!!.application, mSensorType, mGroup)).get(SensorViewModel::class.java)
        mSensorViewModel.sensorData?.observe(this, Observer {
            logger("FragmentCompass", "Observer $it")
            when {
                it[ERROR] != null -> showError(it[ERROR]!![0])
                it[NAME] != null -> {
                    setTitle(it[NAME]!![0])
                    setOrientationViewsVisibility()
                    setCompassDirectionAxis()
                }
                else -> {
                    when {
                        it[TYPE_NEGATIVE_Z_AXIS_DIRECTION] != null ->
                            textview_compass_value.text = it[TYPE_NEGATIVE_Z_AXIS_DIRECTION]!![0]
                        it[TYPE_DEPRECATED_ORIENTATION] != null ->
                            textview_orientation_value.text = it[TYPE_DEPRECATED_ORIENTATION]!![0]
                        else ->
                            textview_compass_value.text = it[mCompassDirectionAxis]!![0]
                    }
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOrientationViewsVisibility()
    }

    private fun setTitle(title: String) {
        textview_title.text = title
    }

    private fun setCompassDirectionAxis() {
        mCompassDirectionAxis = getCompassSensorType(context!!)
    }

    override fun showError(errorMessage: String?) {
        textview_error.text = errorMessage
    }

    private fun setOrientationViewsVisibility() {
        if ((mSensorType and TYPE_DEPRECATED_ORIENTATION) != 0) {
            if (textview_orientation.visibility != View.VISIBLE) {
                textview_orientation.visibility = View.VISIBLE
                textview_orientation_value.visibility = View.VISIBLE
            }
        } else if (textview_orientation.visibility == View.VISIBLE) {
            textview_orientation.visibility = View.GONE
            textview_orientation_value.visibility = View.GONE
        }
    }
}

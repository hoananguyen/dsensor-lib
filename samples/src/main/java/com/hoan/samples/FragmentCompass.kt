package com.hoan.samples


import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.*
import kotlinx.android.synthetic.main.fragment_compass.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


class FragmentCompass : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(dSensorType: Int) =
            FragmentCompass().apply {
                mSensorType = dSensorType
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_compass, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOrientationViewsVisibility()
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

        if (isAdded) {
            setOrientationViewsVisibility()
        }
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

    override fun onDSensorChanged(changedDSensorTypes: Int, resultMap: SparseArray<DSensorEvent>) {
        //logger("FragmentCompass", "onDSensorChanged: changedSensorTypes = ${changedDSensorTypes}")
        mCoroutineScope.launch {
            when {
                (TYPE_DEPRECATED_ORIENTATION and changedDSensorTypes) != 0 -> {
                    val orientation = resultMap[TYPE_DEPRECATED_ORIENTATION]?.values!![0]
                    textview_orientation_value.text = convertToDegree(orientation).toString()
                }
                else -> {
                    resultMap[TYPE_NEGATIVE_Z_AXIS_DIRECTION]?.apply {
                        if (values[0].isFinite()) {
                            textview_compass_value.text = convertToDegree(values[0]).toString()
                            return@launch
                        }
                    }
                    getXOrYDSensorEvent(resultMap)?.apply {
                        if (values[0].isFinite()) {
                            textview_compass_value.text = convertToDegree(values[0]).toString()
                        } else if (resultMap[TYPE_NEGATIVE_Z_AXIS_DIRECTION] == null) {
                            textview_compass_value.setText(R.string.device_not_flat_no_compass_value)
                        }
                    }
                }
            }
        }
    }

    private fun getXOrYDSensorEvent(resultMap: SparseArray<DSensorEvent>): DSensorEvent? {
        return resultMap[TYPE_X_AXIS_DIRECTION] ?: resultMap[TYPE_Y_AXIS_DIRECTION] ?: resultMap[TYPE_NEGATIVE_X_AXIS_DIRECTION] ?:
                resultMap[TYPE_NEGATIVE_Y_AXIS_DIRECTION]
    }

    private fun convertToDegree(valueInRadian: Float): Int {
        var convertValue =  Math.toDegrees(valueInRadian.toDouble()).roundToInt()
        if (convertValue < 0) {
            convertValue = (convertValue + 360) % 360
        }

        return convertValue
    }

    override fun showError(errorMessage: String?) {
        textview_compass_value.text = errorMessage
    }
}

package com.hoan.samples


import android.os.Bundle
import android.util.Log
import android.view.*
import com.hoan.dsensor.*
import kotlinx.android.synthetic.main.fragment_compass.*
import kotlin.math.round
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
        Log.e("FragmentCompass", "onPause")
        stopSensor()

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        Log.e("FragmentCompass", "onResume")
        startSensor()
    }

    override fun onSensorChanged(newSensorType: Int) {
        super.onSensorChanged(newSensorType)

        setOrientationViewsVisibility()
    }

    private fun setOrientationViewsVisibility() {
        if ((mSensorType and DSensor.TYPE_DEPRECATED_ORIENTATION) != 0) {
            if (textview_orientation.visibility != View.VISIBLE) {
                textview_orientation.visibility = View.VISIBLE
                textview_orientation_value.visibility = View.VISIBLE
            }
        } else if (textview_orientation.visibility == View.VISIBLE) {
            textview_orientation.visibility = View.GONE
            textview_orientation_value.visibility = View.GONE
        }
    }

    override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
        when {
            (DSensor.TYPE_DEPRECATED_ORIENTATION and changedDSensorTypes) != 0  -> {
                val orientation = processedSensorEvent.depreciatedOrientation?.values?.get(0)
                if (orientation != null) {
                    textview_orientation_value.text = round(orientation).toString()
                }
            }
            else -> {
                processedSensorEvent.minusZAxisDirection?.let {
                    if (it.values[0].isFinite()) {
                        var valueInDegree = Math.toDegrees(it.values[0].toDouble()).roundToInt()
                        if (valueInDegree < 0) {
                            valueInDegree = (valueInDegree + 360) % 360
                        }
                        textview_compass_value.text = valueInDegree.toString()
                        return
                    }
                }
                getDSensorEvent(processedSensorEvent)?.let {
                    if (it.values[0].isFinite()) {
                        textview_compass_value.text = it.values[0].toString()
                    } else if (processedSensorEvent.minusZAxisDirection == null) {
                        textview_compass_value.setText(R.string.device_not_flat_no_compass_value)
                    }
                }
            }
        }
    }

    private fun getDSensorEvent(dProcessedSensorEvent: DProcessedSensorEvent): DSensorEvent? {
        return when {
            (mSensorType or DSensor.TYPE_X_AXIS_DIRECTION) != 0 -> dProcessedSensorEvent.xAxisDirection
            (mSensorType or DSensor.TYPE_MINUS_X_AXIS_DIRECTION) != 0 -> dProcessedSensorEvent.minusXAxisDirection
            (mSensorType or DSensor.TYPE_Y_AXIS_DIRECTION) != 0 -> dProcessedSensorEvent.yAxisDirection
            (mSensorType or DSensor.TYPE_MINUS_Y_AXIS_DIRECTION) != 0 -> dProcessedSensorEvent.minusYAxisDirection
            else -> null
        }
    }

    override fun showError(errorMessage: String?) {
        textview_compass_value.text = errorMessage
    }
}

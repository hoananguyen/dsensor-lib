package com.hoan.samples


import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.*
import kotlinx.android.synthetic.main.fragment_compass.*
import kotlinx.android.synthetic.main.fragment_compass.view.*
import kotlinx.android.synthetic.main.fragment_compass.view.textview_orientation_value
import kotlin.math.round
import kotlin.math.roundToInt

private const val DEPRECATED = "depreciated"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentCompass.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class FragmentCompass : BaseSensorFragment() {

    companion object {
        @JvmStatic
        fun newInstance(sensorType: Int) =
            FragmentCompass().apply {
                arguments = Bundle().apply {
                    //putString(COMPASS_TYPE, compassType)
                }
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v = inflater.inflate(R.layout.fragment_compass, container, false)
        /*if (COMPASS_3D_AND_DEPRECIATED_ORIENTATION == mCompassType || COMPASS_AND_DEPRECIATED_ORIENTATION == mCompassType) {
            v.textview_orientation.visibility = View.VISIBLE
            v.textview_orientation_value.visibility = View.VISIBLE
        }*/
        return v
    }

    override fun onPause() {
        Log.e("FragmentCompass", "onPause")
        stopSensor()

        super.onPause()
    }

    override fun onResume() {
        super.onResume()

        Log.e("FragmentCompass", "onResume")
        //startDProcessedSensor()
    }

    /*override fun onSensorChanged(newSensorType: String) {
        setOrientationViewsVisibility(newSensorType)
        super.onSensorChanged(newSensorType)
    }*/

    private fun setOrientationViewsVisibility(newSensorType: String) {
        if (newSensorType.contains(DEPRECATED)) {
            if (textview_orientation.visibility != View.VISIBLE) {
                textview_orientation.visibility = View.VISIBLE
                textview_orientation_value.visibility = View.VISIBLE
            }
        } else if (textview_orientation.visibility == View.VISIBLE) {
            textview_orientation.visibility = View.GONE
            textview_orientation_value.visibility = View.GONE
        }
    }

    /*override fun onProcessedValueChanged(dSensorEvent: DSensorEvent) {
        //Log.e("FragmentCompass", "onProcessedValueChanged: type = ${dSensorEvent.sensorType} , value = ${dSensorEvent.values[0]}")
        if (dSensorEvent.sensorType == DSensor.TYPE_DEPRECIATED_ORIENTATION) {
            textview_orientation_value.text = round(dSensorEvent.values[0]).toString()
        } else {
            if (dSensorEvent.values[0].isNaN()) {
                textview_compass_value.setText(R.string.device_not_flat_no_compass_value)
            } else {
                var valueInDegree = Math.toDegrees(dSensorEvent.values[0].toDouble()).roundToInt()
                if (valueInDegree < 0) {
                    valueInDegree = (valueInDegree + 360) % 360
                }
                textview_compass_value.text = valueInDegree.toString()
            }
        }
    }*/
    override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {

    }
}

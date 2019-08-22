package com.hoan.samples


import android.hardware.Sensor
import android.os.Build
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.text.style.TextAppearanceSpan
import android.util.SparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.hoan.dsensor.DSensorManager
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_sensor_info.view.*


/**
 * Show all sensors in a device.
 */
class FragmentSensorInfo : Fragment() {

    private val sensorTypes = SparseArray<String>()

    init {
        sensorTypes.put(Sensor.TYPE_ACCELEROMETER, "TYPE_ACCELEROMETER")
        sensorTypes.put(Sensor.TYPE_GYROSCOPE, "TYPE_GYROSCOPE")
        sensorTypes.put(Sensor.TYPE_LIGHT, "TYPE_LIGHT")
        sensorTypes.put(Sensor.TYPE_MAGNETIC_FIELD, "TYPE_MAGNETIC_FIELD")
        @Suppress("DEPRECATION")
        sensorTypes.put(Sensor.TYPE_ORIENTATION, "TYPE_ORIENTATION")
        sensorTypes.put(Sensor.TYPE_PRESSURE, "TYPE_PRESSURE")
        sensorTypes.put(Sensor.TYPE_PROXIMITY, "TYPE_PROXIMITY")
        @Suppress("DEPRECATION")
        sensorTypes.put(Sensor.TYPE_TEMPERATURE, "TYPE_TEMPERATURE")
        sensorTypes.put(Sensor.TYPE_GRAVITY, "TYPE_GRAVITY")
        sensorTypes.put(Sensor.TYPE_LINEAR_ACCELERATION, "TYPE_LINEAR_ACCELERATION")
        sensorTypes.put(Sensor.TYPE_ROTATION_VECTOR, "TYPE_ROTATION_VECTOR")
        sensorTypes.put(Sensor.TYPE_AMBIENT_TEMPERATURE, "TYPE_AMBIENT_TEMPERATURE")
        sensorTypes.put(Sensor.TYPE_RELATIVE_HUMIDITY, "TYPE RELATIVE HUMIDITY")

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
            sensorTypes.put(Sensor.TYPE_GAME_ROTATION_VECTOR, "TYPE GAME ROTATION VECTOR")
            sensorTypes.put(Sensor.TYPE_GYROSCOPE_UNCALIBRATED, "TYPE GYROSCOPE UNCALIBRATED")
            sensorTypes.put(Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED, "TYPE MAGNETIC FIELD UNCALIBRATED")
            sensorTypes.put(Sensor.TYPE_SIGNIFICANT_MOTION, "TYPE SIGNIFICANT MOTION")

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                sensorTypes.put(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR, "TYPE GEOMAGNETIC ROTATION VECTOR")
                sensorTypes.put(Sensor.TYPE_STEP_COUNTER, "TYPE STEP COUNTER")
                sensorTypes.put(Sensor.TYPE_STEP_DETECTOR, "TYPE STEP DETECTOR")

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                    sensorTypes.put(Sensor.TYPE_HEART_RATE, "TYPE HEART RATE")
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorInfo::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_info, container, false)
        v.textview_sensors_info.text = getSensorInfo()

        return v
    }

    private fun getSensorInfo(): SpannableStringBuilder {
        logger(FragmentSensorInfo::class.java.simpleName, "getSensorInfo")
        val builder = SpannableStringBuilder()

        val sensors = DSensorManager(context!!).listSensor()

        builder.append("The sensors on this device are\n\n")
        builder.setSpan(
            TextAppearanceSpan(activity, android.R.style.TextAppearance_Medium),
            0, getString(R.string.sensor_info_start).length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        var start: Int
        var type: String?
        for (sensor in sensors) {
            start = builder.length
            builder.append("Name: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Name: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.name)
            start = builder.length
            builder.append("\nType: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Type: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            type = sensorTypes.get(sensor.type)
            if (type == null) {
                type = "TYPE_UNKNOWN"
            }
            builder.append(type)
            start = builder.length
            builder.append("\nVendor: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Vendor: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.vendor)
            start = builder.length
            builder.append("\nVersion: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Version: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.version.toString())
            start = builder.length
            builder.append("\nResolution: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Resolution: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.resolution.toString())
            start = builder.length
            builder.append("\nMax Range: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Max Range: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.maximumRange.toString())
            start = builder.length
            builder.append("\nMin Delay: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Min Delay: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.minDelay.toString())
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                start = builder.length
                builder.append("\nFifo Max Event Count: ")
                builder.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD), start,
                    start + "Fifo Max Event Count: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.append(sensor.fifoMaxEventCount.toString())
                start = builder.length
                builder.append("\nFifo Reserved Event Count: ")
                builder.setSpan(
                    StyleSpan(android.graphics.Typeface.BOLD), start,
                    start + "Fifo Reserved Event Count: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                builder.append(sensor.fifoReservedEventCount.toString())

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
                    start = builder.length
                    builder.append("\nMax Delay: ")
                    builder.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), start,
                        start + "Max Delay: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.append(sensor.maxDelay.toString())
                    start = builder.length
                    builder.append("\nIs Wake Up Sensor: ")
                    builder.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), start,
                        start + "Is Wake Up Sensor: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.append(sensor.isWakeUpSensor.toString())
                    var reportingMode = "UNKNOWN"
                    when (sensor.reportingMode) {
                        Sensor.REPORTING_MODE_CONTINUOUS -> reportingMode = "CONTINUOUS"

                        Sensor.REPORTING_MODE_ON_CHANGE -> reportingMode = "ON CHANGE"

                        Sensor.REPORTING_MODE_ONE_SHOT -> reportingMode = "ONE SHOT"

                        Sensor.REPORTING_MODE_SPECIAL_TRIGGER -> reportingMode = "SPECIAL TRIGGER"
                    }
                    start = builder.length
                    builder.append("\nReporting Mode: ")
                    builder.setSpan(
                        StyleSpan(android.graphics.Typeface.BOLD), start,
                        start + "Reporting Mode: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    builder.append(reportingMode)
                }
            }
            start = builder.length
            builder.append("\nPower: ")
            builder.setSpan(
                StyleSpan(android.graphics.Typeface.BOLD), start,
                start + "Power: ".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.append(sensor.power.toString())
            builder.append(" mA\n\n")
        }

        return builder
    }
}

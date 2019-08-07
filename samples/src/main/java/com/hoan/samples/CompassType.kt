package com.hoan.samples

import android.content.Context
import android.view.Surface
import android.view.WindowManager
import com.hoan.dsensor.DSensor

fun getDSensorTypes(context: Context?, compassType: Int?): Int {
    if (context == null) return 0

    val sensorTypes = when((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
        Surface.ROTATION_90 -> DSensor.TYPE_X_AXIS_DIRECTION
        Surface.ROTATION_180 -> DSensor.TYPE_MINUS_Y_AXIS_DIRECTION
        Surface.ROTATION_270 -> DSensor.TYPE_MINUS_X_AXIS_DIRECTION
        else -> DSensor.TYPE_Y_AXIS_DIRECTION
    }

    return when (compassType) {
        CompassType.TYPE_COMPASS -> sensorTypes
        CompassType.TYPE_COMPASS_AND_DEPRECATED_ORIENTATION -> sensorTypes or DSensor.TYPE_DEPRECATED_ORIENTATION
        CompassType.TYPE_3D_COMPASS -> sensorTypes or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION
        CompassType.TYPE_3D_COMPASS_AND_DEPRECATED_ORIENTATION ->
            sensorTypes or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION or DSensor.TYPE_DEPRECATED_ORIENTATION
        else -> 0
    }
}

/**
 *
 * Compass
 */
object CompassType {

    /**
     * This type returns the angle between the magnetic north and the projection
     * of the axis starting from the lower left corner to the upper left corner
     * of the device.
     * In case of the device natural orientation i.e Portrait for phone and Landscape
     * for tablet, this value is the value[0] of getOrientation of the SDK SensorManager class.
     * However, this type also returns the correct direction independent of type of devices
     * as well as requested screen orientation of the activity. For example for tablet and
     * the activity is in Portrait only, the value return is either the direction of the
     * X-axis or minus X-axis depending on the manufacturer. If the activity does not set
     * orientation preference then when the activity started the direction is the direction
     * of the axis parallel to where the user looks.
     * Thus for a compass app, all you need to do is to call DSensorManager.startDProcessedSensor
     * passing in this type.
     * The value for this sensor will be from -PI to PI when the device is flat and Float.NAN otherwise.
     *
     * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD and
     * TYPE_GRAVITY or TYPE_ACCELEROMETER
     */
    const val TYPE_COMPASS = 1

    /**
     * This is like TYPE_COMPASS_FLAT_ONLY except that when the device
     * is not flat, the value return will be the direction of the device
     * minus z-axis (the direction of the back camera) instead of Float.NAN
     *
     * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD and
     * TYPE_GRAVITY or TYPE_ACCELEROMETER
     */
    const val TYPE_3D_COMPASS = 3

    /**
     * This is TYPE_COMPASS_FLAT_ONLY and the depreciated TYPE_ORIENTATION.
     * This is intended for testing purpose. The application that register
     * for this type should check the member sensorType of the parameter
     * DSensorEvent in the onProcessedValueChanged(DSensorEvent dSensorEvent)
     * callback for the type to update the appropriate UI view. The value
     * for TYPE_ORIENTATION should be the same or differ by PI/2, -PI/2, PI or
     * -PI depending on the initial orientation of the activity.
     *
     * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD,
     * TYPE_ORIENTATION and TYPE_GRAVITY or TYPE_ACCELEROMETER
     */
    const val TYPE_COMPASS_AND_DEPRECATED_ORIENTATION = 100

    /**
     * This is TYPE_3D_COMPASS and the depreciated TYPE_ORIENTATION.
     * This is intended for testing purpose. The application that register
     * for this type should check the member sensorType of the parameter
     * DSensorEvent in the onProcessedValueChanged(DSensorEvent dSensorEvent)
     * callback for the type to update the appropriate UI view.
     *
     * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD,
     * TYPE_ORIENTATION and TYPE_GRAVITY or TYPE_ACCELEROMETER
     */
    const val TYPE_3D_COMPASS_AND_DEPRECATED_ORIENTATION = 101
}

package com.hoan.dsensor

import android.content.Context
import android.hardware.Sensor
import android.view.Surface
import android.view.WindowManager

/**
 *
 * Compass
 */
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
 * Thus for a compass app, all you need to do is to call DSensorManager.startSensor
 * passing in this type.
 *
 * The value for this sensor will be from -PI to PI when the device is flat and Float.NAN otherwise.
 *
 * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD and
 * TYPE_GRAVITY or TYPE_ACCELEROMETER
 *
 * @param context   application context is fine.
 */
fun getCompassSensorType(context: Context) : Int {
    return when ((context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation) {
        Surface.ROTATION_90 -> TYPE_X_AXIS_DIRECTION
        Surface.ROTATION_180 -> TYPE_NEGATIVE_Y_AXIS_DIRECTION
        Surface.ROTATION_270 -> TYPE_NEGATIVE_X_AXIS_DIRECTION
        else -> TYPE_Y_AXIS_DIRECTION
    }
}

/**
 * This is compass as above plus that when the device
 * is not flat, the value return will be the direction of the device
 * minus z-axis (the direction of the back camera) instead of Float.NAN
 *
 * Note: this type required the device to have sensor of TYPE_MAGNETIC_FIELD and
 * TYPE_GRAVITY or TYPE_ACCELEROMETER
 */
fun get3DCompassSensor(context: Context) : Int {
    return getCompassSensorType(context) or TYPE_NEGATIVE_Z_AXIS_DIRECTION
}

fun getDirectionTypes(): List<Int> {
        return arrayListOf(TYPE_X_AXIS_DIRECTION, TYPE_NEGATIVE_X_AXIS_DIRECTION, TYPE_Y_AXIS_DIRECTION,
            TYPE_NEGATIVE_Y_AXIS_DIRECTION, TYPE_Z_AXIS_DIRECTION, TYPE_NEGATIVE_Z_AXIS_DIRECTION)
    }

    fun getWorldCoordinatesTypes(): List<Int> {
        return arrayListOf(TYPE_WORLD_ACCELEROMETER, TYPE_WORLD_LINEAR_ACCELERATION, TYPE_WORLD_GRAVITY, TYPE_WORLD_MAGNETIC_FIELD)
    }

    fun getRawDSensor(dWorldCoordinatesSensor: Int): Int {
        return when (dWorldCoordinatesSensor) {
            TYPE_WORLD_ACCELEROMETER -> TYPE_DEVICE_ACCELEROMETER
            TYPE_WORLD_LINEAR_ACCELERATION -> TYPE_DEVICE_LINEAR_ACCELERATION
            TYPE_WORLD_GRAVITY -> TYPE_DEVICE_GRAVITY
            TYPE_WORLD_MAGNETIC_FIELD -> TYPE_DEVICE_MAGNETIC_FIELD
            else -> ERROR_UNSUPPORTED_TYPE
        }
    }

    fun getDSensorList(): ArrayList<Int> {
        return arrayListOf(TYPE_DEVICE_ACCELEROMETER, TYPE_DEVICE_LINEAR_ACCELERATION, TYPE_DEVICE_GRAVITY,
            TYPE_DEVICE_MAGNETIC_FIELD, TYPE_GYROSCOPE, TYPE_ROTATION_VECTOR, TYPE_WORLD_ACCELEROMETER,
            TYPE_WORLD_LINEAR_ACCELERATION, TYPE_WORLD_GRAVITY, TYPE_WORLD_MAGNETIC_FIELD, TYPE_INCLINATION,
            TYPE_DEVICE_ROTATION, TYPE_PITCH, TYPE_ROLL, TYPE_X_AXIS_DIRECTION, TYPE_NEGATIVE_X_AXIS_DIRECTION,
            TYPE_Y_AXIS_DIRECTION, TYPE_NEGATIVE_Y_AXIS_DIRECTION, TYPE_Z_AXIS_DIRECTION,
            TYPE_NEGATIVE_Z_AXIS_DIRECTION)
    }

    fun getDSensorType(androidSensor: Int): Int {
        return when (androidSensor) {
            Sensor.TYPE_ACCELEROMETER -> TYPE_DEVICE_ACCELEROMETER
            Sensor.TYPE_LINEAR_ACCELERATION -> TYPE_DEVICE_LINEAR_ACCELERATION
            Sensor.TYPE_GRAVITY -> TYPE_DEVICE_GRAVITY
            Sensor.TYPE_MAGNETIC_FIELD -> TYPE_DEVICE_MAGNETIC_FIELD
            Sensor.TYPE_ROTATION_VECTOR -> TYPE_ROTATION_VECTOR
            Sensor.TYPE_GYROSCOPE -> TYPE_GYROSCOPE
            else -> ERROR_UNSUPPORTED_TYPE
        }
    }

    const val TYPE_DEVICE_ACCELEROMETER = 2
    const val TYPE_DEVICE_LINEAR_ACCELERATION = 4
    const val TYPE_DEVICE_GRAVITY = 8
    const val TYPE_DEVICE_MAGNETIC_FIELD = 16
    const val TYPE_GYROSCOPE = 32
    const val TYPE_ROTATION_VECTOR = 64

    /**
     * This type give the device acceleration vector in world coordinates system
     */
    const val TYPE_WORLD_ACCELEROMETER = 128

    /**
     * This type give the device linear acceleration vector in world coordinates system
     */
    const val TYPE_WORLD_LINEAR_ACCELERATION = 256

    /**
     * This type give the device gravity vector in world coordinates system
     */
    const val TYPE_WORLD_GRAVITY = 512

    /**
     * This type give the device magnetic field vector in world coordinates system
     */
    const val TYPE_WORLD_MAGNETIC_FIELD = 1024

    /**
     * This type gives the inclination of the device.
     * This is the angle between the surface of the device screen
     * and the surface parallel to the earth surface.
     * Mathematically, this is the angle between the device
     * z axis (0, 0, 1) and the gravity vector in device coordinates.
     *
     * Values are between 0 and PI.
     */
    const val TYPE_INCLINATION = 2048

    /**
     * This type gives the rotation of the device, i.e. portrait/landscape
     * For example, for most phone, when the phone is held 45 degree
     * counterclockwise from portrait, the value return will be PI/4.
     *
     * Values depend on inclination which are between -PI and PI
     * when inclination >= 25 (degree) or inclination <= 155, otherwise it is Float.NaN.
     */
    const val TYPE_DEVICE_ROTATION = 4096

    /**
     * This is the angle between the projection of the device y-axis
     * into the world East-North plane and the device y-axis. That is
     * the amount of rotation from laying flat when the device is in
     * portrait when rotate up or down. This is the same as the pitch
     * in getOrientation when the device is flat. When calling
     * remapCoordinateSystem and then getOrientation will be different
     * since in this case the SDK getOrientation will calculate the
     * pitch in a new frame of reference. Thus for example if calling
     * remapCoordinateSystem(inR, AXIS_X, AXIS_Z, outR) then getOrientation
     * the pitch will be PI/2 when the device is flat.
     */
    const val TYPE_PITCH = 8192

    /**
     * This is the angle between the projection of the device x-axis
     * into the world East-North plane and the device x-axis.
     * See TYPE_PITCH for more info.
     */
    const val TYPE_ROLL = 16384

    /**
     * This is the angle between the world magnetic north and
     * the projection of the device x-axis into the world
     * East-North plane. Register for this type if you want to
     * write a compass app for tablet in portrait mode.
     * The value is between PI and -PI when the device is flat,
     * otherwise it is NAN
     */
    const val TYPE_X_AXIS_DIRECTION = 32768

    /**
     * Same as TYPE_X_AXIS_DIRECTION but minus x-axis instead.
     */
    const val TYPE_NEGATIVE_X_AXIS_DIRECTION = 65536

    /**
     * This is the angle between the world magnetic north and
     * the projection of the device y-axis into the world
     * East-North plane. Register for this type if you want to
     * write a compass app for phone in portrait mode. This is
     * the value returned by getOrientation in values[0] when
     * remapCoordinateSystem is not called prior to getOrientation.
     * The value is between PI and -PI when the device is flat,
     * otherwise it is NAN
     */
    const val TYPE_Y_AXIS_DIRECTION = 131072

    /**
     * Same as TYPE_Y_AXIS_DIRECTION but minus y-axis instead.
     */
    const val TYPE_NEGATIVE_Y_AXIS_DIRECTION = 262144

    /**
     * Same as TYPE_NEGATIVE_Z_AXIS_DIRECTION below but z-axis instead.
     */
    const val TYPE_Z_AXIS_DIRECTION = 524288

    /**
     * This is the angle between the world magnetic north and
     * the projection of the device minus z-axis into the world
     * East-North plane. Register for this type if you want to
     * find the direction of the back camera. This is
     * the value returned by getOrientation in values[0] when
     * remapCoordinateSystem(inR, AXIS_X, AXIS_Z, outR) is called
     * prior to getOrientation.
     * The value is between PI and -PI when the device is not flat,
     * otherwise it is NAN. This value is independent of the
     * orientation of the device i.e portrait/landscape since
     * the direction of the device -z does not change when the
     * device orientation changes.
     */
    const val TYPE_NEGATIVE_Z_AXIS_DIRECTION = 1048576

    /**
     * All of the above.
     */
    const val TYPE_ALL = (TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER
            or TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY
            or TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD
            or TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION
            or TYPE_GYROSCOPE or TYPE_ROTATION_VECTOR
            or TYPE_INCLINATION or TYPE_DEVICE_ROTATION
            or TYPE_PITCH or TYPE_ROLL
            or TYPE_Z_AXIS_DIRECTION or TYPE_NEGATIVE_Z_AXIS_DIRECTION
            or TYPE_X_AXIS_DIRECTION or TYPE_NEGATIVE_X_AXIS_DIRECTION
            or TYPE_Y_AXIS_DIRECTION or TYPE_NEGATIVE_Y_AXIS_DIRECTION)



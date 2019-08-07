package com.hoan.dsensor

/**
 * Class represents sensors supported.
 */
object DSensor {
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
    const val TYPE_MINUS_X_AXIS_DIRECTION = 65536

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
    const val TYPE_MINUS_Y_AXIS_DIRECTION = 262144

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
    const val TYPE_MINUS_Z_AXIS_DIRECTION = 524288

    /**
     * Same as TYPE_MINUS_Z_AXIS_DIRECTION but z-axis instead.
     */
    const val TYPE_Z_AXIS_DIRECTION = 1048576

    /**
     * This is the depreciated Sensor.TYPE_ORIENTATION.
     *
     * Values are between -PI and PI.
     */
    const val TYPE_DEPRECATED_ORIENTATION = 2097152

    /**
     * All of the above.
     */
    const val TYPE_ALL = (TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER
            or TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY
            or TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD
            or TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION
            or TYPE_GYROSCOPE or TYPE_ROTATION_VECTOR
            or TYPE_INCLINATION or TYPE_DEVICE_ROTATION
            or TYPE_PITCH or TYPE_ROLL or TYPE_DEPRECATED_ORIENTATION
            or TYPE_Z_AXIS_DIRECTION or TYPE_MINUS_Z_AXIS_DIRECTION
            or TYPE_X_AXIS_DIRECTION or TYPE_MINUS_X_AXIS_DIRECTION
            or TYPE_Y_AXIS_DIRECTION or TYPE_MINUS_Y_AXIS_DIRECTION)
}

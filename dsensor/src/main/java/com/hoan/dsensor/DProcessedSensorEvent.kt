package com.hoan.dsensor

/**
 * Processed sensors' values
 */
class DProcessedSensorEvent(var accelerometerInDeviceBasis: DSensorEvent? = null,
                            var accelerometerInWorldBasis: DSensorEvent? = null,
                            var gravityInDeviceBasis: DSensorEvent? = null,
                            var gravityInWorldBasis: DSensorEvent? = null,
                            var magneticFieldInDeviceBasis: DSensorEvent? = null,
                            var magneticFieldInWorldBasis: DSensorEvent? = null,
                            var linearAccelerationInDeviceBasis: DSensorEvent? = null,
                            var linearAccelerationInWorldBasis: DSensorEvent? = null,
                            var gyroscope: DSensorEvent? = null,
                            var rotationVector: DSensorEvent? = null,
                            var depreciatedOrientation: DSensorEvent? = null,
                            var inclination: DSensorEvent? = null,
                            var deviceRotation: DSensorEvent? = null,
                            var pitch: DSensorEvent? = null,
                            var roll: DSensorEvent? = null,
                            var xAxisDirection: DSensorEvent? = null,
                            var negativeXAxisDirection: DSensorEvent? = null,
                            var yAxisDirection: DSensorEvent? = null,
                            var negativeYAxisDirection: DSensorEvent? = null,
                            var zAxisDirection: DSensorEvent? = null,
                            var negativeZAxisDirection: DSensorEvent? = null) {

    fun getDSensorEvent(dSensorType: Int): DSensorEvent? {
        return when (dSensorType) {
            TYPE_DEVICE_ACCELEROMETER -> accelerometerInDeviceBasis
            TYPE_DEVICE_LINEAR_ACCELERATION -> linearAccelerationInDeviceBasis
            TYPE_DEVICE_GRAVITY -> gravityInDeviceBasis
            TYPE_DEVICE_MAGNETIC_FIELD -> magneticFieldInDeviceBasis
            TYPE_GYROSCOPE -> gyroscope
            TYPE_ROTATION_VECTOR -> rotationVector
            TYPE_WORLD_ACCELEROMETER -> accelerometerInWorldBasis
            TYPE_WORLD_LINEAR_ACCELERATION -> linearAccelerationInWorldBasis
            TYPE_WORLD_GRAVITY -> gravityInWorldBasis
            TYPE_WORLD_MAGNETIC_FIELD -> magneticFieldInWorldBasis
            TYPE_INCLINATION -> inclination
            TYPE_DEVICE_ROTATION -> deviceRotation
            TYPE_PITCH -> pitch
            TYPE_ROLL -> roll
            TYPE_X_AXIS_DIRECTION -> xAxisDirection
            TYPE_NEGATIVE_X_AXIS_DIRECTION -> negativeXAxisDirection
            TYPE_Y_AXIS_DIRECTION -> yAxisDirection
            TYPE_NEGATIVE_Y_AXIS_DIRECTION -> negativeYAxisDirection
            TYPE_Z_AXIS_DIRECTION -> zAxisDirection
            TYPE_NEGATIVE_Z_AXIS_DIRECTION -> negativeZAxisDirection
            TYPE_DEPRECATED_ORIENTATION -> depreciatedOrientation
            else -> null
        }
    }

    fun setDSensorEvent(dSensorEvent: DSensorEvent) {
        when (dSensorEvent.sensorType) {
            TYPE_DEVICE_ACCELEROMETER -> accelerometerInDeviceBasis = dSensorEvent
            TYPE_DEVICE_LINEAR_ACCELERATION -> linearAccelerationInDeviceBasis = dSensorEvent
            TYPE_DEVICE_GRAVITY -> gravityInDeviceBasis = dSensorEvent
            TYPE_DEVICE_MAGNETIC_FIELD -> magneticFieldInDeviceBasis = dSensorEvent
            TYPE_GYROSCOPE -> gyroscope = dSensorEvent
            TYPE_ROTATION_VECTOR -> rotationVector = dSensorEvent
            TYPE_WORLD_ACCELEROMETER -> accelerometerInWorldBasis = dSensorEvent
            TYPE_WORLD_LINEAR_ACCELERATION -> linearAccelerationInWorldBasis = dSensorEvent
            TYPE_WORLD_GRAVITY -> gravityInWorldBasis = dSensorEvent
            TYPE_WORLD_MAGNETIC_FIELD -> magneticFieldInWorldBasis = dSensorEvent
            TYPE_INCLINATION -> inclination = dSensorEvent
            TYPE_DEVICE_ROTATION -> deviceRotation = dSensorEvent
            TYPE_PITCH -> pitch = dSensorEvent
            TYPE_ROLL -> roll = dSensorEvent
            TYPE_X_AXIS_DIRECTION -> xAxisDirection = dSensorEvent
            TYPE_NEGATIVE_X_AXIS_DIRECTION -> negativeXAxisDirection = dSensorEvent
            TYPE_Y_AXIS_DIRECTION -> yAxisDirection = dSensorEvent
            TYPE_NEGATIVE_Y_AXIS_DIRECTION -> negativeYAxisDirection = dSensorEvent
            TYPE_Z_AXIS_DIRECTION -> zAxisDirection = dSensorEvent
            TYPE_NEGATIVE_Z_AXIS_DIRECTION -> negativeZAxisDirection = dSensorEvent
            TYPE_DEPRECATED_ORIENTATION -> depreciatedOrientation = dSensorEvent
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()

        accelerometerInDeviceBasis?.let { sb.append(it.toString()) }
        accelerometerInWorldBasis?.let { sb.append(it.toString()) }
        gravityInDeviceBasis?.let { sb.append(it.toString()) }
        gravityInWorldBasis?.let { sb.append(it.toString()) }
        magneticFieldInDeviceBasis?.let { sb.append(it.toString()) }
        magneticFieldInWorldBasis?.let { sb.append(it.toString()) }
        linearAccelerationInDeviceBasis?.let { sb.append(it.toString()) }
        linearAccelerationInWorldBasis?.let { sb.append(it.toString()) }
        gyroscope?.let { sb.append(it.toString()) }
        rotationVector?.let { sb.append(it.toString()) }
        depreciatedOrientation?.let { sb.append(it.toString()) }
        inclination?.let { sb.append(it.toString()) }
        deviceRotation?.let { sb.append(it.toString()) }
        pitch?.let { sb.append(it.toString()) }
        roll?.let { sb.append(it.toString()) }
        xAxisDirection?.let { sb.append(it.toString()) }
        negativeXAxisDirection?.let { sb.append(it.toString()) }
        yAxisDirection?.let { sb.append(it.toString()) }
        negativeYAxisDirection?.let { sb.append(it.toString()) }
        zAxisDirection?.let { sb.append(it.toString()) }
        negativeZAxisDirection?.let { sb.append(it.toString()) }

        return sb.toString()
    }
}

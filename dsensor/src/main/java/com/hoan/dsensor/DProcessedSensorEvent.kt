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
                            var minusXAxisDirection: DSensorEvent? = null,
                            var yAxisDirection: DSensorEvent? = null,
                            var minusYAxisDirection: DSensorEvent? = null,
                            var zAxisDirection: DSensorEvent? = null,
                            var minusZAxisDirection: DSensorEvent? = null) {


    override fun toString(): String {
        val sb = StringBuilder()

        accelerometerInDeviceBasis?.let { sb.append(accelerometerInDeviceBasis.toString()) }
        accelerometerInWorldBasis?.let { sb.append(accelerometerInWorldBasis.toString()) }
        gravityInDeviceBasis?.let { sb.append(gravityInDeviceBasis.toString()) }
        gravityInWorldBasis?.let { sb.append(gravityInWorldBasis.toString()) }
        magneticFieldInDeviceBasis?.let { sb.append(magneticFieldInDeviceBasis.toString()) }
        magneticFieldInWorldBasis?.let { sb.append(magneticFieldInWorldBasis.toString()) }
        linearAccelerationInDeviceBasis?.let { sb.append(linearAccelerationInDeviceBasis.toString()) }
        linearAccelerationInWorldBasis?.let { sb.append(linearAccelerationInWorldBasis.toString()) }
        gyroscope?.let { sb.append(gyroscope.toString()) }
        rotationVector?.let { sb.append(rotationVector.toString()) }
        depreciatedOrientation?.let { sb.append(depreciatedOrientation.toString()) }
        inclination?.let { sb.append(inclination.toString()) }
        deviceRotation?.let { sb.append(deviceRotation.toString()) }
        pitch?.let { sb.append(pitch.toString()) }
        roll?.let { sb.append(roll.toString()) }
        xAxisDirection?.let { sb.append(xAxisDirection.toString()) }
        minusXAxisDirection?.let { sb.append(minusXAxisDirection.toString()) }
        yAxisDirection?.let { sb.append(yAxisDirection.toString()) }
        minusYAxisDirection?.let { sb.append(minusYAxisDirection.toString()) }
        zAxisDirection?.let { sb.append(zAxisDirection.toString()) }
        minusZAxisDirection?.let { sb.append(minusZAxisDirection.toString()) }

        return sb.toString()
    }
}

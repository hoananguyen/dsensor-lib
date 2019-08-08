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
        minusXAxisDirection?.let { sb.append(it.toString()) }
        yAxisDirection?.let { sb.append(it.toString()) }
        minusYAxisDirection?.let { sb.append(it.toString()) }
        zAxisDirection?.let { sb.append(it.toString()) }
        minusZAxisDirection?.let { sb.append(it.toString()) }

        return sb.toString()
    }
}

package com.hoan.dsensor

import com.hoan.dsensor.utils.ONE_FIFTY_FIVE_DEGREE_IN_RADIAN
import com.hoan.dsensor.utils.TWENTY_FIVE_DEGREE_IN_RADIAN
import com.hoan.dsensor.utils.productOfSquareMatrixAndVector
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2


/**
 * Low pass filter constant.
 * Use to filter linear acceleration from accelerometer values.
 */
private const val ALPHA = .1f
private const val ONE_MINUS_ALPHA = 1 - ALPHA

internal fun calculateDeviceRotation(gravityNorm: Float, inclinationEvent: DSensorEvent, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    val deviceRotation = if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN
        ||inclinationEvent.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
        Float.NaN
    }
    else {
        atan2(gravityDSensorEvent.values[0] / gravityNorm, gravityDSensorEvent.values[1] / gravityNorm)
    }

    return DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp, floatArrayOf(deviceRotation))
}

internal fun calculateDeviceRotation(inclinationEvent: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
    val deviceRotation = if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN
        ||inclinationEvent.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
        Float.NaN
    }
    else {
        atan2(rotationMatrix[6], rotationMatrix[7])
    }

    return DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp, floatArrayOf(deviceRotation))
}

internal fun calculateGravity(accelerometerDSensorEvent: DSensorEvent, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    if (gravityDSensorEvent.timestamp == 0L) {
        gravityDSensorEvent.values = accelerometerDSensorEvent.values.copyOf()
    } else {
        for (i in 0..2) {
            gravityDSensorEvent.values[i] = ALPHA * accelerometerDSensorEvent.values[i] + ONE_MINUS_ALPHA * gravityDSensorEvent.values[i]
        }
    }

    gravityDSensorEvent.accuracy = accelerometerDSensorEvent.accuracy
    gravityDSensorEvent.timestamp = accelerometerDSensorEvent.timestamp

    return gravityDSensorEvent
}

internal fun calculateInclination(gravityDSensorEvent: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
    return DSensorEvent(TYPE_INCLINATION, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp, floatArrayOf(acos(rotationMatrix[8])))
}

internal fun calculateInclination(gravityNorm: Float, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    return DSensorEvent(TYPE_INCLINATION, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp, floatArrayOf(gravityDSensorEvent.values[2] / gravityNorm))
}

internal fun calculateLinearAcceleration(accelerometerDSensorEvent: DSensorEvent, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    val values = FloatArray(3)
    for (i in 0..2) {
        values[i] = accelerometerDSensorEvent.values[i] - gravityDSensorEvent.values[i]
    }

    return DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, accelerometerDSensorEvent.accuracy, accelerometerDSensorEvent.timestamp, values)
}

internal fun calculatePitch(gravityNorm: Float, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    return DSensorEvent(TYPE_PITCH, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp,
        floatArrayOf(asin(-gravityDSensorEvent.values[1] / gravityNorm)))
}

internal fun calculatePitch(gravityDSensorEvent: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
    return DSensorEvent(TYPE_PITCH, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp, floatArrayOf(asin(-rotationMatrix[7])))
}

internal fun calculateRoll(gravityDSensorEvent: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
    return DSensorEvent(TYPE_ROLL, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp,
        floatArrayOf(atan2(-rotationMatrix[6], rotationMatrix[8])))
}

internal fun calculateRoll(gravityNorm: Float, gravityDSensorEvent: DSensorEvent): DSensorEvent {
    return DSensorEvent(TYPE_ROLL, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp,
        floatArrayOf(atan2(-gravityDSensorEvent.values[0] / gravityNorm, gravityDSensorEvent.values[2] / gravityNorm)))
}

internal fun transformToWorldCoordinate(dSensorType: Int, dSensorEventToBeTransformed: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent? {
    productOfSquareMatrixAndVector(rotationMatrix, dSensorEventToBeTransformed.values)?.apply {
        return DSensorEvent(dSensorType, dSensorEventToBeTransformed.accuracy, dSensorEventToBeTransformed.timestamp, this.copyOf())
    }

    return null
}
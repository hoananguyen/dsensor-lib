package com.hoan.dsensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.*
import java.util.*
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.round

internal const val DEFAULT_HISTORY_SIZE = 10

internal const val TWENTY_FIVE_DEGREE_IN_RADIAN = 0.436332313f

internal const val ONE_FIFTY_FIVE_DEGREE_IN_RADIAN = 2.7052603f

/**
 * Low pass filter constant.
 * Use to filter linear acceleration from accelerometer values.
 */
private const val ALPHA = .1f
private const val ONE_MINUS_ALPHA = 1 - ALPHA

class DSensorEventProcessor(dSensorTypes: Int,
                            dSensorEventListener: DSensorEventListener,
                            hasGravitySensor: Boolean = true,
                            hasLinearAccelerationSensor: Boolean = true,
                            hasRotationVectorSensor: Boolean = true,
                            historyMaxLength: Int = DEFAULT_HISTORY_SIZE) : SensorEventListener {
    /**
     * The type of sensor to listen to. See DSensor.
     */
    private val mDSensorTypes: Int = dSensorTypes

    /**
     * Flag to indicate device has TYPE_ROTATION_VECTOR. If value is true then
     * data processing is done when onSensorChanged is of TYPE_ROTATION_VECTOR.
     */
    private val mHasRotationVectorSensor: Boolean = hasRotationVectorSensor

    /**
     * Flag to indicate if calculation of gravity is needed that is device has no TYPE_GRAVITY.
     * gravity is then derive from accelerometer value using low filter.
     */
    private val mCalculateGravity: Boolean

    /**
     * Flag to indicate device has TYPE_LINEAR_ACCELERATION. If needed and the value is
     * false then calculate from accelerometer and gravity.
     */
    private val mHasLinearAccelerationSensor: Boolean = hasLinearAccelerationSensor

    private val mDSensorEventListener: DSensorEventListener = dSensorEventListener

    private var mUIHandler: Handler?
    private val mPostEventRunnable = PostEventRunnable()

    /**
     * List to keep history directions of compass for averaging.
     */
    private val mXAxisDirectionHistories: DirectionHistory?
    private val mMinusXAxisDirectionHistories: DirectionHistory?
    private val mYAxisDirectionHistories: DirectionHistory?
    private val mMinusYAxisDirectionHistories: DirectionHistory?
    private val mZAxisDirectionHistories: DirectionHistory?
    private val mMinusZAxisDirectionHistories: DirectionHistory?

    /**
     * List to keep history of sensor in world coordinate for averaging.
     */
    private val mAccelerometerInWorldBasisHistories: WorldHistory?
    private val mGravityInWorldBasisHistories: WorldHistory?
    private val mLinearAccelerationInWorldBasisHistories: WorldHistory?

    /**
     * For DSensor types that required Rotation Matrix for data processing
     * i.e. direction or world coordinate
     */
    private val mProcessDataWithRotationMatrix: Boolean

    /**
     * For sensor type that does not require Rotation Matrix'
     * but gravity for calculation
     */
    private val mProcessDataWithGravity: Boolean

    private val mCalculateInclination: Boolean

    private val mRotationMatrix = FloatArray(9)
    private val mAccelerometer: DSensorEvent = DSensorEvent(DSensor.TYPE_DEVICE_ACCELEROMETER, 3)
    private val mGravity: DSensorEvent = DSensorEvent(DSensor.TYPE_DEVICE_GRAVITY, 3)
    private val mMagneticField: DSensorEvent = DSensorEvent(DSensor.TYPE_DEVICE_MAGNETIC_FIELD, 3)
    // need this member to calculate TYPE_WORLD_LINEAR_ACCELERATION
    // when device has TYPE_LINEAR_ACCELERATION
    private val mLinearAcceleration: DSensorEvent?
    private val mInclination: DSensorEvent?

    init {
        mUIHandler = Handler(Looper.getMainLooper())
        mXAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        mMinusXAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_MINUS_X_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        mYAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        mMinusYAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        mZAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_Z_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        mMinusZAxisDirectionHistories = if (mDSensorTypes and DSensor.TYPE_MINUS_Z_AXIS_DIRECTION != 0)
            DirectionHistory(historyMaxLength) else null
        val hasDirectionMember = mXAxisDirectionHistories != null || mYAxisDirectionHistories != null
                || mZAxisDirectionHistories != null || mMinusXAxisDirectionHistories != null
                || mMinusYAxisDirectionHistories != null || mMinusZAxisDirectionHistories != null
        mAccelerometerInWorldBasisHistories = if (mDSensorTypes and DSensor.TYPE_WORLD_ACCELEROMETER != 0)
            WorldHistory(historyMaxLength, 3) else null
        mGravityInWorldBasisHistories = if (mDSensorTypes and DSensor.TYPE_WORLD_GRAVITY != 0)
            WorldHistory(historyMaxLength, 3) else null
        mLinearAccelerationInWorldBasisHistories = if (mDSensorTypes and DSensor.TYPE_WORLD_LINEAR_ACCELERATION != 0)
            WorldHistory(historyMaxLength, 3) else null
        mProcessDataWithRotationMatrix = mDSensorTypes and DSensor.TYPE_WORLD_MAGNETIC_FIELD != 0
                || mLinearAccelerationInWorldBasisHistories != null
                || mGravityInWorldBasisHistories != null
                || mAccelerometerInWorldBasisHistories != null || hasDirectionMember
        mCalculateInclination = (mDSensorTypes and DSensor.TYPE_INCLINATION != 0
                || mDSensorTypes and DSensor.TYPE_DEVICE_ROTATION != 0 || hasDirectionMember)
        mInclination = if (mCalculateInclination) DSensorEvent(DSensor.TYPE_INCLINATION, 1) else null
        mProcessDataWithGravity = mCalculateInclination || mDSensorTypes and DSensor.TYPE_PITCH != 0
                || mDSensorTypes and DSensor.TYPE_ROLL != 0
        mCalculateGravity = !hasGravitySensor && (mProcessDataWithGravity || mProcessDataWithRotationMatrix)
        mLinearAcceleration = if (!mHasLinearAccelerationSensor && mLinearAccelerationInWorldBasisHistories != null)
            DSensorEvent(DSensor.TYPE_DEVICE_LINEAR_ACCELERATION, 3) else null
    }

    @Synchronized fun finish() {
        logger(DSensorEventProcessor::class.java.simpleName, "finish")
        mUIHandler?.removeCallbacks(mPostEventRunnable)
        mUIHandler = null
    }

    @Deprecated(message = "TYPE_ORIENTATION is for testing")
    override fun onSensorChanged(event: SensorEvent) {
        logger(DSensorEventProcessor::class.java.simpleName, "onSensorChanged: type = ${event.sensor.type}")
        val dProcessedSensorEvent = DProcessedSensorEvent()

        val changedSensorTypes = when {
            event.sensor.type == Sensor.TYPE_ACCELEROMETER -> onAccelerometerChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_GYROSCOPE -> onGyroscopeChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD -> onMagneticFieldChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_ORIENTATION -> onOrientationChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_GRAVITY -> onGravityChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION -> onLinearAccelerationChanged(event, dProcessedSensorEvent)
            event.sensor.type == Sensor.TYPE_ROTATION_VECTOR -> onRotationVectorChanged(event, dProcessedSensorEvent)
            else -> 0
        }

        if (changedSensorTypes != 0) {
            mPostEventRunnable.mChangedSensorTypes = changedSensorTypes
            mPostEventRunnable.mDProcessedSensorEvent = dProcessedSensorEvent
            mUIHandler?.post(mPostEventRunnable)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        logger(DSensorEventProcessor::class.java.simpleName, "onAccuracyChanged(" + sensor.name + ", " + accuracy + ")")
    }

    private fun onAccelerometerChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onAccelerometerChanged")
        var changedSensorTypes = 0

        if (mDSensorTypes and DSensor.TYPE_DEVICE_ACCELEROMETER != 0) {
            dProcessedSensorEvent.accelerometerInDeviceBasis =
                    DSensorEvent(DSensor.TYPE_DEVICE_ACCELEROMETER, event.accuracy, event.timestamp, event.values)
            changedSensorTypes = DSensor.TYPE_DEVICE_ACCELEROMETER
        }

        if (mCalculateGravity) {
            setAccelerometer(event)
            calculateGravity()

            if (mDSensorTypes and DSensor.TYPE_DEVICE_GRAVITY != 0) {
                dProcessedSensorEvent.gravityInDeviceBasis = DSensorEvent(mGravity)
                changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_GRAVITY
            }

            if (mDSensorTypes and (DSensor.TYPE_DEVICE_LINEAR_ACCELERATION or DSensor.TYPE_WORLD_LINEAR_ACCELERATION) != 0) {
                val dLinearAccelerationSensorEvent = calculateLinearAcceleration()
                if (mLinearAcceleration != null) {
                    setLinearAcceleration(dLinearAccelerationSensorEvent)
                }
                if (mDSensorTypes and DSensor.TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
                    dProcessedSensorEvent.linearAccelerationInDeviceBasis = DSensorEvent(dLinearAccelerationSensorEvent)
                    changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_LINEAR_ACCELERATION
                }
            }
            if (mMagneticField.timestamp != 0L) {
                if (mProcessDataWithRotationMatrix && !mHasRotationVectorSensor) {
                    if (SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity.values, mMagneticField.values)) {
                        changedSensorTypes = changedSensorTypes or processSensorDataWithRotationMatrix(dProcessedSensorEvent)
                    }
                } else if (mProcessDataWithGravity) {
                    changedSensorTypes = changedSensorTypes or processSensorData(dProcessedSensorEvent)
                }
            }
        }
        return changedSensorTypes
    }

    private fun onGyroscopeChanged(sensorEvent: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onGyroscopeChanged")
        var changedSensorTypes = 0
        if (mDSensorTypes and DSensor.TYPE_GYROSCOPE != 0) {
            dProcessedSensorEvent.gyroscope = DSensorEvent(DSensor.TYPE_GYROSCOPE, sensorEvent.accuracy,
                    sensorEvent.timestamp, sensorEvent.values.copyOf())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_GYROSCOPE
        }
        return changedSensorTypes
    }

    private fun onMagneticFieldChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onMagneticFieldChanged")
        var changedSensorTypes = 0

        mMagneticField.accuracy = event.accuracy
        mMagneticField.timestamp = event.timestamp
        System.arraycopy(event.values, 0, mMagneticField.values, 0, event.values.size)

        if (mDSensorTypes and DSensor.TYPE_DEVICE_MAGNETIC_FIELD != 0) {
            dProcessedSensorEvent.magneticFieldInDeviceBasis = DSensorEvent(DSensor.TYPE_DEVICE_MAGNETIC_FIELD,
                    event.accuracy, event.timestamp, event.values)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_MAGNETIC_FIELD
        }

        if (mGravity.timestamp == 0L) {
            if (mProcessDataWithRotationMatrix && !mHasRotationVectorSensor) {
                if (SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity.values, mMagneticField.values)) {
                    changedSensorTypes = changedSensorTypes or processSensorDataWithRotationMatrix(dProcessedSensorEvent)
                }
            } else if (mProcessDataWithGravity) {
                changedSensorTypes = changedSensorTypes or processSensorData(dProcessedSensorEvent)
            }
        }

        return changedSensorTypes
    }

    private fun onOrientationChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onOrientationChanged  angle = " + round(event.values[0]))
        dProcessedSensorEvent.depreciatedOrientation = DSensorEvent(DSensor.TYPE_DEPRECATED_ORIENTATION,
                event.accuracy, event.timestamp, event.values)

        return DSensor.TYPE_DEPRECATED_ORIENTATION
    }

    private fun onGravityChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onGravityChanged")
        var changedSensorTypes = 0

        mGravity.accuracy = event.accuracy
        mGravity.timestamp = event.timestamp
        System.arraycopy(event.values, 0, mGravity.values, 0, event.values.size)

        if (mDSensorTypes and DSensor.TYPE_DEVICE_GRAVITY != 0) {
            dProcessedSensorEvent.gravityInDeviceBasis = DSensorEvent(mGravity)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_GRAVITY
        }

        if (mMagneticField.timestamp != 0L) {
            if (mProcessDataWithRotationMatrix && !mHasRotationVectorSensor) {
                if (SensorManager.getRotationMatrix(mRotationMatrix, null, mGravity.values, mMagneticField.values)) {
                    changedSensorTypes = changedSensorTypes or processSensorDataWithRotationMatrix(dProcessedSensorEvent)
                }
            } else if (mProcessDataWithGravity) {
                changedSensorTypes = changedSensorTypes or processSensorData(dProcessedSensorEvent)
            }
        }

        return changedSensorTypes
    }

    private fun onLinearAccelerationChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onLinearAccelerationChanged")
        var changedSensorTypes = 0

        if (mDSensorTypes and DSensor.TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
            dProcessedSensorEvent.linearAccelerationInDeviceBasis = DSensorEvent(DSensor.TYPE_DEVICE_LINEAR_ACCELERATION,
                    event.accuracy, event.timestamp, event.values)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_LINEAR_ACCELERATION
        }

        if (mDSensorTypes and DSensor.TYPE_WORLD_LINEAR_ACCELERATION != 0 && mLinearAccelerationInWorldBasisHistories != null) {
            mLinearAccelerationInWorldBasisHistories.add(DSensor.TYPE_DEVICE_LINEAR_ACCELERATION,
                event.accuracy, event.timestamp, event.values)
            dProcessedSensorEvent.linearAccelerationInWorldBasis =
                mLinearAccelerationInWorldBasisHistories.getAverageSensorEvent(DSensor.TYPE_DEVICE_LINEAR_ACCELERATION)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_WORLD_LINEAR_ACCELERATION
        }

        return changedSensorTypes
    }

    private fun onRotationVectorChanged(event: SensorEvent, dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "onRotationVectorChanged")
        var changedSensorTypes = 0

        if (mDSensorTypes and DSensor.TYPE_ROTATION_VECTOR != 0) {
            dProcessedSensorEvent.rotationVector = DSensorEvent(DSensor.TYPE_ROTATION_VECTOR, event.accuracy,
                    event.timestamp, event.values)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_ROTATION_VECTOR
        }

        SensorManager.getRotationMatrixFromVector(mRotationMatrix, event.values)

        if (mProcessDataWithRotationMatrix) {
            changedSensorTypes = changedSensorTypes or processSensorDataWithRotationMatrix(dProcessedSensorEvent)
        }

        return changedSensorTypes
    }

    private fun setAccelerometer(sensorEvent: SensorEvent) {
        mAccelerometer.accuracy = sensorEvent.accuracy
        mAccelerometer.timestamp = sensorEvent.timestamp
        System.arraycopy(sensorEvent.values, 0, mAccelerometer.values, 0, 3)
    }

    private fun calculateGravity() {
        logger(DSensorEventProcessor::class.java.simpleName, "calculateGravity")
        if (mGravity.timestamp == 0L) {
            System.arraycopy(mAccelerometer.values, 0, mGravity.values, 0, 3)
        } else {
            for (i in 0..2) {
                mGravity.values[i] = ALPHA * mAccelerometer.values[i] + ONE_MINUS_ALPHA * mGravity.values[i]
            }
        }

        mGravity.accuracy = mAccelerometer.accuracy
        mGravity.timestamp = mAccelerometer.timestamp
    }

    private fun calculateLinearAcceleration(): DSensorEvent {
        logger(DSensorEventProcessor::class.java.simpleName, "calculateLinearAcceleration")
        val values = FloatArray(3)
        for (i in 0..2) {
            values[i] = mAccelerometer.values[i] - mGravity.values[i]
        }

        return DSensorEvent(DSensor.TYPE_DEVICE_LINEAR_ACCELERATION, mAccelerometer.accuracy,
            mAccelerometer.timestamp, values)
    }

    private fun setLinearAcceleration(dSensorEvent: DSensorEvent) {
        mLinearAcceleration?.let {
            it.accuracy = dSensorEvent.accuracy
            it.timestamp = dSensorEvent.timestamp
            for (i in 0..2) {
                it.values[i] = dSensorEvent.values[i]
            }
        }
    }

    private fun processSensorData(dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "processSensorData")
        var changedSensorTypes = 0
        val gravityNorm = calculateNorm(mGravity.values)

        if (mCalculateInclination) {
            calculateAndSetInclination(gravityNorm)

            if (mDSensorTypes and DSensor.TYPE_INCLINATION != 0) {
                mInclination?.let {
                    dProcessedSensorEvent.inclination = DSensorEvent(it)
                    changedSensorTypes = changedSensorTypes or DSensor.TYPE_INCLINATION
                }
            }
        }

        if (mDSensorTypes and DSensor.TYPE_DEVICE_ROTATION != 0) {
            dProcessedSensorEvent.deviceRotation = DSensorEvent(calculateDeviceRotation(gravityNorm))
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_ROTATION
        }

        if (mDSensorTypes and DSensor.TYPE_PITCH != 0) {
            dProcessedSensorEvent.pitch = DSensorEvent(DSensor.TYPE_PITCH, mGravity.accuracy,
                    mGravity.timestamp, floatArrayOf(asin(-mGravity.values[1] / gravityNorm)))
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_PITCH
        }

        if (mDSensorTypes and DSensor.TYPE_ROLL != 0) {
            dProcessedSensorEvent.roll = DSensorEvent(DSensor.TYPE_ROLL, mGravity.accuracy, mGravity.timestamp,
                    floatArrayOf(atan2(-mGravity.values[0] / gravityNorm, mGravity.values[2] / gravityNorm)))
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_ROLL
        }

        return changedSensorTypes
    }

    private fun calculateAndSetInclination(gravityNorm: Float) {
        if (mInclination != null) {
            mInclination.values[0] = acos(mGravity.values[2] / gravityNorm)
            mInclination.accuracy = mGravity.accuracy
            mInclination.timestamp = mGravity.timestamp
        }
    }

    private fun calculateDeviceRotation(gravityNorm: Float): DSensorEvent {
        val deviceRotation = if (mInclination == null || mInclination.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN
            || mInclination.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            Float.NaN
        }
        else {
            atan2(mGravity.values[0] / gravityNorm, mGravity.values[1] / gravityNorm)
        }

        return DSensorEvent(DSensor.TYPE_DEVICE_ROTATION, mInclination?.accuracy ?: 0,
            mInclination?.timestamp ?: 0, floatArrayOf(deviceRotation)
        )
    }

    private fun processSensorDataWithRotationMatrix(dProcessedSensorEvent: DProcessedSensorEvent): Int {
        logger(DSensorEventProcessor::class.java.simpleName, "processSensorDataWithRotationMatrix()")
        var changedSensorTypes = 0

        if (mDSensorTypes and DSensor.TYPE_ROLL != 0) {
            dProcessedSensorEvent.roll = DSensorEvent(DSensor.TYPE_ROLL, mGravity.accuracy, mGravity.timestamp,
                    floatArrayOf(atan2(-mRotationMatrix[6].toDouble(), mRotationMatrix[8].toDouble()).toFloat()))
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_ROLL
        }

        if (mDSensorTypes and DSensor.TYPE_PITCH != 0) {
            dProcessedSensorEvent.pitch = DSensorEvent(DSensor.TYPE_PITCH, mGravity.accuracy,
                    mGravity.timestamp, floatArrayOf(asin((-mRotationMatrix[7]).toDouble()).toFloat()))
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_PITCH
        }

        changedSensorTypes = changedSensorTypes or processWorldBasisDSensorEvent(dProcessedSensorEvent)

        if (mCalculateInclination) {
            calculateAndSetInclination()
            if (mInclination != null) {
                if (mDSensorTypes and DSensor.TYPE_INCLINATION != 0) {
                    dProcessedSensorEvent.inclination = DSensorEvent(mInclination)
                    changedSensorTypes = changedSensorTypes or DSensor.TYPE_INCLINATION
                }

                // Due to noise and numerical limitation, compass field is calculable when inclination < 25 or inclination > 155
                if (mInclination.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN ||
                    mInclination.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
                    changedSensorTypes = changedSensorTypes or processCompassEvents(dProcessedSensorEvent)

                    if (mDSensorTypes and DSensor.TYPE_DEVICE_ROTATION != 0) {
                        dProcessedSensorEvent.deviceRotation = DSensorEvent(DSensor.TYPE_DEVICE_ROTATION,
                                mGravity.accuracy, mGravity.timestamp, floatArrayOf(Float.NaN))
                        changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_ROTATION
                    }
                } else {
                    changedSensorTypes = changedSensorTypes or processCameraDirectionEvents(dProcessedSensorEvent)

                    if (mDSensorTypes and DSensor.TYPE_DEVICE_ROTATION != 0) {
                        dProcessedSensorEvent.deviceRotation = DSensorEvent(DSensor.TYPE_DEVICE_ROTATION,
                                mGravity.accuracy, mGravity.timestamp,
                                floatArrayOf(atan2(mRotationMatrix[6].toDouble(), mRotationMatrix[7].toDouble()).toFloat()))
                        changedSensorTypes = changedSensorTypes or DSensor.TYPE_DEVICE_ROTATION
                    }
                }
            }
        }

        return changedSensorTypes
    }

    private fun calculateAndSetInclination() {
        if (mInclination != null) {
            mInclination.values[0] = acos(mRotationMatrix[8].toDouble()).toFloat()
            mInclination.accuracy = mGravity.accuracy
            mInclination.timestamp = mGravity.timestamp
        }
    }

    private fun processWorldBasisDSensorEvent(dProcessedSensorEvent: DProcessedSensorEvent) : Int {
        var changedSensorTypes = 0
        if (mDSensorTypes and DSensor.TYPE_WORLD_ACCELEROMETER != 0 && mAccelerometerInWorldBasisHistories != null) {
            dProcessedSensorEvent.accelerometerInWorldBasis = getAverageWorldBasisEvent(mAccelerometer,
                mAccelerometerInWorldBasisHistories)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_WORLD_ACCELEROMETER
        }

        if (mDSensorTypes and DSensor.TYPE_WORLD_GRAVITY != 0 && mGravityInWorldBasisHistories != null) {
            dProcessedSensorEvent.gravityInWorldBasis = getAverageWorldBasisEvent(mGravity, mGravityInWorldBasisHistories)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_WORLD_GRAVITY
        }

        if (mDSensorTypes and DSensor.TYPE_WORLD_MAGNETIC_FIELD != 0) {
            val values = productOfSquareMatrixAndVector(mRotationMatrix, mMagneticField.values)
            if (values != null) {
                dProcessedSensorEvent.magneticFieldInWorldBasis = DSensorEvent(DSensor.TYPE_WORLD_MAGNETIC_FIELD,
                    mMagneticField.accuracy, mMagneticField.timestamp, values)
                changedSensorTypes = changedSensorTypes or DSensor.TYPE_WORLD_MAGNETIC_FIELD
            }
        }

        if (!mHasLinearAccelerationSensor && mLinearAcceleration != null && mLinearAccelerationInWorldBasisHistories != null) {
            dProcessedSensorEvent.linearAccelerationInWorldBasis = getAverageWorldBasisEvent(mLinearAcceleration,
                mLinearAccelerationInWorldBasisHistories)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_WORLD_LINEAR_ACCELERATION
        }

        return changedSensorTypes
    }

    private fun getAverageWorldBasisEvent(itemInDeviceBasis: DSensorEvent, worldHistory: WorldHistory) : DSensorEvent {
        worldHistory.add(itemInDeviceBasis)
        return worldHistory.getAverageSensorEvent(itemInDeviceBasis.sensorType)
    }

    private fun processCompassEvents(dProcessedSensorEvent: DProcessedSensorEvent) : Int {
        var changedSensorTypes = 0
        if (mDSensorTypes and DSensor.TYPE_Z_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.zAxisDirection = getDirection(mZAxisDirectionHistories,
                DSensor.TYPE_Z_AXIS_DIRECTION, Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_Z_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_Z_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.minusZAxisDirection = getDirection(mZAxisDirectionHistories,
                DSensor.TYPE_MINUS_Z_AXIS_DIRECTION, Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION != 0 && mYAxisDirectionHistories != null) {
            dProcessedSensorEvent.yAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_Y_AXIS_DIRECTION,
                atan2(mRotationMatrix[1].toDouble(), mRotationMatrix[4].toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_Y_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION != 0 && mMinusYAxisDirectionHistories != null) {
            dProcessedSensorEvent.minusYAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_MINUS_Y_AXIS_DIRECTION,
                atan2((-mRotationMatrix[1]).toDouble(), (-mRotationMatrix[4]).toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_Y_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION != 0 && mXAxisDirectionHistories != null) {
            dProcessedSensorEvent.xAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_X_AXIS_DIRECTION,
                atan2(mRotationMatrix[0].toDouble(), mRotationMatrix[3].toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_X_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_X_AXIS_DIRECTION != 0 && mMinusXAxisDirectionHistories != null) {
            dProcessedSensorEvent.minusXAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_MINUS_X_AXIS_DIRECTION,
                atan2((-mRotationMatrix[0]).toDouble(), (-mRotationMatrix[3]).toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_X_AXIS_DIRECTION
        }

        return changedSensorTypes
    }

    private fun getDirection(directionHistory: DirectionHistory?, dSensorType: Int, directionValue: Float) : DSensorEvent? {
        if (directionHistory != null) {
            if (directionValue.isNaN()) {
                directionHistory.clearHistories()
                return DSensorEvent(dSensorType, 0, 0, floatArrayOf(Float.NaN))
            }

            directionHistory.add(
                DSensorEvent(
                    dSensorType, mGravity.accuracy,
                    if (mGravity.timestamp > mMagneticField.timestamp)
                        mGravity.timestamp
                    else
                        mMagneticField.timestamp,
                    floatArrayOf(directionValue)
                )
            )

            return directionHistory.getAverageSensorEvent(dSensorType)
        }

        return null
    }

    private fun processCameraDirectionEvents(dProcessedSensorEvent: DProcessedSensorEvent) : Int {
        var changedSensorTypes = 0
        if (mDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.yAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_Y_AXIS_DIRECTION,
                Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_Y_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.minusYAxisDirection = getDirection(mYAxisDirectionHistories,
                DSensor.TYPE_MINUS_Y_AXIS_DIRECTION, Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_Y_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.xAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_X_AXIS_DIRECTION,
                Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_X_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_X_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.minusXAxisDirection = getDirection(mYAxisDirectionHistories,
                DSensor.TYPE_MINUS_X_AXIS_DIRECTION, Float.NaN)
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_X_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_Z_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.zAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_Z_AXIS_DIRECTION,
                atan2(mRotationMatrix[2].toDouble(), mRotationMatrix[5].toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_Z_AXIS_DIRECTION
        }

        if (mDSensorTypes and DSensor.TYPE_MINUS_Z_AXIS_DIRECTION != 0) {
            dProcessedSensorEvent.minusZAxisDirection = getDirection(mYAxisDirectionHistories, DSensor.TYPE_Z_AXIS_DIRECTION,
                atan2((-mRotationMatrix[2]).toDouble(), (-mRotationMatrix[5]).toDouble()).toFloat())
            changedSensorTypes = changedSensorTypes or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION
        }

        return changedSensorTypes
    }

    private class DirectionHistory(historyMaxSize: Int) {
        internal val mHistories = LinkedList<DSensorEvent>()
        internal val mHistoriesSum = floatArrayOf(0.0f, 0.0f)
        internal var mHistoryTimeStampSum: Long = 0
        internal val mHistoryMaxSize = historyMaxSize

        fun clearHistories() {
            logger(DirectionHistory::class.java.simpleName, "clearHistories")
            mHistories.clear()
            mHistoriesSum[0] = 0.0f
            mHistoriesSum[1] = 0.0f
        }

        fun add(item: DSensorEvent) {
            logger(DirectionHistory::class.java.simpleName, "add size = " + mHistories.size
                        + " angle = " + round(Math.toDegrees(item.values[0].toDouble())))
            mHistoryTimeStampSum += item.timestamp
            if (mHistories.size == mHistoryMaxSize) {
                val firstTerm = mHistories.removeFirst()
                mHistoryTimeStampSum -= firstTerm.timestamp
                removeAngle(firstTerm.values[0], mHistoriesSum)
            }
            mHistories.addLast(item)
            addAngle(item.values[0], mHistoriesSum)
        }

        fun getAverageSensorEvent(sensorType: Int): DSensorEvent {
            return DSensorEvent(
                sensorType, mHistories.first.accuracy,
                (1.0f / mHistories.size).toLong() * mHistoryTimeStampSum,
                if (mHistories.isEmpty()) floatArrayOf(java.lang.Float.NaN)
                else floatArrayOf(averageAngle(mHistoriesSum, mHistories.size))
            )
        }
    }

    private inner class WorldHistory(historyMaxSize: Int, historyValuesSumLength: Int) {
        internal val mHistories = LinkedList<DSensorEvent>()
        internal var mHistoryTimeStampSum: Long = 0
        internal val mHistoryMaxSize = historyMaxSize
        internal val mHistoriesValuesSum = FloatArray(historyValuesSumLength)

        fun add(itemInDeviceBasis: DSensorEvent) {
            logger(WorldHistory::class.java.simpleName, "add")
            mHistoryTimeStampSum += itemInDeviceBasis.timestamp
            var firstTerm: DSensorEvent? = null
            if (mHistories.size == mHistoryMaxSize) {
                firstTerm = mHistories.removeFirst()
                mHistoryTimeStampSum -= firstTerm!!.timestamp
            }
            val itemInWorldBasisValues = productOfSquareMatrixAndVector(mRotationMatrix, itemInDeviceBasis.values)
            if (itemInWorldBasisValues != null) {
                for (i in mHistoriesValuesSum.indices) {
                    mHistoriesValuesSum[i] += itemInWorldBasisValues[i]
                    if (firstTerm != null) {
                        mHistoriesValuesSum[i] -= firstTerm.values[i]
                    }
                }

                mHistories.addLast(DSensorEvent(
                    itemInDeviceBasis.sensorType, itemInDeviceBasis.accuracy,
                    itemInDeviceBasis.timestamp, itemInWorldBasisValues)
                )
            }
        }

        fun add(sensorType: Int, accuracy: Int, timestamp: Long, valuesInDeviceBasis: FloatArray) {
            logger(WorldHistory::class.java.simpleName, "add")
            mHistoryTimeStampSum += timestamp
            var firstTerm: DSensorEvent? = null
            if (mHistories.size == mHistoryMaxSize) {
                firstTerm = mHistories.removeFirst()
                mHistoryTimeStampSum -= firstTerm!!.timestamp
            }
            val itemInWorldBasisValues = productOfSquareMatrixAndVector(mRotationMatrix, valuesInDeviceBasis)
            if (itemInWorldBasisValues != null) {
                for (i in mHistoriesValuesSum.indices) {
                    mHistoriesValuesSum[i] += itemInWorldBasisValues[i]
                    if (firstTerm != null) {
                        mHistoriesValuesSum[i] -= firstTerm.values[i]
                    }
                }
                mHistories.addLast(DSensorEvent(sensorType, accuracy, timestamp, itemInWorldBasisValues))
            }
        }

        fun getAverageSensorEvent(sensorType: Int): DSensorEvent {
            logger(WorldHistory::class.java.simpleName, "getAverageSensorEvent")
            return DSensorEvent(
                sensorType, mHistories.first.accuracy,
                (1.0f / mHistories.size).toLong() * mHistoryTimeStampSum,
                scaleVector(mHistoriesValuesSum, 1.0f / mHistories.size))
        }
    }

    private inner class PostEventRunnable : Runnable {
        var mChangedSensorTypes: Int = 0
        lateinit var mDProcessedSensorEvent: DProcessedSensorEvent

        override fun run() {
            mDSensorEventListener.onDSensorChanged(mChangedSensorTypes, mDProcessedSensorEvent)
        }
    }

}
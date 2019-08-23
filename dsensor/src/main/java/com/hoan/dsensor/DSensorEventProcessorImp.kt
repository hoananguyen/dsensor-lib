package com.hoan.dsensor

import android.hardware.SensorManager
import android.util.SparseArray
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.interfaces.DSensorEventProcessor
import com.hoan.dsensor.utils.*
import kotlinx.coroutines.*
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2

/**
 * Low pass filter constant.
 * Use to filter linear acceleration from accelerometer values.
 */
private const val ALPHA = .1f
private const val ONE_MINUS_ALPHA = 1 - ALPHA

class DSensorEventProcessorImp(dSensorTypes: Int,
                               dSensorEventListener: DSensorEventListener,
                               hasGravitySensor: Boolean,
                               hasLinearAccelerationSensor: Boolean,
                               historyMaxLength: Int = DEFAULT_HISTORY_SIZE) : DSensorEventProcessor {

    private val mCoroutineScope = CoroutineScope(Dispatchers.Default)

    private val mDSensorEventListener: DSensorEventListener = dSensorEventListener

    private val mRegisteredDSensorTypes = dSensorTypes

    /**
     * map to keep history directions of compass for averaging.
     */
    private val mRegisteredDirectionMap: List<Direction>?

    /**
     * List containing registered world coordinates types and its associate raw sensor.
     * i.e. TYPE_WORLD_COORDINATES_GRAVITY and TYPE_DEVICE_GRAVITY
     */
    private val mRegisteredWorldCoordinatesMap:List<Pair<Int, Int>>?

    private val mSaveDSensorMap = SparseArray<DSensorEvent>(3)

    private lateinit var mRotationMatrix: FloatArray

    /**
     * Property to indicate whether gravity should be calculated i.e device does not gravity sensor
     * and it is required to process other data.
     */
    private val mCalculateGravity: Boolean

    private val mCalculateLinearAcceleration: Boolean

    init {
        mRegisteredDirectionMap = getDirectionHistoryMap(historyMaxLength)
        mRegisteredWorldCoordinatesMap = getRegisteredWorldCoordinatesDSensorMap()
        if (!mRegisteredWorldCoordinatesMap.isNullOrEmpty() || !mRegisteredDirectionMap.isNullOrEmpty()) {
            mRotationMatrix = FloatArray(9)
        }
        val (calculateGravity, calculateLinearAcceleration) = getRequiredCalculationDSensor(hasGravitySensor, hasLinearAccelerationSensor)
        mCalculateGravity = calculateGravity
        mCalculateLinearAcceleration = calculateLinearAcceleration
        setSaveDSensorMap()
    }

    private fun getDirectionHistoryMap(historyMaxLength: Int): List<Direction>? {
        val registeredDirectionList = ArrayList<Direction>(2)
        for (directionType in getDirectionTypes()) {
            if (mRegisteredDSensorTypes and directionType != 0) {
                registeredDirectionList.add(Direction(directionType, historyMaxLength))
            }
        }

        return if (registeredDirectionList.isNotEmpty()) registeredDirectionList else null
    }

    private fun getRegisteredWorldCoordinatesDSensorMap(): List<Pair<Int, Int>>? {
        val registeredWorldCoordinatesList = ArrayList<Pair<Int, Int>>(4)
        for (worldCoordinatesType in getWorldCoordinatesTypes()) {
            if (mRegisteredDSensorTypes and worldCoordinatesType != 0) {
                registeredWorldCoordinatesList.add(Pair(worldCoordinatesType, getRawDSensor(worldCoordinatesType)))
            }
        }

        return if (registeredWorldCoordinatesList.isNotEmpty()) registeredWorldCoordinatesList else null
    }

    private fun getRequiredCalculationDSensor(hasGravitySensor: Boolean,
                                              hasLinearAccelerationSensor: Boolean): Pair<Boolean, Boolean> {
        val calculateLinearAcceleration = !hasLinearAccelerationSensor &&
                (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 ||
                mRegisteredDSensorTypes and TYPE_WORLD_LINEAR_ACCELERATION != 0)
        val calculateGravity = !hasGravitySensor && (mRegisteredDSensorTypes and TYPE_DEVICE_GRAVITY != 0 ||
                !mRegisteredDirectionMap.isNullOrEmpty() || !mRegisteredWorldCoordinatesMap.isNullOrEmpty() ||
                mCalculateLinearAcceleration || mRegisteredDSensorTypes and TYPE_INCLINATION != 0 ||
                mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0)

        return Pair(calculateGravity, calculateLinearAcceleration)
    }

    private fun setSaveDSensorMap() {

        if (mCalculateLinearAcceleration) {
            mSaveDSensorMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
            if (!mCalculateGravity) {
                mSaveDSensorMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            }
        }

        if (mCalculateGravity && mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER] == null) {
            mSaveDSensorMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
        }

        if (!mRegisteredDirectionMap.isNullOrEmpty() || !mRegisteredWorldCoordinatesMap.isNullOrEmpty()) {
            if (mSaveDSensorMap[TYPE_DEVICE_GRAVITY] == null) {
                mSaveDSensorMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 0L, FloatArray(3)))
            }

            mSaveDSensorMap.put(TYPE_DEVICE_MAGNETIC_FIELD, DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 0L, FloatArray(3)))

            mRegisteredWorldCoordinatesMap?.let {
                for (item in it) {
                    if (mSaveDSensorMap[item.second] == null) {
                        mSaveDSensorMap.put(item.second, DSensorEvent(item.second, 0, 0, FloatArray(3)))
                    }
                }
            }
        }
    }

    override fun finish() {
        mCoroutineScope.cancel()
    }

    override fun onDSensorChanged(dSensorEvent: DSensorEvent) {
        runBlocking {
            val resultMap = SparseArray<DSensorEvent>()
            val result = mCoroutineScope.async {
                when (dSensorEvent.sensorType) {
                    TYPE_DEVICE_ACCELEROMETER -> onAccelerometerChanged(dSensorEvent, resultMap)
                    TYPE_DEVICE_GRAVITY -> onGravityChanged(dSensorEvent, resultMap)
                    TYPE_DEVICE_MAGNETIC_FIELD -> onMagneticFieldChanged(dSensorEvent, resultMap)
                    TYPE_DEVICE_LINEAR_ACCELERATION -> onLinearAccelerationChanged(dSensorEvent, resultMap)
                    else -> onDSensorChangedDefaultHandler(dSensorEvent, resultMap)
                }
            }
            val changedSensorTypes = result.await()
            if (changedSensorTypes != 0) {
                mDSensorEventListener.onDSensorChanged(changedSensorTypes, resultMap)
            }
        }
    }

    private fun onAccelerometerChanged(accelerometerEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]?.apply {
            saveDSensorEvent(accelerometerEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(accelerometerEvent, resultMap)
        }

        if (mCalculateGravity) {
            changedSensorTypes = changedSensorTypes or onGravityChanged(calculateGravity(), resultMap, changedSensorTypes)
        }

        return changedSensorTypes
    }

    private fun onGravityChanged(gravityEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>, dChangedSensorTypes: Int = 0): Int {
        var changedSensorTypes = dChangedSensorTypes
        mSaveDSensorMap[TYPE_DEVICE_GRAVITY]?.apply {
            saveDSensorEvent(gravityEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_GRAVITY != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(gravityEvent, resultMap)
        }

        if (mCalculateLinearAcceleration) {
            val linearAccelerationEvent by lazy { calculateLinearAcceleration() }
            mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION]?.apply {
                saveDSensorEvent(linearAccelerationEvent)
            }
            if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
                changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(linearAccelerationEvent, resultMap)
            }
        }

        //if (mRegisteredDirectionMap.size() != 0 || mRegisteredWorldCoordinatesMap.size() != 0)
        if (::mRotationMatrix.isInitialized) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, gravityEvent.values, mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD].values)) {
                changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        } else {
            changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingGravity(resultMap)
        }

        return changedSensorTypes
    }

    private fun onMagneticFieldChanged(magneticFieldEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD]?.apply {
            saveDSensorEvent(magneticFieldEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(magneticFieldEvent, resultMap)
        }

        if (::mRotationMatrix.isInitialized) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mSaveDSensorMap[TYPE_DEVICE_GRAVITY].values, magneticFieldEvent.values)) {
                changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        }

        return changedSensorTypes
    }

    private fun onLinearAccelerationChanged(linearAccelerationEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION]?.apply {
            saveDSensorEvent(linearAccelerationEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(linearAccelerationEvent, resultMap)
        }

        if (::mRotationMatrix.isInitialized && mRegisteredWorldCoordinatesMap?.find { it.first == TYPE_WORLD_LINEAR_ACCELERATION } != null) {
            productOfSquareMatrixAndVector(mRotationMatrix, linearAccelerationEvent.values)?.apply {
                changedSensorTypes = changedSensorTypes or
                        setResultDSensorEventMap(DSensorEvent(TYPE_WORLD_LINEAR_ACCELERATION,
                            linearAccelerationEvent.accuracy, linearAccelerationEvent.timestamp, this.copyOf()), resultMap)
            }
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(
                mRegisteredDirectionMap!![TYPE_WORLD_LINEAR_ACCELERATION].getDirectionDSensorEvent(calculateInclination(), mRotationMatrix), resultMap)
        }

        return changedSensorTypes
    }

    private fun onDSensorChangedDefaultHandler(event: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        return setResultDSensorEventMap(event, resultMap)
    }

    private fun saveDSensorEvent(event: DSensorEvent) {
        val dSensorEvent = mSaveDSensorMap[event.sensorType]
        dSensorEvent.accuracy = event.accuracy
        dSensorEvent.timestamp = event.timestamp
        dSensorEvent.values = event.values.copyOf()
    }

    private fun setResultDSensorEventMap(event: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        resultMap.put(event.sensorType, event.copyOf())
        return event.sensorType
    }

    private fun calculateLinearAcceleration(): DSensorEvent {
        logger(DSensorEventProcessorImp::class.java.simpleName, "calculateLinearAcceleration")
        val accelerometer = mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]
        val gravity = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        val values = FloatArray(3)
        for (i in 0..2) {
            values[i] = accelerometer.values[i] - gravity.values[i]
        }

        return DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, accelerometer.accuracy, accelerometer.timestamp, values)
    }

    private fun processRegisteredDSensorUsingRotationMatrix(resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculatePitch(), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateRoll(), resultMap)
        }

        if (!mRegisteredWorldCoordinatesMap.isNullOrEmpty()) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventListForWorldCoordinatesDSensor(resultMap)
        }

        val inclinationEvent by lazy { calculateInclination() }
        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (!mRegisteredDirectionMap.isNullOrEmpty()) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventForDirection(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventListForDeviceRotation(inclinationEvent, resultMap)
        }

        return changedSensorTypes
    }

    private fun setResultDSensorEventListForWorldCoordinatesDSensor(resultMap: SparseArray<DSensorEvent>) : Int {
        var changedSensorTypes = 0
        mRegisteredWorldCoordinatesMap?.let {
            for (item in it) {
                val dSensorEvent = mSaveDSensorMap[item.second]
                productOfSquareMatrixAndVector(mRotationMatrix, dSensorEvent.values)?.apply {
                    changedSensorTypes = changedSensorTypes or
                            setResultDSensorEventMap(DSensorEvent(item.first, dSensorEvent.accuracy, dSensorEvent.timestamp, this.copyOf()), resultMap)
                }
            }
        }

        return changedSensorTypes
    }

    private fun calculateInclination(): DSensorEvent {
        val gravityDSensorEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_INCLINATION, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp, floatArrayOf(acos(mRotationMatrix[8])))
    }

    private fun setResultDSensorEventForDirection(inclinationEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        mRegisteredDirectionMap?.let {
            for (directionEvent in it) {
                changedSensorTypes = changedSensorTypes or
                        setResultDSensorEventMap(directionEvent.getDirectionDSensorEvent(inclinationEvent, mRotationMatrix), resultMap)
            }
        }

        return changedSensorTypes
    }

    private fun setResultDSensorEventListForDeviceRotation(inclinationEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclinationEvent.values[0]> ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            setResultDSensorEventMap(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(Float.NaN)), resultMap)
        } else {
            setResultDSensorEventMap(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(atan2(mRotationMatrix[6], mRotationMatrix[7]))), resultMap)
        }

        return TYPE_DEVICE_ROTATION
    }

    private fun processRegisteredDSensorUsingGravity(resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        val gravityNorm by lazy { calculateNorm(mSaveDSensorMap[TYPE_DEVICE_GRAVITY].values) }
        val inclinationEvent by lazy { calculateInclination(gravityNorm)}

        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateDeviceRotation(gravityNorm, inclinationEvent), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculatePitch(gravityNorm), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateRoll(gravityNorm), resultMap)
        }

        return changedSensorTypes
    }

    private fun calculateInclination(gravityNorm: Float): DSensorEvent {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_INCLINATION, gravityEvent.accuracy, gravityEvent.timestamp, floatArrayOf(gravityEvent.values[2] / gravityNorm))
    }

    private fun calculateRoll(): DSensorEvent {
        val gravity = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_ROLL, gravity.accuracy, gravity.timestamp, floatArrayOf(atan2(-mRotationMatrix[6], mRotationMatrix[8])))
    }

    private fun calculateRoll(gravityNorm: Float): DSensorEvent {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_ROLL, gravityEvent.accuracy, gravityEvent.timestamp,
            floatArrayOf(atan2(-gravityEvent.values[0] / gravityNorm, gravityEvent.values[2] / gravityNorm)))
    }

    private fun calculatePitch(gravityNorm: Float): DSensorEvent {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_PITCH, gravityEvent.accuracy, gravityEvent.timestamp,
            floatArrayOf(asin(-gravityEvent.values[1] / gravityNorm)))
    }

    private fun calculatePitch(): DSensorEvent {
        val gravity = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_PITCH, gravity.accuracy, gravity.timestamp, floatArrayOf(asin(-mRotationMatrix[7])))
    }

    private fun calculateDeviceRotation(gravityNorm: Float, inclinationEvent: DSensorEvent): DSensorEvent {
        val deviceRotation = if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN
            ||inclinationEvent.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            Float.NaN
        }
        else {
            atan2(mSaveDSensorMap[TYPE_DEVICE_GRAVITY].values[0] / gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY].values[1] / gravityNorm)
        }

        return DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp, floatArrayOf(deviceRotation))
    }

    private fun calculateGravity(): DSensorEvent {
        val accelerometer = mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER)
        val gravity = mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY)
        if (gravity.timestamp == 0L) {
            gravity.values = accelerometer.values.copyOf()
        } else {
            for (i in 0..2) {
                gravity.values[i] = ALPHA * accelerometer.values[i] + ONE_MINUS_ALPHA * gravity.values[i]
            }
        }

        gravity.accuracy = accelerometer.accuracy
        gravity.timestamp = accelerometer.timestamp

        return gravity
    }

    private class Direction(val dDirectionSensorType: Int, historyMaxLength: Int = DEFAULT_HISTORY_SIZE) {
        val rotationMatrixIndices: Pair<Int, Int> = getRotationMatrixIndices(dDirectionSensorType)
        val isPositive: Boolean = isPositive(dDirectionSensorType)
        val isCamera: Boolean = isCamera(dDirectionSensorType)
        val directionHistory = DirectionHistory(dDirectionSensorType, historyMaxLength)

        fun getDirectionDSensorEvent(inclination: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
            return when {
                (inclination.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclination.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) && isCamera -> {
                    directionHistory.clearHistories()
                    DSensorEvent(dDirectionSensorType, inclination.accuracy, inclination.timestamp, floatArrayOf(Float.NaN))
                }
                else -> {
                    directionHistory.add(DSensorEvent(dDirectionSensorType, inclination.accuracy, inclination.timestamp,
                        floatArrayOf(getDirection(rotationMatrix))))
                    directionHistory.getAverageSensorEvent()
                }
            }
        }

        private fun getDirection(rotationMatrix: FloatArray): Float {
            return when (isPositive) {
                true -> atan2(rotationMatrix[rotationMatrixIndices.first], rotationMatrix[rotationMatrixIndices.second])
                else -> atan2(-rotationMatrix[rotationMatrixIndices.first], -rotationMatrix[rotationMatrixIndices.second])
            }
        }

        companion object {
            fun getRotationMatrixIndices(dSensorTypes: Int): Pair<Int, Int> {
                return when (dSensorTypes) {
                    TYPE_X_AXIS_DIRECTION, TYPE_NEGATIVE_X_AXIS_DIRECTION -> Pair(0, 3)
                    TYPE_Y_AXIS_DIRECTION, TYPE_NEGATIVE_Y_AXIS_DIRECTION -> Pair(1, 4)
                    TYPE_Z_AXIS_DIRECTION, TYPE_NEGATIVE_Z_AXIS_DIRECTION -> Pair(2, 5)
                    else -> Pair(0, 0)
                }
            }

            fun isPositive(dSensorTypes: Int): Boolean {
                return when (dSensorTypes) {
                    TYPE_X_AXIS_DIRECTION, TYPE_Y_AXIS_DIRECTION, TYPE_Z_AXIS_DIRECTION -> true
                    else -> false
                }
            }

            fun isCamera(dSensorTypes: Int): Boolean {
                return when (dSensorTypes) {
                    TYPE_Z_AXIS_DIRECTION, TYPE_NEGATIVE_Z_AXIS_DIRECTION -> true
                    else -> false
                }
            }
        }
    }
}
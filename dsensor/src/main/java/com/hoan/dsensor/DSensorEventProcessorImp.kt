package com.hoan.dsensor

import android.hardware.SensorManager
import android.util.SparseArray
import androidx.collection.SparseArrayCompat
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.interfaces.DSensorEventProcessor
import com.hoan.dsensor.utils.*
import kotlinx.coroutines.*
import kotlin.math.atan2
import kotlin.system.measureTimeMillis

class DSensorEventProcessorImp(dSensorTypes: Int,
                               dSensorEventListener: DSensorEventListener,
                               hasGravitySensor: Boolean = true,
                               hasLinearAccelerationSensor: Boolean = true,
                               historyMaxLength: Int = DEFAULT_HISTORY_SIZE) : DSensorEventProcessor {

    private lateinit var mOnDSensorChangedResult: Deferred<Int>

    private val mDSensorEventListener: DSensorEventListener = dSensorEventListener

    private val mRegisteredDSensorTypes = dSensorTypes

    /**
     * list to keep directions of compass for averaging.
     */
    private lateinit var mRegisteredDirectionList: List<Direction>

    /**
     * List containing registered world coordinates types and its associate raw sensor.
     * i.e. TYPE_WORLD_COORDINATES_GRAVITY and TYPE_DEVICE_GRAVITY
     */
    private lateinit var mRegisteredWorldCoordinatesList:List<Pair<Int, Int>>

    private lateinit var mSaveDSensorMap: SparseArrayCompat<DSensorEvent>

    private lateinit var mRotationMatrix: FloatArray

    /**
     * Property to indicate whether gravity should be calculated i.e device does not gravity sensor
     * and it is required to process other data.
     */
    private val mCalculateGravity: Boolean

    private val mCalculateLinearAcceleration: Boolean

    init {
        getRegisteredDirectionList(dSensorTypes, historyMaxLength)?.let {
            mRegisteredDirectionList = it
        }
        getRegisteredWorldCoordinatesDSensorList(dSensorTypes)?.let {
            mRegisteredWorldCoordinatesList = it
        }
        if (::mRegisteredWorldCoordinatesList.isInitialized || ::mRegisteredDirectionList.isInitialized) {
            mRotationMatrix = FloatArray(9)
        }
        val (calculateGravity, calculateLinearAcceleration) = getRequiredCalculationDSensor(dSensorTypes, hasGravitySensor, hasLinearAccelerationSensor)
        mCalculateGravity = calculateGravity
        mCalculateLinearAcceleration = calculateLinearAcceleration
        getSaveDSensorMap()?.let {
            mSaveDSensorMap = it
        }
    }

    private fun getRegisteredDirectionList(dSensorTypes: Int, historyMaxLength: Int = DEFAULT_HISTORY_SIZE): List<Direction>? {
        val registeredDirectionList = ArrayList<Direction>(2)
        for (directionType in getDirectionTypes()) {
            if (dSensorTypes and directionType != 0) {
                registeredDirectionList.add(Direction(directionType, historyMaxLength))
            }
        }

        return if (registeredDirectionList.isNotEmpty()) registeredDirectionList else null
    }

    private fun getRegisteredWorldCoordinatesDSensorList(dSensorTypes: Int): List<Pair<Int, Int>>? {
        val registeredWorldCoordinatesList = ArrayList<Pair<Int, Int>>(4)
        for (worldCoordinatesType in getWorldCoordinatesTypes()) {
            if (dSensorTypes and worldCoordinatesType != 0) {
                registeredWorldCoordinatesList.add(Pair(worldCoordinatesType, getRawDSensor(worldCoordinatesType)))
            }
        }

        return if (registeredWorldCoordinatesList.isNotEmpty()) registeredWorldCoordinatesList else null
    }

    private fun getRequiredCalculationDSensor(dSensorTypes: Int,
                                              hasGravitySensor: Boolean,
                                              hasLinearAccelerationSensor: Boolean): Pair<Boolean, Boolean> {
        val calculateLinearAcceleration = !hasLinearAccelerationSensor &&
                (dSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 || dSensorTypes and TYPE_WORLD_LINEAR_ACCELERATION != 0)
        val calculateGravity = !hasGravitySensor && (dSensorTypes and TYPE_DEVICE_GRAVITY != 0 || calculateLinearAcceleration ||
                ::mRotationMatrix.isInitialized || dSensorTypes and TYPE_INCLINATION != 0 || dSensorTypes and TYPE_DEVICE_ROTATION != 0 ||
                dSensorTypes and TYPE_PITCH != 0 || dSensorTypes and TYPE_ROLL != 0)

        return Pair(calculateGravity, calculateLinearAcceleration)
    }

    private fun getSaveDSensorMap(): SparseArrayCompat<DSensorEvent>? {
        val resultMap = SparseArrayCompat<DSensorEvent>(3)

        if (mCalculateLinearAcceleration) {
            resultMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
            if (!mCalculateGravity) {
                resultMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            }
        }

        if (mCalculateGravity && resultMap[TYPE_DEVICE_ACCELEROMETER] == null) {
            resultMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
        }

        if (::mRotationMatrix.isInitialized) {
            if (resultMap[TYPE_DEVICE_GRAVITY] == null) {
                resultMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 0L, FloatArray(3)))
            }

            resultMap.put(TYPE_DEVICE_MAGNETIC_FIELD, DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 0L, FloatArray(3)))

            if (::mRegisteredWorldCoordinatesList.isInitialized) {
                for (item in mRegisteredWorldCoordinatesList) {
                    if (resultMap[item.second] == null) {
                        resultMap.put(item.second, DSensorEvent(item.second, 0, 0, FloatArray(3)))
                    }
                }
            }
        }

        return if (resultMap.size() == 0) null else resultMap
    }

    override fun finish() {
        logger("DSensorEventProcessorImp", "finish")
        if (::mOnDSensorChangedResult.isInitialized) {
            CoroutineScope(Dispatchers.Default).launch {
                mOnDSensorChangedResult.cancelAndJoin()
            }
        }
        //mCoroutineScope.cancel()
    }

    override fun onDSensorChanged(dSensorEvent: DSensorEvent) {
        val time = measureTimeMillis {
            runBlocking {
                val resultMap = SparseArray<DSensorEvent>()
                coroutineScope {
                    mOnDSensorChangedResult = when (dSensorEvent.sensorType) {
                        TYPE_DEVICE_ACCELEROMETER -> async { onAccelerometerChanged(dSensorEvent, resultMap) }
                        TYPE_DEVICE_GRAVITY -> async { onGravityChanged(dSensorEvent, resultMap) }
                        TYPE_DEVICE_MAGNETIC_FIELD -> async { onMagneticFieldChanged(dSensorEvent, resultMap) }
                        TYPE_DEVICE_LINEAR_ACCELERATION -> async { onLinearAccelerationChanged(dSensorEvent, resultMap) }
                        else -> async { onDSensorChangedDefaultHandler(dSensorEvent, resultMap) }
                    }
                }
                val changedSensorTypes = mOnDSensorChangedResult.await()
                if (changedSensorTypes != 0) {
                    mDSensorEventListener.onDSensorChanged(changedSensorTypes, resultMap)
                }
            }
        }
        logger("DSensorEventProcessorImp", "onDSensorChanged: type = ${dSensorEvent.sensorType} done in $time")
    }

    private fun onAccelerometerChanged(accelerometerEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER] != null) {
            saveDSensorEvent(accelerometerEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(accelerometerEvent, resultMap)
        }

        if (mCalculateGravity) {
            changedSensorTypes = changedSensorTypes or
                    onGravityChanged(calculateGravity(mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!),
                        resultMap, changedSensorTypes)
        }

        return changedSensorTypes
    }

    private fun onGravityChanged(gravityEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>, dChangedSensorTypes: Int = 0): Int {
        var changedSensorTypes = dChangedSensorTypes
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_GRAVITY] != null) {
            saveDSensorEvent(gravityEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_GRAVITY != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(gravityEvent, resultMap)
        }

        if (mCalculateLinearAcceleration) {
            val linearAccelerationEvent by lazy { calculateLinearAcceleration(mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!) }
            mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION]?.apply {
                saveDSensorEvent(linearAccelerationEvent)
            }
            if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
                changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(linearAccelerationEvent, resultMap)
            }
        }

        if (::mRotationMatrix.isInitialized) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, gravityEvent.values, mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD]!!.values)) {
                changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        } else {
            changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingGravity(resultMap)
        }

        return changedSensorTypes
    }

    private fun onMagneticFieldChanged(magneticFieldEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD] != null) {
            saveDSensorEvent(magneticFieldEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(magneticFieldEvent, resultMap)
        }

        if (::mRotationMatrix.isInitialized) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.values, magneticFieldEvent.values)) {
                changedSensorTypes = changedSensorTypes or processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        }

        return changedSensorTypes
    }

    private fun onLinearAccelerationChanged(linearAccelerationEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION] != null) {
            saveDSensorEvent(linearAccelerationEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(linearAccelerationEvent, resultMap)
        }

        if (::mRegisteredWorldCoordinatesList.isInitialized && mRegisteredWorldCoordinatesList.find { it.first == TYPE_WORLD_LINEAR_ACCELERATION } != null) {
            productOfSquareMatrixAndVector(mRotationMatrix, linearAccelerationEvent.values)?.apply {
                changedSensorTypes = changedSensorTypes or
                        setResultDSensorEventMap(DSensorEvent(TYPE_WORLD_LINEAR_ACCELERATION,
                            linearAccelerationEvent.accuracy, linearAccelerationEvent.timestamp, this.copyOf()), resultMap)
            }
        }

        return changedSensorTypes
    }

    private fun onDSensorChangedDefaultHandler(event: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        return setResultDSensorEventMap(event, resultMap)
    }

    private fun saveDSensorEvent(event: DSensorEvent) {
        mSaveDSensorMap[event.sensorType]?.apply {
            accuracy = event.accuracy
            timestamp = event.timestamp
            values = event.values.copyOf()
        }
    }

    private fun setResultDSensorEventMap(event: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        resultMap.put(event.sensorType, event.copyOf())
        return event.sensorType
    }

    private fun processRegisteredDSensorUsingRotationMatrix(resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculatePitch(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateRoll(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix), resultMap)
        }

        if (::mRegisteredWorldCoordinatesList.isInitialized) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventListForWorldCoordinatesDSensor(resultMap)
        }

        val inclinationEvent by lazy { calculateInclination(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix) }
        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (::mRegisteredDirectionList.isInitialized) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventForDirection(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventListForDeviceRotation(inclinationEvent, resultMap)
        }

        return changedSensorTypes
    }

    private fun setResultDSensorEventListForWorldCoordinatesDSensor(resultMap: SparseArray<DSensorEvent>) : Int {
        var changedSensorTypes = 0
        for (item in mRegisteredWorldCoordinatesList) {
            transformToWorldCoordinate(item.first, mSaveDSensorMap[item.second]!!, mRotationMatrix)?.let {
                changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(it, resultMap)
            }
        }

        return changedSensorTypes
    }

    private fun setResultDSensorEventForDirection(inclinationEvent: DSensorEvent, resultMap: SparseArray<DSensorEvent>): Int {
        var changedSensorTypes = 0
        for (direction in mRegisteredDirectionList) {
            changedSensorTypes = changedSensorTypes or
                    setResultDSensorEventMap(direction.getDirectionDSensorEvent(inclinationEvent, mRotationMatrix), resultMap)
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
        val gravityNorm by lazy { calculateNorm(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.values) }
        val inclinationEvent by lazy { calculateInclination(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!)}

        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateDeviceRotation(gravityNorm, inclinationEvent,
                mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculatePitch(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            changedSensorTypes = changedSensorTypes or setResultDSensorEventMap(calculateRoll(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }

        return changedSensorTypes
    }
}
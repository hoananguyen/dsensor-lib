package com.hoan.dsensor

import android.hardware.SensorManager
import androidx.collection.SparseArrayCompat
import com.hoan.dsensor.interfaces.DSensorEventProcessor
import com.hoan.dsensor.utils.calculateNorm
import com.hoan.dsensor.utils.logger
import com.hoan.dsensor.utils.productOfSquareMatrixAndVector
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.measureTimeMillis

class DSensorEventProcessorImp(dSensorTypes: Int,
                               hasGravitySensor: Boolean = true,
                               hasLinearAccelerationSensor: Boolean = true,
                               historyMaxLength: Int = DEFAULT_HISTORY_SIZE) : DSensorEventProcessor {

    private var mDSensorData = DSensorData()

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

    private var mIsCancelled = AtomicBoolean(false)

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

        if (mCalculateGravity) {
            resultMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            if (resultMap[TYPE_DEVICE_ACCELEROMETER] == null) {
                resultMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
            }
        }

        if (::mRotationMatrix.isInitialized) {
            if (resultMap[TYPE_DEVICE_GRAVITY] == null) {
                resultMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            }

            resultMap.put(TYPE_DEVICE_MAGNETIC_FIELD, DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD))

            if (::mRegisteredWorldCoordinatesList.isInitialized) {
                for (item in mRegisteredWorldCoordinatesList) {
                    if (resultMap[item.second] == null) {
                        resultMap.put(item.second, DSensorEvent(item.second))
                    }
                }
            }
        }

        return if (resultMap.size() == 0) null else resultMap
    }

    fun getSensorData(): DSensorData {
        return mDSensorData
    }

    override fun finish() {
        logger("DSensorEventProcessorImp", "finish")
        mIsCancelled.set(true)
    }

    override fun onDSensorChanged(dSensorEvent: DSensorEvent) {
        val time = measureTimeMillis {
            val resultMap = SparseArrayCompat<DSensorEvent>()
            when (dSensorEvent.sensorType) {
                TYPE_DEVICE_ACCELEROMETER -> onAccelerometerChanged(dSensorEvent, resultMap)
                TYPE_DEVICE_GRAVITY -> onGravityChanged(dSensorEvent, resultMap)
                TYPE_DEVICE_MAGNETIC_FIELD -> onMagneticFieldChanged(dSensorEvent, resultMap)
                TYPE_DEVICE_LINEAR_ACCELERATION -> onLinearAccelerationChanged(dSensorEvent, resultMap)
                else -> setResultDSensorEventMap(dSensorEvent, resultMap)
            }

            if (!resultMap.isEmpty && !mIsCancelled.get()) {
                mDSensorData.data = resultMap
            }
        }
        logger("DSensorEventProcessorImp", "onDSensorChanged: type = ${dSensorEvent.sensorType} done in $time")
    }

    private fun onAccelerometerChanged(accelerometerEvent: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER] != null) {
            saveDSensorEvent(accelerometerEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0) {
            setResultDSensorEventMap(accelerometerEvent, resultMap)
        }

        if (mCalculateGravity) {
            onGravityChanged(calculateGravity(mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        } else if (mCalculateLinearAcceleration && mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.isValuesInitialized()) {
            val linearAccelerationEvent = calculateLinearAcceleration(mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!)
            mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION]?.apply {
                saveDSensorEvent(linearAccelerationEvent)
            }
            if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
                setResultDSensorEventMap(linearAccelerationEvent, resultMap)
            }
        }
    }

    private fun onGravityChanged(gravityEvent: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_GRAVITY] != null) {
            saveDSensorEvent(gravityEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_GRAVITY != 0) {
            setResultDSensorEventMap(gravityEvent, resultMap)
        }

        if (mCalculateLinearAcceleration && mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!.isValuesInitialized()) {
            val linearAccelerationEvent by lazy { calculateLinearAcceleration(mSaveDSensorMap[TYPE_DEVICE_ACCELEROMETER]!!, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!) }
            mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION]?.apply {
                saveDSensorEvent(linearAccelerationEvent)
            }
            if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
                setResultDSensorEventMap(linearAccelerationEvent, resultMap)
            }
        }

        if (::mRotationMatrix.isInitialized && mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD]!!.isValuesInitialized()) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, gravityEvent.values, mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD]!!.values)) {
                processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        } else {
            processRegisteredDSensorUsingGravity(resultMap)
        }
    }

    private fun onMagneticFieldChanged(magneticFieldEvent: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD] != null) {
            saveDSensorEvent(magneticFieldEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0) {
            setResultDSensorEventMap(magneticFieldEvent, resultMap)
        }

        if (::mRotationMatrix.isInitialized && mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.isValuesInitialized()) {
            if (SensorManager.getRotationMatrix(mRotationMatrix, null, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.values, magneticFieldEvent.values)) {
                processRegisteredDSensorUsingRotationMatrix(resultMap)
            }
        }
    }

    private fun onLinearAccelerationChanged(linearAccelerationEvent: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        if (::mSaveDSensorMap.isInitialized && mSaveDSensorMap[TYPE_DEVICE_LINEAR_ACCELERATION] != null) {
            saveDSensorEvent(linearAccelerationEvent)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0) {
            setResultDSensorEventMap(linearAccelerationEvent, resultMap)
        }

        if (::mRegisteredWorldCoordinatesList.isInitialized &&
                mRegisteredWorldCoordinatesList.find { it.first == TYPE_WORLD_LINEAR_ACCELERATION } != null &&
                mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.isValuesInitialized() && mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD]!!.isValuesInitialized()) {
            productOfSquareMatrixAndVector(mRotationMatrix, linearAccelerationEvent.values)?.apply {
                setResultDSensorEventMap(DSensorEvent(TYPE_WORLD_LINEAR_ACCELERATION, linearAccelerationEvent.accuracy,
                    linearAccelerationEvent.timestamp, this.copyOf()), resultMap)
            }
        }
    }

    private fun saveDSensorEvent(event: DSensorEvent) {
        mSaveDSensorMap[event.sensorType]?.apply {
            accuracy = event.accuracy
            timestamp = event.timestamp
            values = event.values.copyOf()
        }
    }

    private fun setResultDSensorEventMap(event: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        resultMap.put(event.sensorType, event.copyOf())
    }

    private fun processRegisteredDSensorUsingRotationMatrix(resultMap: SparseArrayCompat<DSensorEvent>) {
        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            setResultDSensorEventMap(calculatePitch(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            setResultDSensorEventMap(calculateRoll(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix), resultMap)
        }

        if (::mRegisteredWorldCoordinatesList.isInitialized) {
            setResultDSensorEventListForWorldCoordinatesDSensor(resultMap)
        }

        val inclinationEvent by lazy { calculateInclination(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!, mRotationMatrix) }
        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (::mRegisteredDirectionList.isInitialized) {
            setResultDSensorEventForDirection(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            setResultDSensorEventMap(calculateDeviceRotation(inclinationEvent, mRotationMatrix), resultMap)
        }
    }

    private fun setResultDSensorEventListForWorldCoordinatesDSensor(resultMap: SparseArrayCompat<DSensorEvent>) {
        for (item in mRegisteredWorldCoordinatesList) {
            if (mSaveDSensorMap[item.second]!!.isValuesInitialized()) {
                transformToWorldCoordinate(item.first, mSaveDSensorMap[item.second]!!, mRotationMatrix)?.let {
                    setResultDSensorEventMap(it, resultMap)
                }
            }
        }
    }

    private fun setResultDSensorEventForDirection(inclinationEvent: DSensorEvent, resultMap: SparseArrayCompat<DSensorEvent>) {
        for (direction in mRegisteredDirectionList) {
            setResultDSensorEventMap(direction.getDirectionDSensorEvent(inclinationEvent, mRotationMatrix), resultMap)
        }
    }

    private fun processRegisteredDSensorUsingGravity(resultMap: SparseArrayCompat<DSensorEvent>) {
        val gravityNorm by lazy { calculateNorm(mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!.values) }
        val inclinationEvent by lazy { calculateInclination(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!)}

        if (mRegisteredDSensorTypes and TYPE_INCLINATION != 0) {
            setResultDSensorEventMap(inclinationEvent, resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_DEVICE_ROTATION != 0) {
            setResultDSensorEventMap(calculateDeviceRotation(gravityNorm, inclinationEvent, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_PITCH != 0) {
            setResultDSensorEventMap(calculatePitch(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }

        if (mRegisteredDSensorTypes and TYPE_ROLL != 0) {
            setResultDSensorEventMap(calculateRoll(gravityNorm, mSaveDSensorMap[TYPE_DEVICE_GRAVITY]!!), resultMap)
        }
    }
}
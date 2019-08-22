package com.hoan.dsensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.util.SparseArray
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.interfaces.DSensorEventProcessor
import com.hoan.dsensor.utils.*
import kotlinx.coroutines.*
import java.lang.reflect.Type
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction0
import kotlin.reflect.KType
import kotlin.reflect.jvm.javaType

/**
 * Low pass filter constant.
 * Use to filter linear acceleration from accelerometer values.
 */
private const val ALPHA = .1f
private const val ONE_MINUS_ALPHA = 1 - ALPHA

private const val DIRECTION_HISTORY = "direction history"
private const val METHOD = "method"

class DSensorEventProcessorImpOld(dSensorEventListener: DSensorEventListener) : DSensorEventProcessor {

    private val mCoroutineScope = CoroutineScope(Dispatchers.Default)

    private val mDSensorEventListener: DSensorEventListener = dSensorEventListener

    /**
     * map to keep history directions of compass for averaging.
     */
    private val mDirectionHistoryMap = SparseArray<DirectionHistory>()

    private val mFlatDirectionFunctionMappingMap = SparseArray<KFunction0<Float>>()

    private val mNonFlatDirectionFunctionMappingMap = SparseArray<KFunction0<Float>>()

    private val mRegisteredWorldCoordinatesDSensorMap = SparseArray<Int>()

    private lateinit var mSaveDSensorMap: SparseArray<DSensorEvent>

    private val mRotationMatrix = FloatArray(9)

    init {

    }

    fun setUp(dSensorTypes: Int, hasGravitySensor: Boolean, hasLinearAccelerationSensor: Boolean,
                       historyMaxSize: Int) {
        setDirectionHistoryMap(dSensorTypes, historyMaxSize)
        setRegisteredWorldCoordinatesDSensorMap(dSensorTypes)
        val (isGravityCalculationRequired, isLinearAccelerationCalculationRequired) =
            setRequiredCalculationDSensor(dSensorTypes, hasGravitySensor, hasLinearAccelerationSensor)
        val otherRegisteredDSensorList = getRegisteredDSensorList(dSensorTypes)
        setSaveDSensorMap(isGravityCalculationRequired, isLinearAccelerationCalculationRequired)
        setOnSensorChangedHandler(otherRegisteredDSensorList, isGravityCalculationRequired, isLinearAccelerationCalculationRequired)
    }

    private fun setDirectionHistoryMap(dSensorTypes: Int, historyMaxLength: Int) {
        val directionTypes = getDirectionTypes()
        for (directionType in directionTypes) {
            if (dSensorTypes and directionType != 0) {
                mDirectionHistoryMap.put(directionType, DirectionHistory(directionType, historyMaxLength))
            }
        }
    }

    private fun setRegisteredWorldCoordinatesDSensorMap(dSensorTypes: Int) {
        for (worldCoordinatesType in getWorldCoordinatesTypes()) {
            if (dSensorTypes and worldCoordinatesType != 0) {
                val dAssociateSensorType = getRawDSensor(worldCoordinatesType)
                if (dAssociateSensorType != ERROR_UNSUPPORTED_TYPE) {
                    mRegisteredWorldCoordinatesDSensorMap.put(worldCoordinatesType, dAssociateSensorType)
                }
            }
        }
    }

    private fun getRegisteredDSensorList(dSensorTypes: Int): List<Int> {
        val dSensorList = getDSensorList().filterNot { mDirectionHistoryMap.get(it) != null }
            .filterNot { mRegisteredWorldCoordinatesDSensorMap.get(it) != null }
        val registeredDSensorList = ArrayList<Int>()
        for (dSensor in dSensorList) {
            if (dSensorTypes and dSensor != 0) {
                registeredDSensorList.add(dSensor)
            }
        }
        return registeredDSensorList
    }

    private fun setRequiredCalculationDSensor(dSensorTypes: Int, hasGravitySensor: Boolean,
                                              hasLinearAccelerationSensor: Boolean): Pair<Boolean, Boolean> {
        val isLinearAccelerationCalculationRequired = !hasLinearAccelerationSensor &&
                (dSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 ||
                dSensorTypes and TYPE_WORLD_LINEAR_ACCELERATION != 0)
        val isGravityCalculationRequired = !hasGravitySensor && (dSensorTypes and TYPE_DEVICE_GRAVITY != 0 ||
                mDirectionHistoryMap.size() != 0 || mRegisteredWorldCoordinatesDSensorMap.size() != 0 ||
                isLinearAccelerationCalculationRequired || dSensorTypes and TYPE_INCLINATION != 0 ||
                dSensorTypes and TYPE_DEVICE_ROTATION != 0)
        return Pair(isGravityCalculationRequired, isLinearAccelerationCalculationRequired)
    }

    private fun setSaveDSensorMap(isGravityCalculationRequired: Boolean,
                                  isLinearAccelerationCalculationRequired: Boolean) {

        if (isLinearAccelerationCalculationRequired) {
            mSaveDSensorMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
            if (!isGravityCalculationRequired) {
                mSaveDSensorMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            }
        }

        if (isGravityCalculationRequired && mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER) == null) {
            mSaveDSensorMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
        }

        if (mDirectionHistoryMap.size() != 0 || mRegisteredWorldCoordinatesDSensorMap.size() != 0) {
            if (mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY) == null) {
                mSaveDSensorMap.put(TYPE_DEVICE_GRAVITY, DSensorEvent(TYPE_DEVICE_GRAVITY))
            }
            mSaveDSensorMap.put(TYPE_DEVICE_MAGNETIC_FIELD, DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD))

            if (mRegisteredWorldCoordinatesDSensorMap.get(TYPE_WORLD_ACCELEROMETER) == null &&
                mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER) == null) {
                mSaveDSensorMap.put(TYPE_DEVICE_ACCELEROMETER, DSensorEvent(TYPE_DEVICE_ACCELEROMETER))
            }
        }
    }

    private fun setOnSensorChangedHandler(registeredDSensorList: List<Int>,
                                          isGravityCalculationRequired: Boolean,
                                          isLinearAccelerationCalculationRequired: Boolean) {
        mCoroutineScope.launch {
            setOnGravityChangedHandler(registeredDSensorList, isLinearAccelerationCalculationRequired)
        }
    }

    private fun setOnGravityChangedHandler(registeredDSensorList: List<Int>,
                                           isLinearAccelerationCalculationRequired: Boolean) {
        val builderList = ArrayList<LambdaBuilderInfo>()
        createOnGravityChangedBuilderList(registeredDSensorList, isLinearAccelerationCalculationRequired, builderList)
    }

    private fun createHandlerLambda(builderList: MutableList<LambdaBuilderInfo>):
                (DSensorEvent, List<DSensorEvent>) -> Int {
        val onChangedLambda = onChangedLambda@ { event: DSensorEvent, changedEventList: List<DSensorEvent> ->
            var changedDSensorTypes = 0
            var dSensorEvent = event
            for (item in builderList) {
                when (item.getNumberOfParameters()) {
                    0 -> {
                        when (item.getJavaReturnType()) {
                            Unit::class.java -> (item.function as KFunction0<Unit>)()
                            Int::class.java -> changedDSensorTypes = changedDSensorTypes or (item.function as KFunction0<Int>)()
                            Boolean::class.java -> if (!(item.function as KFunction0<Boolean>)()) return@onChangedLambda changedDSensorTypes
                        }
                    }
                }
            }
            changedDSensorTypes
        }
        return onChangedLambda
    }

    private fun createOnGravityChangedBuilderList(registeredDSensorList: List<Int>,
                                               isLinearAccelerationCalculationRequired: Boolean,
                                               builderList: MutableList<LambdaBuilderInfo>) {
        if (mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY) != null) {
            builderList.add(LambdaBuilderInfo(::saveDSensorEvent, false))
        }
        if (registeredDSensorList.contains(TYPE_DEVICE_GRAVITY)) {
            builderList.add(LambdaBuilderInfo(::setResultDSensorEventList, true))
        }

        if (isLinearAccelerationCalculationRequired && mRegisteredWorldCoordinatesDSensorMap.get(TYPE_WORLD_LINEAR_ACCELERATION) == null) {
            builderList.add(LambdaBuilderInfo(::calculateLinearAccelerationAndSetResultDSensorEventList, true))
        }

        if (mDirectionHistoryMap.size() != 0 || mRegisteredWorldCoordinatesDSensorMap.size() != 0) {
            builderList.add(LambdaBuilderInfo(::setRotationMatrix, false))
            setBuilderListForDSensorRequiredRotationMatrix(registeredDSensorList, builderList)
        } else {
            if (registeredDSensorList.contains(TYPE_PITCH)) {
                builderList.add(LambdaBuilderInfo(::calculatePitchUsingGravityAndSetResultDSensorEventList, true))
            }
            if (registeredDSensorList.contains(TYPE_ROLL)) {
                builderList.add(LambdaBuilderInfo(::calculateRollUsingGravityAndSetResultDSensorEventList, true))
            }
            if (registeredDSensorList.contains(TYPE_DEVICE_ROTATION)) {
                mSaveDSensorMap.put(TYPE_INCLINATION, calculateInclinationUsingGravity())
                if (registeredDSensorList.contains(TYPE_INCLINATION)) {
                    builderList.add(LambdaBuilderInfo(::setResultDSensorEventForInclination, true))
                }
                builderList.add(LambdaBuilderInfo(::setResultDSensorEventListForDeviceRotation, true))
            } else if (registeredDSensorList.contains(TYPE_INCLINATION)) {
                builderList.add(LambdaBuilderInfo(::calculateInclinationUsingGravityAndSetResultDSensorEventList, true))
            }
        }
    }

    private fun setBuilderListForDSensorRequiredRotationMatrix(registeredDSensorList: List<Int>,
                                                               builderList: MutableList<LambdaBuilderInfo>) {
        if (registeredDSensorList.contains(TYPE_ROLL)) {
            builderList.add(LambdaBuilderInfo(::calculateRollAndSetResultDSensorEventList, true))
        }

        if (registeredDSensorList.contains(TYPE_PITCH)) {
            builderList.add(LambdaBuilderInfo(::calculatePitchAndSetResultDSensorEventList, true))
        }

        if (mRegisteredWorldCoordinatesDSensorMap.size() != 0) {
            builderList.add(LambdaBuilderInfo(::setResultDSensorEventListForWorldCoordinatesDSensor, true))
        }

        if (mDirectionHistoryMap.size() != 0 || registeredDSensorList.contains(TYPE_DEVICE_ROTATION)) {
            mSaveDSensorMap.put(TYPE_INCLINATION, calculateInclination())
            if (registeredDSensorList.get(TYPE_INCLINATION) != null) {
                builderList.add(LambdaBuilderInfo(::setResultDSensorEventForInclination, true))
            }
            if (mDirectionHistoryMap.size() != 0){
                setDirectionFunctionMappingMap()
            }
            when {
                mDirectionHistoryMap.size() != 0 && registeredDSensorList.contains(TYPE_DEVICE_ROTATION) ->
                    builderList.add(LambdaBuilderInfo(::setResultDSensorEventListForDirectionAndDeviceRotation, true))
                mDirectionHistoryMap.size() != 0 -> builderList.add(LambdaBuilderInfo(::setResultDSensorEventListForDirection, true))
                else -> builderList.add(LambdaBuilderInfo(::setResultDSensorEventListForDeviceRotation, true))
            }
        } else if (registeredDSensorList.contains(TYPE_INCLINATION)) {
            builderList.add(LambdaBuilderInfo(::calculateInclinationAndSetResultDSensorEventList, true))
        }
    }

    private fun setDirectionFunctionMappingMap() {
        for (i in 0..mDirectionHistoryMap.size() - 1) {
            when (mDirectionHistoryMap.keyAt(i)) {
                TYPE_X_AXIS_DIRECTION ->
                    mFlatDirectionFunctionMappingMap.put(TYPE_X_AXIS_DIRECTION, ::calculateXAxisDirection)
                TYPE_NEGATIVE_X_AXIS_DIRECTION ->
                    mFlatDirectionFunctionMappingMap.put(TYPE_NEGATIVE_X_AXIS_DIRECTION, ::calculateNegativeXAxisDirection)
                TYPE_Y_AXIS_DIRECTION ->
                    mFlatDirectionFunctionMappingMap.put(TYPE_Y_AXIS_DIRECTION, ::calculateYAxisDirection)
                TYPE_NEGATIVE_Y_AXIS_DIRECTION ->
                    mFlatDirectionFunctionMappingMap.put(TYPE_NEGATIVE_Y_AXIS_DIRECTION, ::calculateNegativeYAxisDirection)
                TYPE_Z_AXIS_DIRECTION ->
                    mNonFlatDirectionFunctionMappingMap.put(TYPE_Z_AXIS_DIRECTION, ::calculateZAxisDirection)
                TYPE_NEGATIVE_Z_AXIS_DIRECTION ->
                    mFlatDirectionFunctionMappingMap.put(TYPE_NEGATIVE_Z_AXIS_DIRECTION, ::calculateNegativeZAxisDirection)
            }
        }
    }

    private fun setResultDSensorEventForInclination(resultList: MutableList<DSensorEvent>): Int {
        return setResultDSensorEventList(mSaveDSensorMap[TYPE_INCLINATION], resultList)
    }

    private fun setResultDSensorEventListForDirectionAndDeviceRotation(resultList: MutableList<DSensorEvent>): Int {
        var changedDSensorTypes = 0
        val inclinationEvent = mSaveDSensorMap[TYPE_INCLINATION]
        if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclinationEvent.values[0]> ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            for (i in 0..mDirectionHistoryMap.size() - 1) {
                val directionFunction = mFlatDirectionFunctionMappingMap.get(mDirectionHistoryMap.keyAt(i))
                changedDSensorTypes = changedDSensorTypes or setResultDSensorEventList(getDirection(mDirectionHistoryMap.keyAt(i),
                    if (directionFunction == null) Float.NaN else directionFunction()), resultList)
            }

            setResultDSensorEventList(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(Float.NaN)), resultList)
            changedDSensorTypes = changedDSensorTypes or TYPE_DEVICE_ROTATION
        } else {
            for (i in 0..mDirectionHistoryMap.size() - 1) {
                val directionFunction = mNonFlatDirectionFunctionMappingMap.get(mDirectionHistoryMap.keyAt(i))
                changedDSensorTypes = changedDSensorTypes or setResultDSensorEventList(getDirection(mDirectionHistoryMap.keyAt(i),
                    if (directionFunction == null) Float.NaN else directionFunction()), resultList)
            }

            setResultDSensorEventList(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(atan2(mRotationMatrix[6], mRotationMatrix[7]))), resultList)
            changedDSensorTypes = changedDSensorTypes or TYPE_DEVICE_ROTATION
        }

        return changedDSensorTypes
    }

    private fun setResultDSensorEventListForDirection(resultList: MutableList<DSensorEvent>): Int {
        var changedDSensorTypes = 0
        val inclinationEvent = mSaveDSensorMap[TYPE_INCLINATION]
        if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclinationEvent.values[0]> ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            for (i in 0..mDirectionHistoryMap.size() - 1) {
                val directionFunction = mFlatDirectionFunctionMappingMap.get(mDirectionHistoryMap.keyAt(i))
                changedDSensorTypes = changedDSensorTypes or setResultDSensorEventList(getDirection(mDirectionHistoryMap.keyAt(i),
                    if (directionFunction == null) Float.NaN else directionFunction()), resultList)
            }
        } else {
            for (i in 0..mDirectionHistoryMap.size() - 1) {
                val directionFunction = mNonFlatDirectionFunctionMappingMap.get(mDirectionHistoryMap.keyAt(i))
                changedDSensorTypes = changedDSensorTypes or setResultDSensorEventList(getDirection(mDirectionHistoryMap.keyAt(i),
                    if (directionFunction == null) Float.NaN else directionFunction()), resultList)
            }
        }

        return changedDSensorTypes
    }

    private fun setResultDSensorEventListForDeviceRotation(resultList: MutableList<DSensorEvent>): Int {
        var changedDSensorTypes = 0
        val inclinationEvent = mSaveDSensorMap[TYPE_INCLINATION]
        if (inclinationEvent.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclinationEvent.values[0]> ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) {
            setResultDSensorEventList(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(Float.NaN)), resultList)
            changedDSensorTypes = changedDSensorTypes or TYPE_DEVICE_ROTATION
        } else {
            setResultDSensorEventList(DSensorEvent(TYPE_DEVICE_ROTATION, inclinationEvent.accuracy, inclinationEvent.timestamp,
                floatArrayOf(atan2(mRotationMatrix[6], mRotationMatrix[7]))), resultList)
            changedDSensorTypes = changedDSensorTypes or TYPE_DEVICE_ROTATION
        }

        return changedDSensorTypes
    }

    private fun processCompassEvents(resultList: MutableList<DSensorEvent>) : Int {
        var changedSensorTypes = 0
        if (mDirectionHistoryMap.get(TYPE_Z_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_Z_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_Z_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_Z_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_Z_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_Z_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_Y_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_Y_AXIS_DIRECTION, atan2(mRotationMatrix[1], mRotationMatrix[4])))
            changedSensorTypes = changedSensorTypes or TYPE_Y_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_Y_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_Y_AXIS_DIRECTION, atan2((-mRotationMatrix[1]), (-mRotationMatrix[4]))))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_Y_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_X_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_X_AXIS_DIRECTION, atan2(mRotationMatrix[0], mRotationMatrix[3])))
            changedSensorTypes = changedSensorTypes or TYPE_X_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_X_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_X_AXIS_DIRECTION, atan2((-mRotationMatrix[0]), (-mRotationMatrix[3]))))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_X_AXIS_DIRECTION
        }

        return changedSensorTypes
    }

    private fun getDirection(dSensorType: Int, directionValue: Float) : DSensorEvent {
        val directionHistory = mDirectionHistoryMap[dSensorType]
        if (directionValue.isNaN()) {
            directionHistory.clearHistories()
            return DSensorEvent(dSensorType, 0, 0, floatArrayOf(Float.NaN))
        }

        directionHistory.add(
            DSensorEvent(dSensorType, mSaveDSensorMap[TYPE_DEVICE_GRAVITY].accuracy,
                if (mSaveDSensorMap[TYPE_DEVICE_GRAVITY].timestamp >= mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD].timestamp)
                    mSaveDSensorMap[TYPE_DEVICE_GRAVITY].timestamp
                else
                    mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD].timestamp,
                floatArrayOf(directionValue)
            )
        )

        return directionHistory.getAverageSensorEvent()
    }

    private fun processCameraDirectionEvents(resultList: MutableList<DSensorEvent>) : Int {
        var changedSensorTypes = 0
        if (mDirectionHistoryMap.get(TYPE_Y_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_Y_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_Y_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_Y_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_Y_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_Y_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_X_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_X_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_X_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_X_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_X_AXIS_DIRECTION, Float.NaN))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_X_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_Z_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_Z_AXIS_DIRECTION, atan2(mRotationMatrix[2], mRotationMatrix[5])))
            changedSensorTypes = changedSensorTypes or TYPE_Z_AXIS_DIRECTION
        }

        if (mDirectionHistoryMap.get(TYPE_NEGATIVE_Z_AXIS_DIRECTION) != null) {
            resultList.add(getDirection(TYPE_NEGATIVE_Z_AXIS_DIRECTION, atan2((-mRotationMatrix[2]), (-mRotationMatrix[5]))))
            changedSensorTypes = changedSensorTypes or TYPE_NEGATIVE_Z_AXIS_DIRECTION
        }

        return changedSensorTypes
    }

    private fun calculateInclinationAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        return setResultDSensorEventList(calculateInclination(), resultList)
    }

    private fun  calculateInclinationUsingGravityAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        return setResultDSensorEventList(calculateInclinationUsingGravity(), resultList)
    }

    private fun calculateInclinationUsingGravity(): DSensorEvent {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_INCLINATION, gravityEvent.accuracy, gravityEvent.timestamp,
            floatArrayOf(gravityEvent.values[2] / calculateNorm(gravityEvent.values)))
    }

    private fun calculateInclination(): DSensorEvent {
        val gravityDSensorEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_INCLINATION, gravityDSensorEvent.accuracy, gravityDSensorEvent.timestamp, floatArrayOf(acos(mRotationMatrix[8])))
    }

    private fun setResultDSensorEventListForWorldCoordinatesDSensor(resultList: MutableList<DSensorEvent>) : Int {
        var changedSensorTypes = 0
        var values: FloatArray?
        for (i in 0..mRegisteredWorldCoordinatesDSensorMap.size() - 1) {
            val dSensorEvent = mSaveDSensorMap.get(mRegisteredWorldCoordinatesDSensorMap.get(mRegisteredWorldCoordinatesDSensorMap[i]))
            values = productOfSquareMatrixAndVector(mRotationMatrix, dSensorEvent.values)
            if (values != null) {
                setResultDSensorEventList(DSensorEvent(mRegisteredWorldCoordinatesDSensorMap.keyAt(i), dSensorEvent.accuracy,
                    dSensorEvent.timestamp, values.copyOf()), resultList)
                changedSensorTypes = changedSensorTypes or mRegisteredWorldCoordinatesDSensorMap.keyAt(i)
            }
        }

        return changedSensorTypes
    }

    private fun calculateRollAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        val rollEvent = calculateRoll()
        setResultDSensorEventList(rollEvent, resultList)
        return TYPE_ROLL
    }

    private fun calculateRoll(): DSensorEvent {
        val gravity = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_ROLL, gravity.accuracy, gravity.timestamp, floatArrayOf(atan2(-mRotationMatrix[6], mRotationMatrix[8])))
    }

    private fun calculateRollUsingGravityAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        val gravityNorm = calculateNorm(gravityEvent.values)
        resultList.add(DSensorEvent(TYPE_ROLL, gravityEvent.accuracy, gravityEvent.timestamp,
            floatArrayOf(atan2(-gravityEvent.values[0] / gravityNorm, gravityEvent.values[2] / gravityNorm))))
        return TYPE_ROLL
    }

    private fun calculatePitchUsingGravityAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        val gravityEvent = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        resultList.add(DSensorEvent(TYPE_PITCH, gravityEvent.accuracy, gravityEvent.timestamp,
            floatArrayOf(asin(-gravityEvent.values[1] / calculateNorm(gravityEvent.values)))))
        return TYPE_PITCH
    }

    private fun calculatePitchAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        val pitchEvent = calculatePitch()
        setResultDSensorEventList(pitchEvent, resultList)
        return TYPE_PITCH
    }

    private fun calculatePitch(): DSensorEvent {
        val gravity = mSaveDSensorMap[TYPE_DEVICE_GRAVITY]
        return DSensorEvent(TYPE_PITCH, gravity.accuracy, gravity.timestamp, floatArrayOf(asin(-mRotationMatrix[7])))
    }

    private fun setOnAccelerometerChangedHandler(registeredDSensorList: ArrayList<Int>,
                                                 registeredWorldCoordinatesDSensorList: List<Int>,
                                                 isGravityCalculationRequired: Boolean,
                                                 isLinearAccelerationCalculationRequired: Boolean):
                ((Int, SensorEvent, DProcessedSensorEvent) -> Int)? {
        val builderList = ArrayList<LambdaBuilderInfo>()

        if (mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER) != null) {
            builderList.add(LambdaBuilderInfo(::saveDSensorEvent, false))
        }

        if (registeredDSensorList.contains(TYPE_DEVICE_ACCELEROMETER)) {
            builderList.add(LambdaBuilderInfo(::setResultDSensorEventList, false))
        }

        if (isGravityCalculationRequired) {
            builderList.add(LambdaBuilderInfo(::calculateGravity, true))
            if (mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY) != null) {
                builderList.add(LambdaBuilderInfo(::saveDSensorEvent, false))
            }
            if (registeredDSensorList.contains(TYPE_DEVICE_GRAVITY)) {
                builderList.add(LambdaBuilderInfo(::setResultDSensorEventList, false))
            }
            if (isLinearAccelerationCalculationRequired && !registeredWorldCoordinatesDSensorList.contains(TYPE_WORLD_LINEAR_ACCELERATION)) {
                builderList.add(LambdaBuilderInfo(::calculateLinearAcceleration, true))
                if (registeredDSensorList.contains(TYPE_DEVICE_LINEAR_ACCELERATION)) {
                    builderList.add(LambdaBuilderInfo(::setResultDSensorEventList, false))
                }
            }
            if (mDirectionHistoryMap.size() != 0 || registeredWorldCoordinatesDSensorList.isNotEmpty()) {

            }
        }

        return null
    }

    private fun saveDSensorEvent(event: DSensorEvent) {
        val dSensorEvent = mSaveDSensorMap.get(event.sensorType)
        dSensorEvent.accuracy = event.accuracy
        dSensorEvent.timestamp = event.timestamp
        dSensorEvent.values = event.values.copyOf()
    }

    private fun setResultDSensorEventList(event: DSensorEvent, resultList: MutableList<DSensorEvent>): Int {
        resultList.add(event.copyOf())
        return event.sensorType
    }

    private fun setRotationMatrix(): Boolean {
        return SensorManager.getRotationMatrix(mRotationMatrix, null, mSaveDSensorMap[TYPE_DEVICE_GRAVITY].values,
            mSaveDSensorMap[TYPE_DEVICE_MAGNETIC_FIELD].values)
    }

    private fun calculateGravity(): DSensorEvent {
        val acceleromter = mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER)
        val gravity = mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY)
        if (gravity.timestamp == 0L) {
            gravity.values = acceleromter.values.copyOf()
        } else {
            for (i in 0..2) {
                gravity.values[i] = ALPHA * acceleromter.values[i] + ONE_MINUS_ALPHA * gravity.values[i]
            }
        }

        gravity.accuracy = acceleromter.accuracy
        gravity.timestamp = acceleromter.timestamp

        return gravity
    }

    private fun calculateLinearAccelerationAndSetResultDSensorEventList(resultList: MutableList<DSensorEvent>): Int {
        setResultDSensorEventList(calculateLinearAcceleration(), resultList)
        return TYPE_DEVICE_LINEAR_ACCELERATION
    }

    private fun calculateLinearAcceleration(): DSensorEvent {
        logger(DSensorEventProcessorImp::class.java.simpleName, "calculateLinearAcceleration")
        val accelerometer = mSaveDSensorMap.get(TYPE_DEVICE_ACCELEROMETER)
        val gravity = mSaveDSensorMap.get(TYPE_DEVICE_GRAVITY)
        val values = FloatArray(3)
        for (i in 0..2) {
            values[i] = accelerometer.values[i] - gravity.values[i]
        }

        return DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, accelerometer.accuracy, accelerometer.timestamp, values)
    }

    private fun setDProcessedSensorEventGravityProperty() {

    }

    private suspend fun onDSensorChangedDefaultHandler(dSensorTypes: Int,
                                                       event: SensorEvent,
                                                       dProcessedSensorEvent: DProcessedSensorEvent) {
        //setDProcessedSensorEventMember(dSensorTypes, event, dProcessedSensorEvent)
    }

    override fun finish() {

    }

    fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        logger(DSensorEventProcessorOldImp::class.java.simpleName, "onAccuracyChanged(${sensor.name}  , ${accuracy})")
    }

    override fun onDSensorChanged(dSensorEvent: DSensorEvent) {
        runBlocking {
            val dProcessedSensorEvent = DProcessedSensorEvent()
            val result = mCoroutineScope.async(start = CoroutineStart.LAZY) {

            }
            /*val changedSensorTypes = result.await()
            if (changedSensorTypes != 0) {
                mDSensorEventListener?.onDSensorChanged(changedSensorTypes, dProcessedSensorEvent)
            }*/
        }
    }

    fun onSensorChanged(event: SensorEvent) {
        runBlocking {
            val dSensorType = getDSensorType(event.sensor.type)
            val dProcessedSensorEvent = DProcessedSensorEvent()
            val result = mCoroutineScope.async(start = CoroutineStart.LAZY) {
                onDSensorChangedDefaultHandler(dSensorType, event, dProcessedSensorEvent)
            }
            /*val changedSensorTypes = result.await()
            if (changedSensorTypes != 0) {
                mDSensorEventListener?.onDSensorChanged(changedSensorTypes, dProcessedSensorEvent)
            }*/
        }
    }

    private fun calculateXAxisDirection(): Float {
        return atan2(mRotationMatrix[0], mRotationMatrix[3])
    }

    private fun calculateNegativeXAxisDirection(): Float {
        return atan2(-mRotationMatrix[0], -mRotationMatrix[3])
    }

    private fun calculateYAxisDirection(): Float {
        return atan2(mRotationMatrix[1], mRotationMatrix[4])
    }

    private fun calculateNegativeYAxisDirection(): Float {
        return atan2(-mRotationMatrix[1], -mRotationMatrix[4])
    }

    private fun calculateZAxisDirection(): Float {
        return atan2(mRotationMatrix[2], mRotationMatrix[5])
    }

    private fun calculateNegativeZAxisDirection(): Float {
        return atan2(-mRotationMatrix[2], -mRotationMatrix[5])
    }

    private class LambdaBuilderInfo(val function: KFunction<Any>, val setChangedDSensorType: Boolean) {

        fun getNumberOfParameters(): Int {
            return function.parameters.size
        }

        fun getParameterType(index: Int): KType? {
            return when {
                function.parameters.isEmpty() || index >= function.parameters.size -> null
                else -> function.parameters[index].type
            }
        }

        fun getJavaReturnType(): Type {
            return function.returnType.javaType
        }
    }
}
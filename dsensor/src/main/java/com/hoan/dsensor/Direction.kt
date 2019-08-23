package com.hoan.dsensor

import android.hardware.SensorManager
import com.hoan.dsensor.utils.*
import java.util.*
import kotlin.math.atan2
import kotlin.math.round

const val DEFAULT_HISTORY_SIZE = 10

class Direction(dDirectionSensorType: Int, historyMaxLength: Int = DEFAULT_HISTORY_SIZE) {
    private val mDirectionSensorType = dDirectionSensorType
    private val mRotationMatrixIndices: Pair<Int, Int> = getRotationMatrixIndices(dDirectionSensorType)
    private val mIsPositive: Boolean = isPositive(dDirectionSensorType)
    private val mIsCamera: Boolean = isCamera(dDirectionSensorType)
    private val mDirectionHistory = DirectionHistory(dDirectionSensorType, historyMaxLength)

    fun getDirectionDSensorEvent(inclination: DSensorEvent, rotationMatrix: FloatArray): DSensorEvent {
        return when {
            (inclination.values[0] < TWENTY_FIVE_DEGREE_IN_RADIAN || inclination.values[0] > ONE_FIFTY_FIVE_DEGREE_IN_RADIAN) && mIsCamera -> {
                mDirectionHistory.clearHistories()
                DSensorEvent(mDirectionSensorType, inclination.accuracy, inclination.timestamp, floatArrayOf(Float.NaN))
            }
            else -> {
                mDirectionHistory.add(
                    DSensorEvent(
                        mDirectionSensorType, inclination.accuracy, inclination.timestamp,
                        floatArrayOf(getDirection(rotationMatrix))
                    )
                )
                mDirectionHistory.getAverageSensorEvent()
            }
        }
    }

    private fun getDirection(rotationMatrix: FloatArray): Float {
        return when (mIsPositive) {
            true -> atan2(rotationMatrix[mRotationMatrixIndices.first], rotationMatrix[mRotationMatrixIndices.second])
            else -> atan2(-rotationMatrix[mRotationMatrixIndices.first], -rotationMatrix[mRotationMatrixIndices.second])
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

    private class DirectionHistory(dSensorDirectionType: Int, historyMaxSize: Int = DEFAULT_HISTORY_SIZE) {
        private val mHistories = LinkedList<DSensorEvent>()
        private val mHistoriesSum = floatArrayOf(0.0f, 0.0f)
        private val mSensorDirectionType = dSensorDirectionType
        private val mHistoryMaxSize = historyMaxSize
        private var mAccuracy: Int = 0
        private var mTimeStamp: Long = 0

        fun clearHistories() {
            logger("DirectionHistory", "clearHistories")
            mHistories.clear()
            mTimeStamp = 0
            mAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
            mHistoriesSum[0] = 0.0f
            mHistoriesSum[1] = 0.0f
        }

        fun add(item: DSensorEvent) {
            logger("DirectionHistory", "add size = ${mHistories.size} angle = ${round(Math.toDegrees(item.values[0].toDouble()))}")

            if (mHistories.size == mHistoryMaxSize) {
                val firstTerm = mHistories.removeFirst()
                removeAngle(firstTerm.values[0], mHistoriesSum)
            }

            mHistories.addLast(item)
            if (item.accuracy < mAccuracy) {
                mAccuracy = item.accuracy
            }
            mTimeStamp = item.timestamp
            addAngle(item.values[0], mHistoriesSum)
        }

        fun getAverageSensorEvent(): DSensorEvent {
            return DSensorEvent(mSensorDirectionType, mAccuracy, mTimeStamp,
                if (mHistories.isEmpty()) floatArrayOf(java.lang.Float.NaN)
                else floatArrayOf(averageAngle(mHistoriesSum, mHistories.size))
            )
        }
    }
}
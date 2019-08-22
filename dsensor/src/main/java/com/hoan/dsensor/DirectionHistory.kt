package com.hoan.dsensor

import android.hardware.SensorManager
import com.hoan.dsensor.utils.addAngle
import com.hoan.dsensor.utils.averageAngle
import com.hoan.dsensor.utils.logger
import com.hoan.dsensor.utils.removeAngle
import java.util.*
import kotlin.math.round

internal const val DEFAULT_HISTORY_SIZE = 10

internal class DirectionHistory(dSensorDirectionType: Int, historyMaxSize: Int = DEFAULT_HISTORY_SIZE) {
    private val mHistories = LinkedList<DSensorEvent>()
    private val mHistoriesSum = floatArrayOf(0.0f, 0.0f)
    private val mSensorDirectionType = dSensorDirectionType
    private val mHistoryMaxSize = historyMaxSize
    private var mAccuracy: Int = 0
    private var mTimeStamp: Long = 0

    fun clearHistories() {
        logger(DirectionHistory::class.java.simpleName, "clearHistories")
        mHistories.clear()
        mTimeStamp = 0
        mAccuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH
        mHistoriesSum[0] = 0.0f
        mHistoriesSum[1] = 0.0f
    }

    fun add(item: DSensorEvent) {
        logger(DirectionHistory::class.java.simpleName, "add size = " + mHistories.size
                + " angle = " + round(Math.toDegrees(item.values[0].toDouble())))

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
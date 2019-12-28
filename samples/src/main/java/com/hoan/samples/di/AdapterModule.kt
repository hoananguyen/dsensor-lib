package com.hoan.samples.di

import android.content.Context
import com.hoan.dsensor.*
import com.hoan.samples.R
import com.hoan.samples.adapters.SensorExpandableListAdapter
import dagger.Module
import dagger.Provides
import java.util.*

@Module
class AdapterModule {

    @Provides
    fun provideExpandableAdapter(context: Context): SensorExpandableListAdapter {
        val mListItemLinkedHashMap: MutableMap<Int, List<Int>?> = LinkedHashMap()
        mListItemLinkedHashMap[R.string.sensors_info] = null
        val compassList = arrayListOf(getCompassSensorType(context))
        compassList.addAll(setOf(compassList[0] or TYPE_NEGATIVE_Z_AXIS_DIRECTION))
        mListItemLinkedHashMap[R.string.compass] = compassList
        mListItemLinkedHashMap[R.string.sensor_in_world_coord] =
            arrayListOf(
                TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER,
                TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY,
                TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION,
                TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD
            )
        mListItemLinkedHashMap[R.string.sensors] = arrayListOf(R.string.accelerometer, R.string.device_rotation, R.string.gravity,
            R.string.gyroscope, R.string.inclination, R.string.linear_acceleration, R.string.magnetic_field, R.string.pitch,
            R.string.roll, R.string.rotation_vector)
        return SensorExpandableListAdapter(context, mListItemLinkedHashMap)
    }
}
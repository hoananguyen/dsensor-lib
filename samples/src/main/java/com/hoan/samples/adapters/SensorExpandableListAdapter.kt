package com.hoan.samples.adapters

import android.content.Context
import com.hoan.dsensor.*
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R
import javax.inject.Inject

class SensorExpandableListAdapter @Inject constructor(context: Context, groupChildMap: Map<Int, List<Int>?>)
    : AbstractExpandableListAdapter(context, groupChildMap) {


    override fun getGroupName(groupPosition: Int): String {
        logger("SensorExpandableListAdapter", "getGroupName($groupPosition) groupName = ${mContext.getString(getGroupId(groupPosition).toInt())}")
        return mContext.getString(getGroupId(groupPosition).toInt())
    }

    override fun getChildName(groupPosition: Int, childPosition: Int): String {
        logger("SensorExpandableListAdapter", "getChildName($groupPosition, $childPosition)")
        return when (getGroup(groupPosition)) {
            R.string.sensor_in_world_coord -> getSensorInWorldCoordName(getChild(groupPosition, childPosition))
            R.string.compass -> getCompassName(getChild(groupPosition, childPosition))
            else -> mContext.getString(getChild(groupPosition, childPosition))
        }
    }

    private fun getSensorInWorldCoordName(dSensorTypes: Int?): String {
        if (dSensorTypes == null) return ""

        return when {
            dSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0 -> mContext.getString(R.string.accelerometer)
            dSensorTypes and TYPE_DEVICE_GRAVITY != 0 -> mContext.getString(R.string.gravity)
            dSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 -> mContext.getString(R.string.linear_acceleration)
            dSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0 -> mContext.getString(R.string.magnetic_field)
            else -> ""
        }
    }

    private fun getCompassName(compassType: Int?): String {
        val compassDirectionSensor = getCompassSensorType(mContext)
        return when (compassType) {
            compassDirectionSensor -> mContext.getString(R.string.compass)
            compassDirectionSensor or TYPE_DEPRECATED_ORIENTATION -> mContext.getString(R.string.compass_and_deprecated_orientation)
            compassDirectionSensor or TYPE_NEGATIVE_Z_AXIS_DIRECTION -> mContext.getString(R.string.compass_3d)
            else -> mContext.getString(R.string.compass_3d_and_deprecated_orientation)
        }
    }
}
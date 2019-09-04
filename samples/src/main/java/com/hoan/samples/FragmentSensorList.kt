package com.hoan.samples

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment
import com.hoan.dsensor.*
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_sensor_list.view.*
import kotlinx.android.synthetic.main.sensor_expandable_list_child_item.view.*
import kotlinx.android.synthetic.main.simple_expandable_list_item.view.*
import java.util.*

class FragmentSensorList : Fragment() {

    private var mFragmentInteractionListener: OnFragmentInteractionListener? = null

    interface OnFragmentInteractionListener {
        fun onGroupItemSelected(item: Int)

        fun onChildItemSelected(group: Int, childItemId: Int, childName: String)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorList::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_list, container, false)

        v.sensor_expandablelistview.setAdapter(SensorExpandableListAdapter())

        v.sensor_expandablelistview.setOnChildClickListener {
                parent: ExpandableListView, view: View, groupPosition: Int, childPosition: Int, id: Long ->
                val group = (parent.expandableListAdapter as SensorExpandableListAdapter).getGroup(groupPosition)
                if (group != R.string.sensors_info) {
                    mFragmentInteractionListener?.onChildItemSelected(group, id.toInt(), view.textview_child_item.text.toString())
                    parent.setSelectedChild(groupPosition, childPosition, true)
                }

                true
            }

        v.sensor_expandablelistview.setOnGroupClickListener { parent: ExpandableListView, _: View?, groupPosition: Int, _: Long ->

                when (groupPosition) {
                    0-> mFragmentInteractionListener?.onGroupItemSelected(
                        (parent.expandableListAdapter as SensorExpandableListAdapter).getGroup(groupPosition))
                    else -> when {
                        parent.isGroupExpanded(groupPosition) -> parent.collapseGroup(groupPosition)
                        else -> parent.expandGroup(groupPosition)
                    }
                }

                true
            }

        return v
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        logger(FragmentSensorList::class.java.simpleName, "onAttach")
        if (context is OnFragmentInteractionListener) {
            mFragmentInteractionListener = context
        } else {
            throw RuntimeException("$context.toString() +  must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        logger(FragmentSensorList::class.java.simpleName, "onDetach")
        mFragmentInteractionListener = null
    }

    private inner class SensorExpandableListAdapter : BaseExpandableListAdapter() {
        private val mListItemLinkedHashMap: LinkedHashMap<Int, List<Int>?> = LinkedHashMap()

        init {
            mListItemLinkedHashMap[R.string.sensors_info] = null
            mListItemLinkedHashMap[R.string.compass] = arrayListOf(TYPE_COMPASS, TYPE_3D_COMPASS,
                TYPE_COMPASS_AND_DEPRECATED_ORIENTATION, TYPE_3D_COMPASS_AND_DEPRECATED_ORIENTATION)
            mListItemLinkedHashMap[R.string.sensor_in_world_coord] =
                arrayListOf(TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER,
                    TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY,
                    TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION,
                    TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD)
        }

        override fun getGroup(groupPosition: Int): Int {
            return mListItemLinkedHashMap.keys.elementAt(groupPosition)
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val v: View = convertView ?: activity!!.layoutInflater.inflate(R.layout.simple_expandable_list_item, parent, false)

            v.item_textview.setText(getGroup(groupPosition))

            return v
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return mListItemLinkedHashMap.values.elementAt(groupPosition)?.size ?: 0
        }

        override fun getChild(groupPosition: Int, childPosition: Int): Int? {
            return mListItemLinkedHashMap[getGroup(groupPosition)]?.get(childPosition)
        }

        override fun getGroupId(groupPosition: Int): Long {
            return 0
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val v: View = convertView ?: activity!!.layoutInflater.inflate(R.layout.sensor_expandable_list_child_item, parent, false)

            val childItemName = when (getGroup(groupPosition)) {
                R.string.sensor_in_world_coord -> getSensorInWorldCoordName(getChild(groupPosition, childPosition))
                R.string.compass -> getCompassName(getChild(groupPosition, childPosition))
                else -> ""
            }
            v.textview_child_item.text = childItemName

            return v
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return when (getGroup(groupPosition)) {
                R.string.sensor_in_world_coord -> getChild(groupPosition, childPosition)?.toLong() ?: 0L
                R.string.compass -> getDSensorTypes((context!!.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation,
                                            getChild(groupPosition, childPosition)).toLong()
                else -> 0L
            }
        }

        override fun getGroupCount(): Int {
            return mListItemLinkedHashMap.size
        }

        private fun getSensorInWorldCoordName(dSensorTypes: Int?): String {
            if (dSensorTypes == null) return ""

            return when {
                dSensorTypes and TYPE_DEVICE_ACCELEROMETER != 0 -> getString(R.string.accelerometer)
                dSensorTypes and TYPE_DEVICE_GRAVITY != 0 -> getString(R.string.gravity)
                dSensorTypes and TYPE_DEVICE_LINEAR_ACCELERATION != 0 -> getString(R.string.linear_acceleration)
                dSensorTypes and TYPE_DEVICE_MAGNETIC_FIELD != 0 -> getString(R.string.magnetic_field)
                else -> ""
            }
        }

        private fun getCompassName(compassType: Int?): String {
            return when (compassType) {
                TYPE_COMPASS -> getString(R.string.compass)
                TYPE_COMPASS_AND_DEPRECATED_ORIENTATION -> getString(R.string.compass_and_deprecated_orientation)
                TYPE_3D_COMPASS -> getString(R.string.compass_3d)
                TYPE_3D_COMPASS_AND_DEPRECATED_ORIENTATION -> getString(R.string.compass_3d_and_deprecated_orientation)
                else -> ""
            }
        }
    }
}

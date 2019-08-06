package com.hoan.samples

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.ExpandableListView
import com.hoan.dsensor.DSensor.TYPE_DEVICE_ACCELEROMETER
import com.hoan.dsensor.DSensor.TYPE_DEVICE_GRAVITY
import com.hoan.dsensor.DSensor.TYPE_DEVICE_LINEAR_ACCELERATION
import com.hoan.dsensor.DSensor.TYPE_DEVICE_MAGNETIC_FIELD
import com.hoan.dsensor.DSensor.TYPE_WORLD_ACCELEROMETER
import com.hoan.dsensor.DSensor.TYPE_WORLD_GRAVITY
import com.hoan.dsensor.DSensor.TYPE_WORLD_LINEAR_ACCELERATION
import com.hoan.dsensor.DSensor.TYPE_WORLD_MAGNETIC_FIELD
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.fragment_sensor_list.view.*
import kotlinx.android.synthetic.main.sensor_expandable_list_child_item.view.*
import kotlinx.android.synthetic.main.simple_expandable_list_item.view.*
import java.util.*

class FragmentSensorList : Fragment() {

    private var mFragmentInteractionListener: OnFragmentInteractionListener? = null

    interface OnFragmentInteractionListener {
        fun onGroupItemSelected(item: String?)

        fun onChildItemSelected(group: String, childItemId: Int)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorList::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_list, container, false)

        v.sensor_expandablelistview.setAdapter(SensorExpandableListAdapter())

        v.sensor_expandablelistview.setOnChildClickListener { parent: ExpandableListView, _: View?, groupPosition: Int, childPosition: Int, id: Long ->
                val group = (parent.expandableListAdapter as SensorExpandableListAdapter).getGroup(groupPosition)
                if (id != 0L && group != null) {
                    mFragmentInteractionListener?.onChildItemSelected(group, id.toInt())
                    parent.setSelectedChild(groupPosition, childPosition, true)
                }

                true
            }

        v.sensor_expandablelistview.setOnGroupClickListener { parent: ExpandableListView?, _: View?, groupPosition: Int, _: Long ->

                when (groupPosition) {
                    0-> mFragmentInteractionListener?.onGroupItemSelected(
                        (parent?.expandableListAdapter as SensorExpandableListAdapter).getGroup(groupPosition))
                    else -> when {
                        parent?.isGroupExpanded(groupPosition) == true -> parent.collapseGroup(groupPosition)
                        else -> parent?.expandGroup(groupPosition)
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
        private val mListItemLinkedHashMap: LinkedHashMap<String, List<String>?> = LinkedHashMap()

        init {
            mListItemLinkedHashMap[getString(R.string.sensors_info)] = null
            mListItemLinkedHashMap[getString(R.string.compass)] = arrayListOf(getString(R.string.compass),
                getString(R.string.compass_3d), getString(R.string.compass_and_deprecated_orientation),
                getString(R.string.compass_3d_and_deprecated_orientation))
            mListItemLinkedHashMap[getString(R.string.sensor_in_world_coord)] = arrayListOf(getString(R.string.accelerometer),
                getString(R.string.gravity), getString(R.string.linear_acceleration), getString(R.string.magnetic_field))
        }

        override fun getGroup(groupPosition: Int): String? {
            if (groupPosition >= mListItemLinkedHashMap.size) {
                return null
            }

            return mListItemLinkedHashMap.keys.toList()[groupPosition]
        }

        override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
            return true
        }

        override fun hasStableIds(): Boolean {
            return false
        }

        override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val v: View = convertView ?: activity!!.layoutInflater.inflate(R.layout.simple_expandable_list_item, parent, false)

            v.item_textview.text = getGroup(groupPosition)

            return v
        }

        override fun getChildrenCount(groupPosition: Int): Int {
            return mListItemLinkedHashMap.values.toList()[groupPosition]?.size ?: 0
        }

        override fun getChild(groupPosition: Int, childPosition: Int): String? {
            return mListItemLinkedHashMap[getGroup(groupPosition)]?.get(childPosition)
        }

        override fun getGroupId(groupPosition: Int): Long {
            return 0
        }

        override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
            val v: View = convertView ?: activity!!.layoutInflater.inflate(R.layout.sensor_expandable_list_child_item, parent, false)

            v.textview_child_item.text = getChild(groupPosition, childPosition)

            return v
        }

        override fun getChildId(groupPosition: Int, childPosition: Int): Long {
            return when (getGroup(groupPosition)) {
                getString(R.string.sensor_in_world_coord) -> {
                    when (getChild(groupPosition, childPosition)) {
                        getString(R.string.accelerometer) -> (TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER).toLong()
                        getString(R.string.gravity) -> (TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY).toLong()
                        getString(R.string.linear_acceleration) -> (TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION).toLong()
                        getString(R.string.magnetic_field) -> (TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD).toLong()
                        else -> 0L
                    }
                }
                getString(R.string.compass) -> 0L
                else -> 0L
            }
        }

        override fun getGroupCount(): Int {
            return mListItemLinkedHashMap.size
        }
    }
}

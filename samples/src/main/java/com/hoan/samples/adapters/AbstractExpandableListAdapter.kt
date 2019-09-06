package com.hoan.samples.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import com.hoan.samples.R
import kotlinx.android.synthetic.main.sensor_expandable_list_child_item.view.*
import kotlinx.android.synthetic.main.simple_expandable_list_item.view.*

abstract class AbstractExpandableListAdapter(context: Context, groupChildMap: Map<Int, List<Int>?>) : BaseExpandableListAdapter() {
    private val mGroupChildMap = groupChildMap
    protected val mContext = context

    abstract fun getGroupName(groupPosition: Int): String
    abstract fun getChildName(groupPosition: Int, childPosition: Int): String

    override fun getGroup(groupPosition: Int): Int {
        return mGroupChildMap.keys.elementAt(groupPosition)
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean, convertView: View?, parent: ViewGroup?): View? {
        val v: View = convertView ?: (mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.simple_expandable_list_item, parent, false)

        v.item_textview.text = getGroupName(groupPosition)

        return v
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return mGroupChildMap.values.elementAt(groupPosition)?.size ?: 0
    }

    override fun getChild(groupPosition: Int, childPosition: Int): Int {
        return mGroupChildMap[getGroup(groupPosition)]?.get(childPosition) ?: 0
    }

    override fun getGroupId(groupPosition: Int): Long {
        return getGroup(groupPosition).toLong()
    }

    override fun getChildView(groupPosition: Int, childPosition: Int, isLastChild: Boolean, convertView: View?, parent: ViewGroup?): View? {
        val v: View = convertView ?: (mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater)
            .inflate(R.layout.sensor_expandable_list_child_item, parent, false)

        v.textview_child_item.text = getChildName(groupPosition, childPosition)

        return v
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return getChild(groupPosition, childPosition).toLong()
    }

    override fun getGroupCount(): Int {
        return mGroupChildMap.size
    }
}
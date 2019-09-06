package com.hoan.samples.fragments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import androidx.fragment.app.Fragment
import com.hoan.dsensor.utils.logger
import com.hoan.samples.R
import com.hoan.samples.SamplesApplication
import com.hoan.samples.adapters.SensorExpandableListAdapter
import kotlinx.android.synthetic.main.fragment_sensor_list.view.*
import kotlinx.android.synthetic.main.sensor_expandable_list_child_item.view.*
import javax.inject.Inject

class FragmentSensorList : Fragment() {

    private var mFragmentInteractionListener: OnFragmentInteractionListener? = null

    @Inject lateinit var mSensorExpandableListAdapter: SensorExpandableListAdapter

    interface OnFragmentInteractionListener {
        fun onGroupItemSelected(item: Int)

        fun onChildItemSelected(group: Int, childItemId: Int, childName: String)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        logger(FragmentSensorList::class.java.simpleName, "onCreateView")
        val v = inflater.inflate(R.layout.fragment_sensor_list, container, false)

        (activity!!.application as SamplesApplication).appComponent.inject(this)
        v.sensor_expandablelistview.setAdapter(mSensorExpandableListAdapter)
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
}

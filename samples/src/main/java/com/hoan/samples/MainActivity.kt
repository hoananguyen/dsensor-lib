package com.hoan.samples

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.hoan.dsensor.utils.logger
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), FragmentSensorList.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        logger(MainActivity::class.java.simpleName, "onCreate")

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null && fragment_container != null) {
            loadFragment(R.id.fragment_container, FragmentSensorList())
        }
    }

    override fun onGroupItemSelected(item: Int) {
        logger(MainActivity::class.java.simpleName, "onGroupItemSelected: item = $item")
        if (R.string.sensors_info == item) {
            if (supportFragmentManager.findFragmentByTag(FragmentSensorInfo::class.java.simpleName) == null) {
                loadFragment(fragment_container?.id ?: R.id.fragment_container_large, FragmentSensorInfo(), true)
            }
        }
    }

    override fun onChildItemSelected(group: Int, childItemId: Int, childName: String) {
        logger(MainActivity::class.java.simpleName, "onChildItemSelected: group = $group childItemId = $childItemId")
        val backStackCount = supportFragmentManager.backStackEntryCount
        if (backStackCount > 0) {
            val fragmentName = supportFragmentManager.getBackStackEntryAt(backStackCount - 1).name
            val topOfStackFragment = supportFragmentManager.findFragmentByTag(fragmentName)
            if (topOfStackFragment is BaseSensorFragment) {
                if (R.string.compass == group && FragmentCompass::class.java.simpleName == fragmentName
                    || (R.string.sensor_in_world_coord == group && FragmentSensorInWorldCoord::class.java.simpleName == fragmentName)) {
                    topOfStackFragment.onNewSensorSelected(childItemId, childName)
                    return
                }
            }
        }

        val fragment = when (group) {
            R.string.compass -> FragmentCompass.newInstance(childItemId, childName, group)
            R.string.sensor_in_world_coord -> FragmentSensorInWorldCoord.newInstance(childItemId, childName, group)
            else -> return
        }

        loadFragment(fragment_container?.id ?: R.id.fragment_container_large, fragment, true)
    }

    override fun onBackPressed() {
        logger(MainActivity::class.java.simpleName, "onBackPressed")
        if (supportFragmentManager.backStackEntryCount != 0) {
            supportFragmentManager.popBackStackImmediate()
        } else {
            finish()
        }
    }

    private fun loadFragment(containerResId: Int, fragment: Fragment, addToBackStact: Boolean = false) {
        logger(MainActivity::class.java.simpleName, "loadFragment")
        if (supportFragmentManager.backStackEntryCount != 0) {
            logger(MainActivity::class.java.simpleName, "loadFragment pop backStack")
            supportFragmentManager.popBackStackImmediate()
        }
        val transaction: FragmentTransaction = supportFragmentManager.beginTransaction()
        transaction.replace(containerResId, fragment, fragment.javaClass.simpleName)
        if (addToBackStact) {
            transaction.addToBackStack(fragment.javaClass.simpleName)
        }
        transaction.commit()
    }
}

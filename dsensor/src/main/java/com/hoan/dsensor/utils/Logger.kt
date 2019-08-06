package com.hoan.dsensor.utils

import android.util.Log
import com.hoan.dsensor.DSensorManager

/**
 * Convenient method for logging
 */

private const val WRITE_TO_FILE = false

private val DEBUG_CLASSES = arrayOf(
    "dummy",
    DSensorManager::class.java.simpleName,
    "BaseSensorFragment",
    "FragmentSensorInfo",
    "FragmentSensorList",
    "MainActivity"
    //DSensorEventProcessor::class.java.simpleName
    //*"DirectionHistory"
    //DMathKt,
    //DSensorEvent.class.getSimpleName(),
    //DSensorEventProcessor.class.getSimpleName(),
    //"WorldHistory"*/
)

@Synchronized
fun logger(tag: String, msg: String) {
    if (DEBUG_CLASSES.contains(tag)) {
        Log.e(tag, msg)
        if (WRITE_TO_FILE) {
            // TODO write to file.
        }
    }
}
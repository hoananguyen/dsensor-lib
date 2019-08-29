package com.hoan.dsensor.utils

import android.util.Log

/**
 * Convenient method for logging
 */

private const val WRITE_TO_FILE = false

private val DEBUG_CLASSES = arrayOf(
    "dummy",
    "DSensorEventProcessorImp"
    //"DSensorManager"
    //"FragmentCompass",
    //"BaseSensorFragment",
    //"FragmentSensorInfo",
    //"FragmentSensorInWorldCoord"
    //"FragmentSensorList",
    //"MainActivity"
    /*"DirectionHistory"
    //DMathKt,
    //DSensorEvent.class.getSimpleName(),*/
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
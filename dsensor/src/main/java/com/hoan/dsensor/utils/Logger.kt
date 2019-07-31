package com.hoan.dsensor.utils

import android.util.Log

/**
 * Convenient method for logging
 */

private const val WRITE_TO_FILE = false

private val DEBUG_CLASSES = arrayOf(
    "dummy"
    /*"DirectionHistory"
    //DMathKt,
    //DProcessedSensorEvent.class.getSimpleName(),
    //DSensorEvent.class.getSimpleName(),
    //DSensorEventProcessor.class.getSimpleName(),
    //DSensorManager.class.getSimpleName(),
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
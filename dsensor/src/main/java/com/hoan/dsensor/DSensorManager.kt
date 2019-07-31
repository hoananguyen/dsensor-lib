package com.hoan.dsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.WindowManager
import com.hoan.dsensor.interfaces.DProcessedEventListener
import com.hoan.dsensor.interfaces.DSensorEventListener
import com.hoan.dsensor.utils.logger

const val ERROR_UNSUPPORTED_TYPE = -1

const val TYPE_ACCELEROMETER_NOT_AVAILABLE = 2
const val TYPE_MAGNETIC_FIELD_NOT_AVAILABLE = 4
const val TYPE_GRAVITY_NOT_AVAILABLE = 8
const val TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE = 16
const val TYPE_GYROSCOPE_NOT_AVAILABLE = 32
const val TYPE_ROTATION_VECTOR_NOT_AVAILABLE = 64
const val TYPE_ORIENTATION_NOT_AVAILABLE = 128

class DSensorManager(context: Context) {

    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val mSensorThread: HandlerThread = HandlerThread("sensor_thread")

    private var mDSensorEventProcessor: DSensorEventProcessor? = null

    private val mRegisterResult: RegisterResult = RegisterResult()

    fun listSensor(): List<Sensor> = mSensorManager.getSensorList(Sensor.TYPE_ALL)

    fun hasSensor(sensorType: Int): Boolean = mSensorManager.getDefaultSensor(sensorType) != null

    fun getErrors(): Set<Int> {
        return mRegisterResult.mErrorList
    }

    fun startDProcessedSensor(context: Context,
                              dProcessedSensorType: Int,
                              dProcessedEventListener: DProcessedEventListener,
                              sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                              historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        logger(DSensorManager::class.java.simpleName, "startDSensor($dProcessedSensorType)")
        when (dProcessedSensorType) {
            DProcessedSensor.TYPE_3D_COMPASS -> return onType3DCompassRegistered(context,
                dProcessedEventListener, sensorRate, historyMaxLength)

            DProcessedSensor.TYPE_COMPASS -> return onTypeCompassRegistered(context, dProcessedEventListener,
                sensorRate, historyMaxLength)

            DProcessedSensor.TYPE_3D_COMPASS_AND_DEPRECIATED_ORIENTATION -> return onType3DCompassAndOrientationRegistered(
                context, dProcessedEventListener, sensorRate, historyMaxLength)

            DProcessedSensor.TYPE_COMPASS_AND_DEPRECIATED_ORIENTATION -> return onTypeCompassAndOrientationRegistered(
                context, dProcessedEventListener,  sensorRate, historyMaxLength)

            else -> mRegisterResult.mErrorList.add(ERROR_UNSUPPORTED_TYPE)
        }

        return false
    }

    private fun onType3DCompassRegistered(context: Context, dProcessedEventListener: DProcessedEventListener,
                                         sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                                         historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        val dSensorDirectionTypes = getCompassDirectionType(context) or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION
        logger(DSensorManager::class.java.simpleName, "onType3DCompassRegistered dSensorDirectionTypes = $dSensorDirectionTypes")
        val flag = startDSensor(dSensorDirectionTypes, Compass3DSensorEventListener(dProcessedEventListener),
            sensorRate, historyMaxLength)
        if (!flag) {
            stopDSensor()
        }

        return flag
    }

    private fun onTypeCompassRegistered(context: Context, dProcessedEventListener: DProcessedEventListener,
                                        sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                                        historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        val dSensorDirectionTypes = getCompassDirectionType(context) or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION
        logger(DSensorManager::class.java.simpleName, "onTypeCompassRegistered dSensorDirectionTypes = $dSensorDirectionTypes")
        val flag = startDSensor(dSensorDirectionTypes, CompassSensorEventListener(dProcessedEventListener),
            sensorRate, historyMaxLength)
        if (!flag) {
            stopDSensor()
        }

        return flag
    }

    private fun onType3DCompassAndOrientationRegistered(context: Context, dProcessedEventListener: DProcessedEventListener,
                                          sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                                          historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        val dSensorDirectionTypes = getCompassDirectionType(context) or DSensor.TYPE_MINUS_Z_AXIS_DIRECTION or DSensor.TYPE_DEPRECIATED_ORIENTATION
        logger(DSensorManager::class.java.simpleName, "onType3DCompassRegistered dSensorDirectionTypes = $dSensorDirectionTypes")
        val flag = startDSensor(dSensorDirectionTypes, Compass3DAndOrientationSensorEventListener(dProcessedEventListener),
            sensorRate, historyMaxLength)
        if (!flag) {
            stopDSensor()
        }

        return flag
    }

    private fun onTypeCompassAndOrientationRegistered(context: Context, dProcessedEventListener: DProcessedEventListener,
                                                        sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                                                        historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        val dSensorDirectionTypes = getCompassDirectionType(context) or DSensor.TYPE_DEPRECIATED_ORIENTATION
        logger(DSensorManager::class.java.simpleName, "onType3DCompassRegistered dSensorDirectionTypes = $dSensorDirectionTypes")
        val flag = startDSensor(dSensorDirectionTypes, CompassAndOrientationSensorEventListener(dProcessedEventListener),
            sensorRate, historyMaxLength)
        if (!flag) {
            stopDSensor()
        }

        return flag
    }

    private fun getCompassDirectionType(context: Context): Int {
        val rotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation
        return when(rotation) {
            Surface.ROTATION_90 -> DSensor.TYPE_X_AXIS_DIRECTION
            Surface.ROTATION_180 -> DSensor.TYPE_MINUS_Y_AXIS_DIRECTION
            Surface.ROTATION_270 -> DSensor.TYPE_MINUS_X_AXIS_DIRECTION
            else -> DSensor.TYPE_Y_AXIS_DIRECTION
        }
    }

    /**
     * Start DSensor processing, processed results are in the DProcessedSensorEvent parameter of
     * onDSensorChanged method of the DSensorEventListener callback.
     * @param dSensorTypes Bitwise OR of DSensor types
     * @param dSensorEventListener callback
     * @param sensorRate Sensor rate using android SensorManager rate constants, i.e. SensorManager.SENSOR_DELAY_FASTEST.
     * @param historyMaxLength max history size for averaging.
     * @return true if device has all sensors or can be calculated from other sensors in sensorTypes.
     * Otherwise false (call getErrors for a list of errors).
     */
    fun startDSensor(dSensorTypes: Int,
                     dSensorEventListener: DSensorEventListener,
                     sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                     historyMaxLength: Int = DEFAULT_HISTORY_SIZE
                    ): Boolean {
        logger(DSensorManager::class.java.simpleName, "startDSensor($dSensorTypes, $sensorRate' $historyMaxLength)")

        mRegisterResult.mErrorList.clear()
        mRegisterResult.mSensorRegisteredList.clear()

        if (mDSensorEventProcessor != null) {
            stopDSensor()
            mSensorThread.start()
        }

        mDSensorEventProcessor = DSensorEventProcessor(dSensorTypes, dSensorEventListener, Handler(mSensorThread.looper),
            mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null,
            mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null, historyMaxLength)

        registerListener(dSensorTypes, sensorRate)

        return mRegisterResult.mErrorList.isEmpty()
    }

    private fun registerListener(dSensorTypes: Int, sensorRate: Int) {
        if (dSensorTypes and (DSensor.TYPE_DEVICE_ACCELEROMETER or DSensor.TYPE_WORLD_ACCELEROMETER) != 0) {
            registerListener(Sensor.TYPE_ACCELEROMETER, sensorRate, TYPE_ACCELEROMETER_NOT_AVAILABLE)
        }

        if (dSensorTypes and (DSensor.TYPE_DEVICE_MAGNETIC_FIELD or DSensor.TYPE_WORLD_MAGNETIC_FIELD) != 0) {
            registerListener(Sensor.TYPE_MAGNETIC_FIELD, sensorRate, TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)
        }

        if (dSensorTypes and DSensor.TYPE_GYROSCOPE != 0) {
            registerListener(Sensor.TYPE_GYROSCOPE, sensorRate, TYPE_GYROSCOPE_NOT_AVAILABLE)
        }

        if (dSensorTypes and DSensor.TYPE_ROTATION_VECTOR != 0) {
            registerListener(Sensor.TYPE_ROTATION_VECTOR, sensorRate, TYPE_ROTATION_VECTOR_NOT_AVAILABLE)
        }

        if (dSensorTypes and DSensor.TYPE_DEPRECIATED_ORIENTATION != 0) {
            registerListener(Sensor.TYPE_ORIENTATION, sensorRate, TYPE_ORIENTATION_NOT_AVAILABLE)
        }

        if (dSensorTypes and (DSensor.TYPE_DEVICE_GRAVITY or DSensor.TYPE_WORLD_GRAVITY) != 0) {
            registerGravityListener(sensorRate)
        }

        if (dSensorTypes and (DSensor.TYPE_DEVICE_LINEAR_ACCELERATION or DSensor.TYPE_WORLD_LINEAR_ACCELERATION) != 0) {
            registerLinearAccelerationListener(sensorRate)
        }

        if (worldTypesRegistered(dSensorTypes) || directionTypesRegistered(dSensorTypes)) {
            registerSensorListenerForRotationMatrix(sensorRate)
        }
    }

    private fun registerGravityListener(sensorRate: Int) {
        registerListener(Sensor.TYPE_GRAVITY, sensorRate, TYPE_GRAVITY_NOT_AVAILABLE)
        if (mRegisterResult.mErrorList.contains(TYPE_GRAVITY_NOT_AVAILABLE)) {
            if (!mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_ACCELEROMETER)) {
                registerListener(Sensor.TYPE_ACCELEROMETER, sensorRate, TYPE_ACCELEROMETER_NOT_AVAILABLE)
            }

            if (mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_ACCELEROMETER)) {
                mRegisterResult.mErrorList.remove(TYPE_GRAVITY_NOT_AVAILABLE)
                mRegisterResult.mSensorRegisteredList.add(Sensor.TYPE_GRAVITY)
            }
        }
    }

    private fun registerLinearAccelerationListener(sensorRate: Int) {
        registerListener(Sensor.TYPE_LINEAR_ACCELERATION, sensorRate, TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)
        if (mRegisterResult.mErrorList.contains(TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)) {
            if (!mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_ACCELEROMETER)) {
                registerListener(Sensor.TYPE_ACCELEROMETER, sensorRate, TYPE_ACCELEROMETER_NOT_AVAILABLE)
            }

            registerGravityListener(sensorRate)
            if (mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_ACCELEROMETER)) {
                mRegisterResult.mErrorList.remove(TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE)
                mRegisterResult.mSensorRegisteredList.add(Sensor.TYPE_LINEAR_ACCELERATION)
            }
        }
    }

    private fun registerSensorListenerForRotationMatrix(sensorRate: Int) {
        if (!mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_MAGNETIC_FIELD)) {
            registerListener(Sensor.TYPE_MAGNETIC_FIELD, sensorRate, TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)
        }

        if (mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_MAGNETIC_FIELD)) {
            registerGravityListener(sensorRate)
        }
    }

    /**
     * Register sensors
     * @param sensorType One of SDK Sensor type.
     * @param sensorRate One of SensorManager.SENSOR_DELAY_*
     * @param errorValue Error to return if sensorType not available.
     */
    private fun registerListener(sensorType: Int, sensorRate: Int, errorValue: Int) {
        logger(DSensorManager::class.java.simpleName, "registerListener($sensorType, $sensorRate, $errorValue)")
        if (mSensorManager.registerListener(mDSensorEventProcessor, mSensorManager.getDefaultSensor(sensorType),
            sensorRate, Handler(mSensorThread.looper))) {
            mRegisterResult.mSensorRegisteredList.add(sensorType)
        } else {
            mRegisterResult.mErrorList.add(errorValue)
        }
    }

    fun stopDSensor() {
        logger(DSensorManager::class.java.simpleName, "stopDSensor")

        if (mSensorThread.isAlive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mSensorThread.quitSafely()
            } else {
                mSensorThread.quit()
            }
        }

        if (mDSensorEventProcessor != null) {
            mSensorManager.unregisterListener(mDSensorEventProcessor)
            mDSensorEventProcessor = null
        }
    }

    /**
     * Check if TYPE_WORLD_* is registered
     * @param dSensorTypes Bitwise OR of DSensor types
     * @return true if dSensorTypes is of TYPE_WORLD_*
     */
    private fun worldTypesRegistered(dSensorTypes: Int): Boolean {
        return (dSensorTypes and DSensor.TYPE_WORLD_ACCELEROMETER == 0
                && dSensorTypes and DSensor.TYPE_WORLD_GRAVITY == 0
                && dSensorTypes and DSensor.TYPE_WORLD_MAGNETIC_FIELD == 0
                && dSensorTypes and DSensor.TYPE_WORLD_LINEAR_ACCELERATION == 0)
    }

    /**
     * Check if TYPE_*_DIRECTION is registered
     * @param dSensorTypes Bitwise OR of DSensor types
     * @return true if dSensorTypes is of TYPE_*_DIRECTION
     */
    private fun directionTypesRegistered(dSensorTypes: Int): Boolean {
        return (dSensorTypes and DSensor.TYPE_Z_AXIS_DIRECTION == 0
                && dSensorTypes and DSensor.TYPE_MINUS_Z_AXIS_DIRECTION == 0
                && dSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION == 0
                && dSensorTypes and DSensor.TYPE_MINUS_X_AXIS_DIRECTION == 0
                && dSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION == 0
                && dSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION == 0)
    }

    private class RegisterResult {
        val mErrorList = HashSet<Int>()
        val mSensorRegisteredList = HashSet<Int>()
    }

    private class Compass3DSensorEventListener(private val dProcessedEventListener: DProcessedEventListener): DSensorEventListener {

        override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
            val result: DSensorEvent? = when {
                processedSensorEvent.minusZAxisDirection!!.values[0].isNaN() -> when {
                    changedDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION == 0 -> when {
                        changedDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION == 0 -> when {
                            changedDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION == 0 -> processedSensorEvent.minusXAxisDirection
                            else -> processedSensorEvent.xAxisDirection
                        }
                        else -> processedSensorEvent.minusYAxisDirection
                    }
                    else -> processedSensorEvent.yAxisDirection
                }
                else -> processedSensorEvent.minusZAxisDirection
            }

            if (result != null) {
                dProcessedEventListener.onProcessedValueChanged(DSensorEvent(DProcessedSensor.TYPE_3D_COMPASS,
                    result.accuracy, result.timestamp, result.values))
            }
        }
    }

    private class CompassSensorEventListener(private val dProcessedEventListener: DProcessedEventListener): DSensorEventListener {

        override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
            val result: DSensorEvent? = when {
                changedDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION == 0 -> when {
                    changedDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION == 0 -> when {
                        changedDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION == 0 -> processedSensorEvent.minusXAxisDirection
                        else -> processedSensorEvent.xAxisDirection
                    }
                    else -> processedSensorEvent.minusYAxisDirection
                }
                else -> processedSensorEvent.yAxisDirection
            }

            if (result != null) {
                dProcessedEventListener.onProcessedValueChanged(DSensorEvent(DProcessedSensor.TYPE_COMPASS,
                    result.accuracy, result.timestamp, result.values))
            }
        }
    }

    private class Compass3DAndOrientationSensorEventListener(private val dProcessedEventListener: DProcessedEventListener): DSensorEventListener {
        override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
            val result: DSensorEvent? = when {
                changedDSensorTypes and DSensor.TYPE_DEPRECIATED_ORIENTATION == 0 -> when {
                    processedSensorEvent.minusZAxisDirection!!.values[0].isNaN() -> when {
                        changedDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION == 0 -> when {
                            changedDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION == 0 -> when {
                                changedDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION == 0 -> processedSensorEvent.minusXAxisDirection
                                else -> processedSensorEvent.xAxisDirection
                            }
                            else -> processedSensorEvent.minusYAxisDirection
                        }
                        else -> processedSensorEvent.yAxisDirection
                    }
                    else -> processedSensorEvent.minusZAxisDirection
                }
                else -> processedSensorEvent.depreciatedOrientation
            }

            if (result != null) {
                val sensorType =
                    if (changedDSensorTypes and DSensor.TYPE_DEPRECIATED_ORIENTATION == 0)
                        DProcessedSensor.TYPE_3D_COMPASS
                    else
                        DSensor.TYPE_DEPRECIATED_ORIENTATION

                dProcessedEventListener.onProcessedValueChanged(DSensorEvent(sensorType,
                    result.accuracy, result.timestamp, result.values))
            }
        }
    }

    private class CompassAndOrientationSensorEventListener(private val dProcessedEventListener: DProcessedEventListener): DSensorEventListener {

        override fun onDSensorChanged(changedDSensorTypes: Int, processedSensorEvent: DProcessedSensorEvent) {
            val result: DSensorEvent? = when {
                changedDSensorTypes and DSensor.TYPE_DEPRECIATED_ORIENTATION == 0 -> when {
                    changedDSensorTypes and DSensor.TYPE_Y_AXIS_DIRECTION == 0 -> when {
                        changedDSensorTypes and DSensor.TYPE_MINUS_Y_AXIS_DIRECTION == 0 -> when {
                            changedDSensorTypes and DSensor.TYPE_X_AXIS_DIRECTION == 0 -> processedSensorEvent.minusXAxisDirection
                            else -> processedSensorEvent.xAxisDirection
                        }
                        else -> processedSensorEvent.minusYAxisDirection
                    }
                    else -> processedSensorEvent.yAxisDirection
                }
                else -> processedSensorEvent.depreciatedOrientation
            }

            if (result != null) {
                val sensorType =
                    if (changedDSensorTypes and DSensor.TYPE_DEPRECIATED_ORIENTATION == 0)
                        DProcessedSensor.TYPE_COMPASS
                    else
                        DSensor.TYPE_DEPRECIATED_ORIENTATION

                dProcessedEventListener.onProcessedValueChanged(DSensorEvent(sensorType,
                        result.accuracy, result.timestamp, result.values))
            }
        }
    }
}
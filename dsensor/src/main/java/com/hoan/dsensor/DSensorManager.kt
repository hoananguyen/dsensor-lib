package com.hoan.dsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
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

class DSensorManager(context: Context): SensorEventListener {

    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val mSensorThread: HandlerThread = HandlerThread("sensor_thread")

    private var mDSensorEventProcessor: DSensorEventProcessorImp? = null

    private val mRegisterResult: RegisterResult = RegisterResult()

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent) {
        mDSensorEventProcessor?.run {
            onDSensorChanged(DSensorEvent(getDSensorType(event.sensor.type), event.accuracy, event.timestamp, event.values))
        }
    }

    fun listSensor(): List<Sensor> = mSensorManager.getSensorList(Sensor.TYPE_ALL)

    fun getErrors(): Set<Int> {
        return mRegisterResult.mErrorList
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
                     historyMaxLength: Int = DEFAULT_HISTORY_SIZE): Boolean {
        logger(DSensorManager::class.java.simpleName, "startDSensor($dSensorTypes, $sensorRate $historyMaxLength)")

        mRegisterResult.mErrorList.clear()
        mRegisterResult.mSensorRegisteredList.clear()

        if (mDSensorEventProcessor != null) {
            stopDSensor()
        }

        mSensorThread.start()

        mDSensorEventProcessor = DSensorEventProcessorImp(dSensorTypes, dSensorEventListener,
            mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null,
            mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null,
            historyMaxLength)

        registerListener(dSensorTypes, sensorRate)

        if (mRegisterResult.mSensorRegisteredList.isEmpty() && mRegisterResult.mErrorList.isEmpty()) {
            mRegisterResult.mErrorList.add(ERROR_UNSUPPORTED_TYPE)
            return false
        }

        return mRegisterResult.mErrorList.isEmpty()
    }

    private fun registerListener(dSensorTypes: Int, sensorRate: Int) {
        if (dSensorTypes and (TYPE_DEVICE_ACCELEROMETER or TYPE_WORLD_ACCELEROMETER) != 0) {
            registerListener(Sensor.TYPE_ACCELEROMETER, sensorRate, TYPE_ACCELEROMETER_NOT_AVAILABLE)
        }

        if (dSensorTypes and (TYPE_DEVICE_MAGNETIC_FIELD or TYPE_WORLD_MAGNETIC_FIELD) != 0) {
            registerListener(Sensor.TYPE_MAGNETIC_FIELD, sensorRate, TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)
        }

        if (dSensorTypes and TYPE_GYROSCOPE != 0) {
            registerListener(Sensor.TYPE_GYROSCOPE, sensorRate, TYPE_GYROSCOPE_NOT_AVAILABLE)
        }

        if (dSensorTypes and TYPE_ROTATION_VECTOR != 0) {
            registerListener(Sensor.TYPE_ROTATION_VECTOR, sensorRate, TYPE_ROTATION_VECTOR_NOT_AVAILABLE)
        }

        if (dSensorTypes and TYPE_DEPRECATED_ORIENTATION != 0) {
            @Suppress("DEPRECATION")
            registerListener(Sensor.TYPE_ORIENTATION, sensorRate, TYPE_ORIENTATION_NOT_AVAILABLE)
        }

        if (dSensorTypes and (TYPE_DEVICE_GRAVITY or TYPE_WORLD_GRAVITY) != 0) {
            registerGravityListener(sensorRate)
        }

        if (dSensorTypes and (TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION) != 0) {
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

            if (mRegisterResult.mSensorRegisteredList.contains(Sensor.TYPE_ACCELEROMETER)) {
                registerGravityListener(sensorRate)
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
        if (mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(sensorType),
            sensorRate, Handler(mSensorThread.looper))) {
            mRegisterResult.mSensorRegisteredList.add(sensorType)
        } else {
            mRegisterResult.mErrorList.add(errorValue)
        }
    }

    fun stopDSensor() {
        logger(DSensorManager::class.java.simpleName, "stopDSensor")

        mSensorManager.unregisterListener(this)

        mDSensorEventProcessor?.run {
            finish()
        }

        mDSensorEventProcessor = null

        if (mSensorThread.isAlive) {
            mSensorThread.quit()
        }
    }

    /**
     * Check if TYPE_WORLD_* is registered
     * @param dSensorTypes Bitwise OR of DSensor types
     * @return true if dSensorTypes is of TYPE_WORLD_*
     */
    private fun worldTypesRegistered(dSensorTypes: Int): Boolean {
        return (dSensorTypes and TYPE_WORLD_ACCELEROMETER != 0
                || dSensorTypes and TYPE_WORLD_GRAVITY != 0
                || dSensorTypes and TYPE_WORLD_MAGNETIC_FIELD != 0
                || dSensorTypes and TYPE_WORLD_LINEAR_ACCELERATION != 0)
    }

    /**
     * Check if TYPE_*_DIRECTION is registered
     * @param dSensorTypes Bitwise OR of DSensor types
     * @return true if dSensorTypes is of TYPE_*_DIRECTION
     */
    private fun directionTypesRegistered(dSensorTypes: Int): Boolean {
        return (dSensorTypes and TYPE_Z_AXIS_DIRECTION != 0
                || dSensorTypes and TYPE_NEGATIVE_Z_AXIS_DIRECTION != 0
                || dSensorTypes and TYPE_X_AXIS_DIRECTION != 0
                || dSensorTypes and TYPE_NEGATIVE_X_AXIS_DIRECTION != 0
                || dSensorTypes and TYPE_Y_AXIS_DIRECTION != 0
                || dSensorTypes and TYPE_NEGATIVE_Y_AXIS_DIRECTION != 0)
    }

    private class RegisterResult {
        val mErrorList = HashSet<Int>()
        val mSensorRegisteredList = HashSet<Int>()
    }
}
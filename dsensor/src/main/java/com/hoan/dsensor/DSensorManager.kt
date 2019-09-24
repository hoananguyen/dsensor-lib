package com.hoan.dsensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.HandlerThread
import com.hoan.dsensor.storage.DSensorRepository
import com.hoan.dsensor.storage.SensorData
import com.hoan.dsensor.storage.Session
import com.hoan.dsensor.utils.logger
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

const val ERROR_UNSUPPORTED_TYPE = -1

const val TYPE_ACCELEROMETER_NOT_AVAILABLE = 2
const val TYPE_MAGNETIC_FIELD_NOT_AVAILABLE = 4
const val TYPE_GRAVITY_NOT_AVAILABLE = 8
const val TYPE_LINEAR_ACCELERATION_NOT_AVAILABLE = 16
const val TYPE_GYROSCOPE_NOT_AVAILABLE = 32
const val TYPE_ROTATION_VECTOR_NOT_AVAILABLE = 64
const val TYPE_ORIENTATION_NOT_AVAILABLE = 128

private const val NOT_SAVE = 0
private const val SAVE = 1
private const val SAVE_ONLY = 2

class DSensorManager(context: Context): SensorEventListener {

    private val mSensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private var mSensorThread: HandlerThread = HandlerThread("sensor_thread")

    private var mDSensorEventProcessor: DSensorEventProcessorImp? = null

    private val mRegisterResult: RegisterResult = RegisterResult()

    private var mSessionId = AtomicLong(0)

    private var mSaveData: Int = NOT_SAVE

    private val mToBeSavedList = Collections.synchronizedList(ArrayList<DSensorEvent>())

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    private val mCoroutineScope = CoroutineScope(newSingleThreadContext("retrieve_data_thread"))

    private val mDSensorRepository: DSensorRepository = DSensorRepository(context)

    fun listSensor(): List<Sensor> = mSensorManager.getSensorList(Sensor.TYPE_ALL)

    fun getErrors(): Set<Int> {
        return mRegisterResult.mErrorList
    }

    suspend fun lastSession(): Session? {
        return mDSensorRepository.lastSession()
    }

    suspend fun allSessions(): List<Session>? {
        return mDSensorRepository.allSessions()
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    suspend fun lastSessionData(): DSensorData? {
        val lastSession = withContext(Dispatchers.IO) {
            mDSensorRepository.lastSession()
        }?: return null
        sensorDataForSession(lastSession)
        return mDSensorEventProcessor?.getSensorData()
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    suspend fun sensorDataForSession(session: Session): DSensorData? {
        mDSensorEventProcessor = DSensorEventProcessorImp(session.dSensorTypes, session.hasGravity, session.hasLinearAcceleration)
        val sensorDataList = withContext(Dispatchers.IO) {
            mDSensorRepository.sensorDataForSession(session.id)
        }
        mCoroutineScope.launch {
            for (sensorData in sensorDataList) {
                mDSensorEventProcessor?.onDSensorChanged(sensorData.getDSensorEvent())
            }
            stopDSensor()
        }
        return mDSensorEventProcessor?.getSensorData()
    }

    /**
     * Start DSensor processing, processed results are in the DProcessedSensorEvent parameter of
     * onDSensorChanged method of the DSensorEventListener callback.
     * @param dSensorTypes Bitwise OR of DSensor types
     * @param sensorRate Sensor rate using android SensorManager rate constants, i.e. SensorManager.SENSOR_DELAY_FASTEST.
     * @param historyMaxLength max history size for averaging.
     * @return true if device has all sensors or can be calculated from other sensors in sensorTypes.
     * Otherwise false (call getErrors for a list of errors).
     */
    @kotlinx.coroutines.ObsoleteCoroutinesApi
    fun startDSensor(dSensorTypes: Int,
                     saveData: Boolean = false,
                     sessionName: String? = null,
                     sensorRate: Int = SensorManager.SENSOR_DELAY_NORMAL,
                     historyMaxLength: Int = DEFAULT_HISTORY_SIZE): DSensorData? {
        logger(DSensorManager::class.java.simpleName, "startDSensor($dSensorTypes, $saveData, $sessionName, $sensorRate $historyMaxLength)")

        mSessionId.set(0)
        mSaveData = NOT_SAVE

        val hasGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY) != null
        val hasLinearAccelerationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION) != null
        if (saveData) {
            mSaveData = SAVE
            saveSession(dSensorTypes, sessionName, hasGravitySensor, hasLinearAccelerationSensor)
        }

        clearRegisterResult()

        if (mDSensorEventProcessor != null) {
            stopDSensor()
        }

        mSensorThread = HandlerThread("sensor_thread")
        mSensorThread.start()

        mDSensorEventProcessor = DSensorEventProcessorImp(dSensorTypes, hasGravitySensor,
            hasLinearAccelerationSensor, historyMaxLength)

        registerListener(dSensorTypes, sensorRate, hasGravitySensor)

        if (mRegisterResult.mSensorRegisteredList.isEmpty() && mRegisterResult.mErrorList.isEmpty()) {
            mRegisterResult.mErrorList.add(ERROR_UNSUPPORTED_TYPE)
            return null
        }
        return mDSensorEventProcessor?.getSensorData()
    }

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    private fun saveSession(dSensorTypes: Int, sessionName: String?, hasGravitySensor: Boolean, hasLinearAccelerationSensor: Boolean) {
        mCoroutineScope.launch {
            var sessionId = 1L
            withContext(Dispatchers.IO) {
                mDSensorRepository.lastSession()?.apply {
                    sessionId = id + 1
                }
                mDSensorRepository.insert(
                    Session(
                        sessionId, dSensorTypes, System.currentTimeMillis(),
                        sessionName, hasGravitySensor, hasLinearAccelerationSensor
                    )
                )
                mSessionId.set(sessionId)
            }
            logger("DSensorManager", "id = $mSessionId")
        }
    }

    private fun clearRegisterResult() {
        mRegisterResult.mErrorList.clear()
        mRegisterResult.mSensorRegisteredList.clear()
    }

    private fun registerListener(dSensorTypes: Int, sensorRate: Int, hasGravitySensor: Boolean) {
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

        if(dSensorTypes and (TYPE_INCLINATION or TYPE_DEVICE_ROTATION or TYPE_ROLL or TYPE_PITCH) != 0) {
            registerForCalculationTypesWithGravity(sensorRate, hasGravitySensor)
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
     * TYPE_INCLINATION, TYPE_DEVICE_ROTATION, TYPE_PITCH and TYPE_ROLL can be calculate using rotation matrix
     * or gravity. Normally if the device has gravity sensor it also has magnetic field sensor, thus if the device
     * has gravity sensor then we assume that it also has magnetic sensor and thus report error if it does not
     * have magnetic sensor (usually it is a defect chip that causes the magnetic field sensor to die in this case).
     * So we only calculate the types above only if the device does not have gravity sensor. Of course we can do
     * the calculation if the device has gravity but no magnetic sensor, but then we have to add hasMagneticField
     * sensor param to startDSensor and thus make the code a lot more complicated. The types above are normally
     * use for direction types and without magnetic field sensor, direction types are not possible and thus it is
     * not worth the trouble to make the code more complicated for these edge use cases.
     */
    private fun registerForCalculationTypesWithGravity(sensorRate: Int, hasGravitySensor: Boolean) {
        if (hasGravitySensor && !mRegisterResult.mSensorRegisteredList.contains(TYPE_DEVICE_MAGNETIC_FIELD) &&
            !mRegisterResult.mErrorList.contains(TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)) {
            registerListener(Sensor.TYPE_MAGNETIC_FIELD, sensorRate, TYPE_MAGNETIC_FIELD_NOT_AVAILABLE)
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

    @kotlinx.coroutines.ObsoleteCoroutinesApi
    fun stopDSensor() {
        logger(DSensorManager::class.java.simpleName, "stopDSensor isAlive = ${mSensorThread.isAlive}")

        mSensorManager.unregisterListener(this)

        mDSensorEventProcessor?.apply {
            finish()
            mDSensorEventProcessor = null
        }

        if (mSensorThread.isAlive) {
            mSensorThread.quit()
        }

        if (mToBeSavedList.isNotEmpty()) {
            saveSensorDataList()
        }

        if (mCoroutineScope.isActive) {
            mCoroutineScope.cancel()
        }
    }

    private fun saveSensorDataList() {
        logger("DSensorManager", "saveSensorDataList: size = ${mToBeSavedList.size}")
        val sensorDataList = ArrayList<SensorData>(mToBeSavedList.size)
        synchronized(mToBeSavedList) {
            while (mToBeSavedList.isNotEmpty()) {
                sensorDataList.add(SensorData(mSessionId.get(), mToBeSavedList.removeAt(0)))
            }
        }
        CoroutineScope(Dispatchers.IO).launch {
            mDSensorRepository.insert(sensorDataList)
            mToBeSavedList.clear()
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

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {

    }

    private var i = 1
    override fun onSensorChanged(event: SensorEvent) {
        logger("DSensorManager", "onSensorChanged: ${event.sensor.name} value = ${event.values!!.contentToString()} i = ${i++}")
        val dSensorEvent = DSensorEvent(getDSensorType(event.sensor.type), event.accuracy, event.timestamp, event.values)
        if (mSaveData != NOT_SAVE) {
            saveData(dSensorEvent)
            if (mSaveData == SAVE_ONLY) return
        }
        mDSensorEventProcessor?.run {
            onDSensorChanged(dSensorEvent)
        }
    }

    private fun saveData(dSensorEvent: DSensorEvent) {
        logger("DSensorManager", "saveData: mSessionId = $mSessionId")
        when (mSessionId.get()){
            0L -> {
                synchronized(mToBeSavedList) {
                    mToBeSavedList.add(dSensorEvent)
                }
            }
            else -> {
                CoroutineScope(Dispatchers.IO).launch {
                    mDSensorRepository.insert(SensorData(mSessionId.get(), dSensorEvent))
                }
            }
        }
    }

    private class RegisterResult {
        val mErrorList = HashSet<Int>()
        val mSensorRegisteredList = HashSet<Int>()
    }
}
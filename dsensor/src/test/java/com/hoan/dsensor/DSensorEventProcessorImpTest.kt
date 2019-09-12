package com.hoan.dsensor

import android.hardware.SensorManager
import android.util.Log
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(Log::class, SensorManager::class)
class DSensorEventProcessorImpTest {

    @Before
    fun setUp() {
        PowerMockito.mockStatic(Log::class.java)
        Mockito.`when`(Log.e(any(), any())).then {
            println(it.arguments[1] as String)
            1
        }

        PowerMockito.mockStatic(SensorManager::class.java)
        Mockito.`when`(SensorManager.getRotationMatrix(any(), any(), any(), any())).thenReturn(true)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_one_key_type_device_accelerometer_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_accelerometer_only() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_ACCELEROMETER)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 0, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size() == 1).isTrue()
        assertThat(dSensorData.data[TYPE_DEVICE_ACCELEROMETER]).isNotNull()
        assertThat(dSensorData.data[TYPE_DEVICE_ACCELEROMETER]).isEqualTo(dSensorEvent)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_one_key_type_device_gravity_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_gravity_only_and_device_has_gravity_sensor() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_GRAVITY)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 0, floatArrayOf(-6.225158E-5f, 9.772965f, 0.8121315f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size() == 1).isTrue()
        assertThat(dSensorData.data[TYPE_DEVICE_GRAVITY]).isNotNull()
        assertThat(dSensorData.data[TYPE_DEVICE_GRAVITY]).isEqualTo(dSensorEvent)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_one_key_type_device_gravity_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_gravity_and_device_has_no_gravity_sensor_and_onSensorChanged_is_called_with_parameter_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_GRAVITY, false)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 0, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_GRAVITY]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_one_key_type_device_magnetic_field_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_MAGNETIC_FIELD)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 0, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_MAGNETIC_FIELD]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_one_key_type_device_linear_acceleration_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_linear_acceleration_and_device_has_no_linear_acceleration_and_gravity_sensors_and_onSensorChanged_is_called_with_parameter_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_is_empty_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_linear_acceleration_and_device_has_no_linear_acceleration_and_gravity_sensors_and_onDSensorChanged_has_not_been_called_with_param_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_is_empty_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_linear_acceleration_and_device_has_no_linear_acceleration_but_has_gravity_sensors_and_onDSensorChanged_has_not_been_called_with_type_device_gravity_param() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_is_empty_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_linear_acceleration_and_device_has_no_linear_acceleration_but_has_gravity_sensors_and_onDSensorChanged_has_not_been_called_with_type_device_accelerometer_param() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_property_has_only_keys_type_device_linear_acceleration_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_type_device_linear_acceleration_and_device_has_no_linear_acceleration_but_has_gravity_sensors_and_onDSensorChanged_has_been_called_with_type_device_accelerometer_param_and_type_device_gravity_param() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_2_and_has_keys_type_device_gravity_and_type_device_linear_acceleration_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_both_types_and_device_has_neither_linear_acceleration_nor_gravity_sensors_and_onDSensorChanged_has_been_called_with_param_of_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_DEVICE_GRAVITY, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(2)
        assertThat(dSensorData.data[TYPE_DEVICE_LINEAR_ACCELERATION]).isNotNull()
        assertThat(dSensorData.data[TYPE_DEVICE_GRAVITY]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_both_types_and_device_has_no_linear_acceleration_but_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_param_of_type_device_accelerometer_but_not_of_type_device_gravity() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_DEVICE_GRAVITY, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_2_and_has_keys_type_device_gravity_and_type_device_linear_acceleration_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_both_types_and_device_has_no_linear_acceleration_but_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_param_of_type_device_accelerometer_and_type_device_gravity() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_DEVICE_GRAVITY, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(2)
        assertThat(dSensorData.data[TYPE_DEVICE_LINEAR_ACCELERATION]).isNotNull()
        assertThat(dSensorData.data[TYPE_DEVICE_GRAVITY]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_linear_acceleration_and_device_has_linear_acceleration_and_onDSensorChanged_has_not_been_called_with_param_of_type_device_linear_acceleration() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = true)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_and_has_key_type_device_linear_acceleration_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_linear_acceleration_and_device_has_linear_acceleration_and_onDSensorChanged_has_been_called_with_param_of_type_device_linear_acceleration() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_accelerometer_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_and_has_key_type_world_accelerometer_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_accelerometer_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_ACCELEROMETER]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_accelerometer_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_accelerometer_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_gravity_and_type_device_magnetic_field_but_has_not_been_called_with_type_device_acceleromter() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_and_has_key_type_world_accelerometer_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_accelerometer_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_all_type_device_gravity_and_type_device_magnetic_field_and_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_ACCELEROMETER, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_ACCELEROMETER]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_gravity_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_type_device_accelerometer_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_and_has_key_type_world_gravity_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_gravity_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_GRAVITY]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_gravity_and_device_has_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_gravity_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_GRAVITY, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_GRAVITY]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_magnetic_field_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_both_called_with_type_device_accelerometer_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_and_has_key_type_world_magnetic_field_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_magnetic_field_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_MAGNETIC_FIELD]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_magnetic_field_and_device_has_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_magnetic_field_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_MAGNETIC_FIELD, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_MAGNETIC_FIELD]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_neither_gravity_nor_linear_acceleration_sensors_and_onDSensorChanged_has_not_been_both_called_with_type_device_accelerometer_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_neither_gravity_nor_linear_acceleration_sensors_and_onDSensorChanged_has_been_both_called_with_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_gravity_but_no_linear_acceleration_sensor_and_onDSensorChanged_has_not_been_called_with_all_type_device_gravity_and_type_device_magnetic_field_and_type_device_acceleromter() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_gravity_but_no_linear_acceleration_sensor_and_onDSensorChanged_has_been_called_with_all_type_device_gravity_and_type_device_magnetic_field_and_type_device_acceleromter() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_no_gravity_but_has_linear_acceleration_sensor_and_onDSensorChanged_has_not_been_called_with_all_type_device_linear_acceleration_and_type_device_magnetic_field_and_type_device_acceleromter() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = true)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = true)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_no_gravity_and_linear_acceleration_sensors_and_onDSensorChanged_has_not_been_called_with_all_type_device_linear_acceleration_and_type_device_magnetic_field_and_type_device_gravity() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = false, hasLinearAccelerationSensor = true)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_both_gravity_but_has_linear_acceleration_sensor_and_onDSensorChanged_has_not_been_called_with_all_type_device_linear_acceleration_and_type_device_magnetic_field_and_type_device_acceleromter() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_world_linear_acceleration_and_device_has_both_gravity_and_linear_acceleration_sensor_and_onDSensorChanged_has_been_called_with_all_type_device_linear_acceleration_and_type_device_magnetic_field_and_type_device_gravity() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_WORLD_LINEAR_ACCELERATION, hasGravitySensor = true, hasLinearAccelerationSensor = true)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(0.0f, 9.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_LINEAR_ACCELERATION, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_WORLD_LINEAR_ACCELERATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_direction_types_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_X_AXIS_DIRECTION, hasGravitySensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_NEGATIVE_Z_AXIS_DIRECTION, hasGravitySensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_direction_types_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_accelerometer_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_X_AXIS_DIRECTION, hasGravitySensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_X_AXIS_DIRECTION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_direction_types_and_device_has_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_NEGATIVE_Y_AXIS_DIRECTION)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_Z_AXIS_DIRECTION, hasGravitySensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(1.0f, 7.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(0)
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_direction_types_and_device_has_gravity_sensor_and_onDSensorChanged_has_been_called_with_both_type_device_gravity_and_type_device_magnetic_field() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_NEGATIVE_Z_AXIS_DIRECTION)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_DEVICE_MAGNETIC_FIELD, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        dSensorEvent = DSensorEvent(TYPE_DEVICE_GRAVITY, 0, 1, floatArrayOf(1.0f, 7.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_NEGATIVE_Z_AXIS_DIRECTION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_inclination_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_type_device_accelerometer() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_INCLINATION, hasGravitySensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_ROTATION_VECTOR, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_INCLINATION]).isNull()
        assertThat(dSensorData.data[TYPE_ROTATION_VECTOR]).isNotNull()
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_INCLINATION, hasGravitySensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_GYROSCOPE, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_INCLINATION]).isNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_inclination_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_INCLINATION, hasGravitySensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_INCLINATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_device_rotation_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_type_device_accelerometer() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_ROTATION, hasGravitySensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_ROTATION_VECTOR, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_ROTATION]).isNull()
        assertThat(dSensorData.data[TYPE_ROTATION_VECTOR]).isNotNull()
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_ROTATION, hasGravitySensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_GYROSCOPE, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_ROTATION]).isNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_device_rotation_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_DEVICE_ROTATION, hasGravitySensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_DEVICE_ROTATION]).isNotNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_0_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_pitch_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_not_been_called_with_type_device_accelerometer() {
        var dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_PITCH, hasGravitySensor = false)
        var dSensorData = dSensorEventProcessorImp.getSensorData()
        var dSensorEvent = DSensorEvent(TYPE_ROTATION_VECTOR, 0, 1, floatArrayOf(0.0f, 9.88766f, -47.7452f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_PITCH]).isNull()
        assertThat(dSensorData.data[TYPE_ROTATION_VECTOR]).isNotNull()
        dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_PITCH, hasGravitySensor = false)
        dSensorData = dSensorEventProcessorImp.getSensorData()
        dSensorEvent = DSensorEvent(TYPE_GYROSCOPE, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_PITCH]).isNull()
    }

    @Test
    fun testOnDSensorChanged_DSensorData_sensorData_size_is_1_when_param_dsensorTypes_of_DSensorEventProcessorImp_is_of_type_roll_and_device_has_no_gravity_sensor_and_onDSensorChanged_has_been_called_with_type_device_accelerometer() {
        val dSensorEventProcessorImp = DSensorEventProcessorImp(TYPE_ROLL, hasGravitySensor = false)
        val dSensorData = dSensorEventProcessorImp.getSensorData()
        val dSensorEvent = DSensorEvent(TYPE_DEVICE_ACCELEROMETER, 0, 1, floatArrayOf(1.0f, 2.77631f, 0.812349f))
        dSensorEventProcessorImp.onDSensorChanged(dSensorEvent)
        assertThat(dSensorData.data.size()).isEqualTo(1)
        assertThat(dSensorData.data[TYPE_ROLL]).isNotNull()
    }
}
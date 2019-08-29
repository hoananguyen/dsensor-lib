package com.hoan.dsensor

import com.google.common.truth.Truth.assertThat
import com.hoan.dsensor.interfaces.DSensorEventListener
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class DSensorEventProcessorImpTest {
    private lateinit var dSensorEventListener: DSensorEventListener

    @Before
    fun setUp() {
        dSensorEventListener = mock(DSensorEventListener::class.java)
    }

    @Test
    fun `getRegisteredDirectionList through init with dSensorTypes containing directions return correct list size and values`() {
        val directionTypeList: List<Int> = getDirectionTypes()
        var directionType = directionTypeList.random()

        for (i in 0..8) {
            // One direction
            var dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(directionType, dSensorEventListener)
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.size == 1).isTrue()
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.find { it.getDirectionType() == dSensorEventProcessorImpForTest.mRegisteredDSensorTypes }).isNotNull()


            // 2 directions
            directionType = directionTypeList.random()
            val dropList = directionTypeList.filterNot { it == directionType }
            val directionType2 = dropList.random()
            dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(directionType or directionType2, dSensorEventListener)
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.size == 2).isTrue()
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.find { it.getDirectionType() == directionType }).isNotNull()
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.find { it.getDirectionType() == directionType2 }).isNotNull()

            // all directions
            dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_ALL, dSensorEventListener)
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.size == directionTypeList.size).isTrue()
            for (dirType in directionTypeList) {
                assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList.find { it.getDirectionType() == dirType }).isNotNull()
            }
        }
    }

    @Test
    fun `getRegisteredDirectionList through init with dSensorTypes not containing direction return null`() {
        val directionTypeList: List<Int> = getDirectionTypes()
        var dSensorTypes = TYPE_ALL
        for (dirType in directionTypeList) {
            dSensorTypes = dSensorTypes and dirType.inv()
        }

        for (dirType in directionTypeList) {
            assertThat(dSensorTypes and dirType).isEqualTo(0)
        }
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(dSensorTypes, dSensorEventListener)
        try {
            assertThat(dSensorEventProcessorImpForTest.mRegisteredDirectionList).isEmpty()
        } catch (e: UninitializedPropertyAccessException) {
            println("mRegisteredDirectionList is uninitialized which is correct")
        }
    }

    @Test
    fun `getRegisteredWorldCoordinatesDSensorList through init return correct list when dSensorTypes contains world coordinates sensors`() {
        val worldCoordinatesDSensorList = getWorldCoordinatesTypes()
        var worldCoordinatesSensor = worldCoordinatesDSensorList.random()
        var dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(worldCoordinatesSensor, dSensorEventListener)

        // One world coordinates
        assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList.size == 1).isTrue()
        assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList[0].second ==
                getRawDSensor(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList[0].first))

        // two world coordinates
        worldCoordinatesSensor = worldCoordinatesDSensorList.random()
        var worldCoordinatesSensor2 = worldCoordinatesDSensorList.filterNot { it == worldCoordinatesSensor }.random()
        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(worldCoordinatesSensor or worldCoordinatesSensor2, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList.toSet().size == 2).isTrue()

        for (worldCoordSensor in dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList) {
            assertThat(worldCoordSensor.second == getRawDSensor(worldCoordSensor.first)).isTrue()
        }

        // 3 world coordinates
        worldCoordinatesSensor = worldCoordinatesDSensorList.random()
        worldCoordinatesSensor2 = worldCoordinatesDSensorList.filterNot { it == worldCoordinatesSensor }.random()
        val worldCoordinatesSensor3 = worldCoordinatesDSensorList.filterNot { it == worldCoordinatesSensor || it == worldCoordinatesSensor2 }.random()
        dSensorEventProcessorImpForTest =
            DSensorEventProcessorImpForTest(worldCoordinatesSensor or worldCoordinatesSensor2 or worldCoordinatesSensor3, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList.toSet().size == 3).isTrue()
        for (worldCoordSensor in dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList) {
            assertThat(worldCoordSensor.second == getRawDSensor(worldCoordSensor.first)).isTrue()
        }

        // 4 worldCoordinates
        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_ALL, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList.toSet().size == 4).isTrue()
        for (worldCoordSensor in dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList) {
            assertThat(worldCoordSensor.second == getRawDSensor(worldCoordSensor.first)).isTrue()
        }

    }

    @Test
    fun `getRegisteredWorldCoordinatesDSensorList through init with dSensorTypes not containing world coordinates return null`() {
        val worldCoordinatesDSensorList = getWorldCoordinatesTypes()
        var dSensorTypes = TYPE_ALL
        for (worldCoordType in worldCoordinatesDSensorList) {
            dSensorTypes = dSensorTypes and worldCoordType.inv()
        }
        for (worldCoordType in worldCoordinatesDSensorList) {
            assertThat(dSensorTypes and worldCoordType).isEqualTo(0)
        }
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(dSensorTypes, dSensorEventListener)
        try {
            assertThat(dSensorEventProcessorImpForTest.mRegisteredWorldCoordinatesList).isNull()
        } catch (e: UninitializedPropertyAccessException) {
            println("mRegisteredWorldCoordinatesList is uninitialized which is correct")
        }
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of false when hasGravitySensor and hasLinearAccelerationSensor are true`() {
        var dSensorEventProcessorImpForTest =
            DSensorEventProcessorImpForTest(TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_X_AXIS_DIRECTION, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_WORLD_LINEAR_ACCELERATION, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_NEGATIVE_Y_AXIS_DIRECTION, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_ALL, dSensorEventListener)
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of false when hasGravitySensor is true and hasLinearAccelerationSensor is false but dSensorTypes contain neither TYPE_DEVICE_LINEAR_ACCELERATION nor TYPE_WORLD_LINEAR_ACCELERATION`() {
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_X_AXIS_DIRECTION, dSensorEventListener, hasLinearAccelerationSensor = false)

        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of false, true when hasGravitySensor is true and hasLinearAccelerationSensor is false and dSensorTypes contain either TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_WORLD_LINEAR_ACCELERATION`() {
        var dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_WORLD_LINEAR_ACCELERATION, dSensorEventListener, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isTrue()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_DEVICE_LINEAR_ACCELERATION, dSensorEventListener, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isTrue()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_WORLD_LINEAR_ACCELERATION or TYPE_DEVICE_LINEAR_ACCELERATION,
            dSensorEventListener, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isTrue()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of false when hasGravitySensor is false and hasLinearAccelerationSensor is true and dSensorTypes contains no sensor requiring calculation of gravity`() {
        val dSensorTypes = TYPE_DEVICE_ACCELEROMETER or TYPE_DEVICE_LINEAR_ACCELERATION or TYPE_DEVICE_MAGNETIC_FIELD or TYPE_GYROSCOPE or
                TYPE_ROTATION_VECTOR or TYPE_DEPRECATED_ORIENTATION
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(dSensorTypes, dSensorEventListener, hasGravitySensor = false)

        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
    }

    @Test
    fun `getRequiredCalculationDSensor return pair of true, false when hasGravitySensor is false and hasLinearAccelerationSensor is true and dSensorTypes contains sensor requiring calculation of gravity`() {
        var dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_WORLD_LINEAR_ACCELERATION, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_WORLD_MAGNETIC_FIELD, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_X_AXIS_DIRECTION, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_DEVICE_GRAVITY, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_INCLINATION, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_DEVICE_ROTATION, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_ROLL, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()

        dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_PITCH, dSensorEventListener, hasGravitySensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of false when hasGravitySensor is false and hasLinearAccelerationSensor is false and dSensorTypes contains no sensor requiring calculation of gravity or linear acceleration`() {
        val dSensorTypes = TYPE_DEVICE_ACCELEROMETER or TYPE_DEVICE_MAGNETIC_FIELD or TYPE_DEPRECATED_ORIENTATION or TYPE_GYROSCOPE or TYPE_ROTATION_VECTOR
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(dSensorTypes, dSensorEventListener,
            hasGravitySensor = false, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isFalse()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of true when hasGravitySensor is false and hasLinearAccelerationSensor is false and dSensorTypes contains sensor requiring calculation of linear acceleration but not gravity`() {
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_DEVICE_LINEAR_ACCELERATION, dSensorEventListener,
            hasGravitySensor = false, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isTrue()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()
    }

    @Test
    fun `getRequiredCalculationDSensor through init return pair of true, false when hasGravitySensor is false and hasLinearAccelerationSensor is false and dSensorTypes contains sensor requiring calculation gravity but no linear acceleration`() {
        val dSensorEventProcessorImpForTest = DSensorEventProcessorImpForTest(TYPE_PITCH, dSensorEventListener,
            hasGravitySensor = false, hasLinearAccelerationSensor = false)
        assertThat(dSensorEventProcessorImpForTest.mCalculateLinearAcceleration).isFalse()
        assertThat(dSensorEventProcessorImpForTest.mCalculateGravity).isTrue()
    }
}
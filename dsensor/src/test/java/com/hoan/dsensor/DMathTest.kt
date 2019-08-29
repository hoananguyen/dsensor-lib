package com.hoan.dsensor

import com.google.common.truth.Truth.assertThat
import com.hoan.dsensor.utils.addAngle
import com.hoan.dsensor.utils.averageAngle
import org.junit.Test
import kotlin.math.PI

class DMathTest {

    @Test
    fun `average angle of 359 degree and 1 degree is 0`() {
        val currentSum = FloatArray(2)
        addAngle((PI / 180.0).toFloat(), currentSum) // 1 degree
        addAngle((359 * PI / 180.0).toFloat(), currentSum) // 359 degree
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 0f < 0.000001).isTrue()
    }

    @Test
    fun `average angle of -3 degree and 1 degree is 359`() {
        val currentSum = FloatArray(2)
        addAngle((-3 * PI / 180.0).toFloat(), currentSum) // 1 degree
        addAngle((PI / 180.0).toFloat(), currentSum) // 359 degree
        println("average minus angle = ${averageAngle(currentSum, 2)}")
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 359 < 0.000001).isTrue()
    }

    @Test
    fun `average angle of 258 degree and 0 degree is 359`() {
        val currentSum = FloatArray(2)
        addAngle((358 * PI / 180).toFloat(), currentSum)
        addAngle(0f, currentSum)
        println("average angle = ${averageAngle(currentSum, 2)}")
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 359 < 0.000001).isTrue()
    }

    @Test
    fun `average angle of 0 degree and 6 degree is 3`() {
        val currentSum = FloatArray(2)
        addAngle(0f, currentSum)
        addAngle((6 * PI / 180).toFloat(), currentSum)
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 3 < 0.000001).isTrue()
    }

    @Test
    fun `average angle of 90 degree and 94 degree is 92 degree`() {
        val currentSum = FloatArray(2)
        addAngle((PI / 2).toFloat(), currentSum)
        addAngle((94 * PI/ 180).toFloat(), currentSum)
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 92 < 0.000001).isTrue()
    }

    @Test
    fun `average angle of 16 degree and 16 degree is 16`() {
        val currentSum = FloatArray(2)
        addAngle((16 * PI / 180).toFloat(), currentSum)
        addAngle((16 * PI / 180).toFloat(), currentSum)
        assertThat(Math.toDegrees(averageAngle(currentSum, 2).toDouble()) - 16 < 0.000001).isTrue()
    }
}
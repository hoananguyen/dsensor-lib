package com.hoan.dsensor.utils

import kotlin.math.*

internal const val TWENTY_FIVE_DEGREE_IN_RADIAN = 0.436332313f

internal const val ONE_FIFTY_FIVE_DEGREE_IN_RADIAN = 2.7052603f


/**
 * Math utility methods
 */

fun addAngle(angle: Float, currentSum: FloatArray) {
    currentSum[0] += sin(angle)
    currentSum[1] += cos(angle)
}

fun averageAngle(currentSum: FloatArray, totalTerms: Int): Float {
    return atan2(currentSum[0] / totalTerms, currentSum[1] / totalTerms)
}

fun calculateNorm(vector: FloatArray): Float {
    var norm = 0f
    for (coordinate in vector) {
        norm += coordinate.pow(2)
    }
    return sqrt(norm)
}

fun convertToDegree(valueInRadian: Float): Int {
    var convertValue =  Math.toDegrees(valueInRadian.toDouble()).roundToInt()
    if (convertValue < 0) {
        convertValue = (convertValue + 360) % 360
    }

    return convertValue
}

fun productOfSquareMatrixAndVector(matrix: FloatArray, vector: FloatArray): FloatArray? {
    if (matrix.size != vector.size * vector.size) {
        return null
    }
    val numberOfColumns = vector.size
    val result = FloatArray(numberOfColumns)
    var k = 0
    for (i in 0 until numberOfColumns) {
        result[i] = 0f
        for (j in 0 until numberOfColumns) {
            result[i] += matrix[k++] * vector[j]
        }
    }
    return result
}

fun removeAngle(angle: Float, currentSum: FloatArray) {
    currentSum[0] -= sin(angle)
    currentSum[1] -= cos(angle)
}
package com.hoan.dsensor.utils

import kotlin.math.*


/**
 * Math utility methods
 */

fun calculateNorm(vector: FloatArray): Float {
    var norm = 0.toFloat()
    for (coordinate in vector) {
        norm += coordinate.pow(2)
    }
    return sqrt(norm)
}

fun addAngle(angle: Float, currentSum: FloatArray) {
    currentSum[0] += sin(angle.toDouble()).toFloat()
    currentSum[1] += cos(angle.toDouble()).toFloat()
}

fun removeAngle(angle: Float, currentSum: FloatArray) {
    currentSum[0] -= sin(angle.toDouble()).toFloat()
    currentSum[1] -= cos(angle.toDouble()).toFloat()
}

fun averageAngle(currentSum: FloatArray, totalTerms: Int): Float {
    return atan2((currentSum[0] / totalTerms).toDouble(), (currentSum[1] / totalTerms).toDouble()).toFloat()
}

fun scaleVector(vector: FloatArray, scaleFactor: Float): FloatArray {
    //logger("DMathKt", "scaleVector(" + vector[0] + ", " + vector[1] + ", " + vector[2] + ", " + scaleFactor + ")")
    val result = FloatArray(vector.size)
    for (i in vector.indices) {
        result[i] = scaleFactor * vector[i]
    }
    return result
}

fun productOfSquareMatrixAndVector(matrix: FloatArray?, vector: FloatArray?): FloatArray? {
    //logger("DMathKt", "productOfSquareMatrixAndVector: vector = ("
    //        + vector[0] + ", " + vector[1] + ", " + vector[2] + ")");
    if (matrix == null || vector == null || matrix.size != vector.size * vector.size) {
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
package com.example.colorsensor

import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

class PlanCPoultryMeatStatus {

    companion object {
        private val CLASS_A = intArrayOf(185, 170, 176)
        private val CLASS_B = intArrayOf(163, 164, 172)
        private val CLASS_C = intArrayOf(165, 165, 173)
        private val CLASS_D = intArrayOf(163, 163, 171)

        enum class MeatStatus {
            CLASS_A, CLASS_B, CLASS_C, CLASS_D
        }

        fun getMeatStatus(meatType: String, rgbValues: Triple<Int, Int, Int>?): Triple<String, IntArray, FloatArray> {
            if (rgbValues == null) {
                throw IllegalArgumentException("RGB values cannot be null.")
            }

            val intRgbValues = intArrayOf(rgbValues.first, rgbValues.second, rgbValues.third)

            // Log the intRgbValues
            Log.d("RGBValues", "RGB Values: ${intRgbValues.joinToString(", ")}")

            val meatStatusTriple = getMeatStatus(intRgbValues)

            val meatStatusString = when (meatStatusTriple.first) {
                MeatStatus.CLASS_A -> "Fresh"
                MeatStatus.CLASS_B -> "Moderately Fresh"
                MeatStatus.CLASS_C -> "Moderately Fresh"
                MeatStatus.CLASS_D -> "Borderline Spoilage"
            }

            val meatStatus = when (meatStatusString) {
                "Fresh" -> MeatStatus.CLASS_A
                "Moderately Fresh" -> MeatStatus.CLASS_B
                "Moderately Fresh" -> MeatStatus.CLASS_C
                "Borderline Spoilage" -> MeatStatus.CLASS_D
                else -> {
                    Log.e("MeatStatus", "Invalid meat status string: $meatStatusString")
                    throw IllegalStateException("Invalid meat status string.")

                }
        }

            return Triple(meatStatusString, intRgbValues, meatStatusTriple.third)
        }

        private fun getMeatStatus(rgbValues: IntArray): Triple<MeatStatus, IntArray, FloatArray> {
            val distanceToClassA = calculateDistance(rgbValues, CLASS_A)
            val distanceToClassB = calculateDistance(rgbValues, CLASS_B)
            val distanceToClassC = calculateDistance(rgbValues, CLASS_C)
            val distanceToClassD = calculateDistance(rgbValues, CLASS_D)

            val minDistance = minOf(distanceToClassA, distanceToClassB, distanceToClassC, distanceToClassD)

            val meatStatus = when (minDistance) {
                distanceToClassA -> MeatStatus.CLASS_A
                distanceToClassB -> MeatStatus.CLASS_B
                distanceToClassC -> MeatStatus.CLASS_C
                distanceToClassD -> MeatStatus.CLASS_D
                else -> throw IllegalStateException("Unable to determine meat status.")
            }

            return Triple(meatStatus, rgbValues, floatArrayOf(minDistance))
        }

        private fun calculateDistance(rgbValues1: IntArray, rgbValues2: IntArray): Float {
            return sqrt(
                (rgbValues1[0] - rgbValues2[0]).toDouble().pow(2.0) +
                        (rgbValues1[1] - rgbValues2[1]).toDouble().pow(2.0) +
                        (rgbValues1[2] - rgbValues2[2]).toDouble().pow(2.0)
            ).toFloat()
        }
    }
}

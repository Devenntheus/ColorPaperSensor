package com.example.colorsensor

import kotlin.math.pow
import kotlin.math.sqrt

class MeatStatus {
    companion object {
        private val FRESH_HSV = floatArrayOf(358.0f, 48.0f, 51.0f)
        private val CHILLED_HSV = floatArrayOf(4.0f, 92.0f, 31.0f)
        private val FROZEN_HSV = floatArrayOf(2.0f, 76.0f, 47.0f)
        private val SPOILED_HSV = floatArrayOf(108.0f, 44.0f, 27.0f)
        private val EXPIRED_HSV = floatArrayOf(26.0f, 15.0f, 69.0f)

        fun getMeatStatus(meatType: String, hsvValues: FloatArray): String {
            val predefinedHSVValues = getPredefinedHSVValues(meatType)

            // Find the closest predefined HSV values
            val closestHSVValues = findClosestHSVValues(hsvValues, predefinedHSVValues)

            return when (closestHSVValues) {
                FRESH_HSV -> "Fresh"
                CHILLED_HSV -> "Chilled"
                FROZEN_HSV -> "Frozen"
                SPOILED_HSV -> "Spoiled"
                EXPIRED_HSV -> "Expired"
                else -> "Unknown"
            }
        }

        private fun findClosestHSVValues(targetHSV: FloatArray, predefinedHSVValues: List<FloatArray>): FloatArray {
            var closestHSV = predefinedHSVValues.firstOrNull()
            var minColorDifference = getHSVColorDifference(targetHSV, closestHSV ?: floatArrayOf())

            for (hsvValues in predefinedHSVValues) {
                val colorDifference = getHSVColorDifference(targetHSV, hsvValues)
                if (colorDifference < minColorDifference) {
                    minColorDifference = colorDifference
                    closestHSV = hsvValues
                }
            }

            return closestHSV ?: floatArrayOf()
        }

        private fun getHSVColorDifference(hsv1: FloatArray, hsv2: FloatArray): Double {
            val h1 = hsv1[0]
            val s1 = hsv1[1]
            val v1 = hsv1[2]

            val h2 = hsv2[0]
            val s2 = hsv2[1]
            val v2 = hsv2[2]

            return sqrt((h2 - h1).toDouble().pow(2) + (s2 - s1).toDouble().pow(2) + (v2 - v1).toDouble().pow(2))
        }

        private fun getPredefinedHSVValues(meatType: String): List<FloatArray> {
            return when (meatType) {
                "Pork" -> listOf(FRESH_HSV, CHILLED_HSV, FROZEN_HSV, SPOILED_HSV, EXPIRED_HSV)
                "Beef" -> listOf(FRESH_HSV, CHILLED_HSV, FROZEN_HSV, SPOILED_HSV, EXPIRED_HSV)
                "Mutton" -> listOf(FRESH_HSV, CHILLED_HSV, FROZEN_HSV, SPOILED_HSV, EXPIRED_HSV)
                "Poultry" -> listOf(FRESH_HSV, CHILLED_HSV, FROZEN_HSV, SPOILED_HSV, EXPIRED_HSV)
                else -> emptyList()
            }
        }
    }
}
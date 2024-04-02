package com.example.colorsensor

import android.content.ContentValues
import android.util.Log
import kotlin.math.pow
import kotlin.math.sqrt

class PlanHPoultryMeatStatus {

    data class LabValue(val L: Float, val a: Float, val b: Float)

    companion object {
        // Average Class LAB values with ranges for accuracy
        private val CLASS_A = LabValue(71.12f, 6.59f, -1.26f)
        private val CLASS_B = LabValue(68.01f, 1.41f, -4.25f)
        private val CLASS_C = LabValue(67.23f, 1.58f, -4.22f)

        // Convert RGB to XYZ color space.
        private fun rgbToXyz(r: Float, g: Float, b: Float): FloatArray {
            // Normalize RGB values
            val varR = r / 255f
            val varG = g / 255f
            val varB = b / 255f

            // Linearize sRGB values
            val finalR = if (varR > 0.04045f) ((varR + 0.055f) / 1.055f).pow(2.4f) else varR / 12.92f
            val finalG = if (varG > 0.04045f) ((varG + 0.055f) / 1.055f).pow(2.4f) else varG / 12.92f
            val finalB = if (varB > 0.04045f) ((varB + 0.055f) / 1.055f).pow(2.4f) else varB / 12.92f

            // Convert sRGB to XYZ
            val X = finalR * 0.4124564f + finalG * 0.3575761f + finalB * 0.1804375f
            val Y = finalR * 0.2126729f + finalG * 0.7151522f + finalB * 0.0721750f
            val Z = finalR * 0.0193339f + finalG * 0.1191920f + finalB * 0.9503041f

            return floatArrayOf(X * 100f, Y * 100f, Z * 100f)
        }

        // Convert XYZ to LAB color space.
        private fun xyzToLab(x: Float, y: Float, z: Float): FloatArray {
            val varX = x / 95.047f
            val varY = y / 100f
            val varZ = z / 108.883f

            val finalX = if (varX > 0.008856f) varX.pow(1 / 3f) else (7.787f * varX) + (16 / 116f)
            val finalY = if (varY > 0.008856f) varY.pow(1 / 3f) else (7.787f * varY) + (16 / 116f)
            val finalZ = if (varZ > 0.008856f) varZ.pow(1 / 3f) else (7.787f * varZ) + (16 / 116f)

            val L = (116f * finalY) - 16
            val a = 500f * (finalX - finalY)
            val b = 200f * (finalY - finalZ)

            return floatArrayOf(L, a, b)
        }

        // Corrected deltaE function to calculate Euclidean distance between two LAB color values
        private fun deltaE(lab1: LabValue, lab2: LabValue): Float {
            val dL = lab1.L - lab2.L
            val da = lab1.a - lab2.a
            val db = lab1.b - lab2.b
            return sqrt((dL * dL) + (da * da) + (db * db))
        }

        // Get meat status from LAB values.
        private fun getMeatStatusFromLAB(labValues: LabValue): String {
            val classADistance = deltaE(labValues, CLASS_A)
            val classBDistance = deltaE(labValues, CLASS_B)
            val classCDistance = deltaE(labValues, CLASS_C)

            // Log message before saving meat information
            Log.d(ContentValues.TAG, "Class A Distance: $classADistance")
            Log.d(ContentValues.TAG, "Class B Distance: $classBDistance")
            Log.d(ContentValues.TAG, "Class C Distance: $classCDistance")

            val minDistance = minOf(classADistance, classBDistance, classCDistance)

            // Define your threshold here
            val threshold = 6.5f // Adjust as needed

            return when {
                minDistance <= threshold -> {
                    when {
                        minDistance == classADistance -> "Fresh"
                        minDistance == classBDistance -> "Moderately Fresh"
                        minDistance == classCDistance -> "Borderline Spoilage"
                        else -> "Unknown"
                    }
                }
                else -> "Unknown"
            }
        }

        // Get meat status from RGB values.
        fun getMeatStatus(meatType: String, rgbValues: Triple<Int, Int, Int>?): Triple<String, FloatArray, FloatArray> {
            if (rgbValues == null) {
                throw IllegalArgumentException("RGB values cannot be null.")
            }

            // Convert RGB to XYZ
            val xyzValues = rgbToXyz(rgbValues.first.toFloat(), rgbValues.second.toFloat(), rgbValues.third.toFloat())

            // Convert XYZ to LAB
            val labValues = xyzToLab(xyzValues[0], xyzValues[1], xyzValues[2])

            /*// Create a LabValue instance
            val labValue = LabValue(labValues[0], labValues[1], labValues[2])*/

            // Create a LabValue instance
            val labValue = LabValue(68.19f, 1.04f, -4.34f)

            // Get meat status from LAB values
            val meatStatus = getMeatStatusFromLAB(labValue)

            return Triple(meatStatus, labValues, xyzValues)
        }
    }
}

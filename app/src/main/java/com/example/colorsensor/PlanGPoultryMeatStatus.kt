package com.example.colorsensor

import android.content.ContentValues
import android.util.Log
import kotlin.math.pow

class PlanGPoultryMeatStatus {

    data class DecimalRange(val from: Long, val to: Long)

    companion object {
        // Decimal range with ranges for accuracy
        private val CLASS_A = DecimalRange(
            12234676,
            12167086
        )

        private val CLASS_B = DecimalRange(
            10921903,
            10790574
        )

        private val CLASS_C = DecimalRange(
            10790317,
            10658218
        )



    // Get meat status from Decimal value.
        private fun getMeatStatusFromHexCode(hexCodeDecimal: Long): String {
            return when {
                hexCodeDecimal <= CLASS_A.from && hexCodeDecimal >= CLASS_A.to -> "Fresh"
                hexCodeDecimal <= CLASS_B.from && hexCodeDecimal >= CLASS_B.to -> "Moderately Fresh"
                hexCodeDecimal <= CLASS_C.from && hexCodeDecimal >= CLASS_C.to -> "Borderline Spoilage"
                else -> "Unknown"
            }
        }

        // Get meat status from RGB values.
        fun getMeatStatus(meatType: String, rgbValues: Triple<Int, Int, Int>?, hexColor: String): Triple<String, FloatArray, FloatArray> {
            if (rgbValues == null) {
                throw IllegalArgumentException("RGB values cannot be null.")
            }

            if (hexColor == null) {
                throw IllegalArgumentException("Hex color cannot be null.")
            }

            val hexWithoutHash = if (hexColor.startsWith("#")) {
                hexColor.substring(1) // Remove the '#' symbol
            } else {
                hexColor
            }

            // Convert hexColor to decimal number with 8 digits
            val hexCodeDecimal = try {
                hexWithoutHash.toLong(16)
            } catch (e: NumberFormatException) {
                throw IllegalArgumentException("Invalid hexadecimal string: $hexColor")
            }

            // Ensure hexCodeDecimal is an 8-digit decimal number
            val formattedDecimal = String.format("%08d", hexCodeDecimal.toLong())

            // Log message before saving meat information
            Log.d(ContentValues.TAG, "Decimal Value: $formattedDecimal")


            // Convert RGB to XYZ
            val xyzValues = rgbToXyz(rgbValues.first.toFloat(), rgbValues.second.toFloat(), rgbValues.third.toFloat())

            // Convert XYZ to LAB
            val labValues = xyzToLab(xyzValues[0], xyzValues[1], xyzValues[2])

            // Get meat status from hex color
            val meatStatus = getMeatStatusFromHexCode(formattedDecimal.toLong()) // Passing the float value for consistency

            return Triple(meatStatus, labValues, xyzValues)
        }

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

    }
}

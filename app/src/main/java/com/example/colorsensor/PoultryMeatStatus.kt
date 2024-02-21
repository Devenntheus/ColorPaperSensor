package com.example.colorsensor

import kotlin.math.*

class PoultryMeatStatus {
    companion object {
        private val REF = floatArrayOf(73.6f, 9.59f, 3.23f)
        private val ONE_HOUR = floatArrayOf(78.36f, 2.8f, 1.03f)
        private val TWO_HOUR = floatArrayOf(75.57f, 6.76f, 2.23f)
        private val THREE_HOUR = floatArrayOf(74.91f, 7.44f, 2.4f)
        private val FOUR_HOUR = floatArrayOf(76.57f, 5.38f, 1.75f)
        private val FIVE_HOUR = floatArrayOf(77.28f, 4.68f, 1.64f)
        private val SIX_HOUR = floatArrayOf(76.6f, 4.85f, 1.52f)
        private val SEVEN_HOUR = floatArrayOf(77.43f, -0.22f, -1.17f)

        // Convert HSV to RGB
        private fun hsvToRgb(h: Float, s: Float, v: Float): FloatArray {
            val c = v * s
            val x = c * (1 - kotlin.math.abs((h / 60) % 2 - 1))
            val m = v - c

            return when {
                h < 60 -> floatArrayOf(c + m, x + m, m)
                h < 120 -> floatArrayOf(x + m, c + m, m)
                h < 180 -> floatArrayOf(m, c + m, x + m)
                h < 240 -> floatArrayOf(m, x + m, c + m)
                h < 300 -> floatArrayOf(x + m, m, c + m)
                else -> floatArrayOf(c + m, m, x + m)
            }
        }

        // Convert RGB to XYZ
        private fun rgbToXyz(r: Float, g: Float, b: Float): FloatArray {
            // Normalize RGB values
            val varR = r / 255
            val varG = g / 255
            val varB = b / 255

            // Linearize sRGB values
            val finalR = if (varR > 0.04045) ((varR + 0.055) / 1.055).pow(2.4) else varR / 12.92
            val finalG = if (varG > 0.04045) ((varG + 0.055) / 1.055).pow(2.4) else varG / 12.92
            val finalB = if (varB > 0.04045) ((varB + 0.055) / 1.055).pow(2.4) else varB / 12.92

            // Convert sRGB to XYZ
            val X = finalR * 0.4124564f + finalG * 0.3575761f + finalB * 0.1804375f
            val Y = finalR * 0.2126729f + finalG * 0.7151522f + finalB * 0.0721750f
            val Z = finalR * 0.0193339f + finalG * 0.1191920f + finalB * 0.9503041f

            return floatArrayOf((X * 100).toFloat(), (Y * 100).toFloat(), (Z * 100).toFloat())
        }

        // Convert XYZ to LAB
        private fun xyzToLab(x: Float, y: Float, z: Float): FloatArray {
            val varX = x / 95.047
            val varY = y / 100.0
            val varZ = z / 108.883

            val finalX = if (varX > 0.008856) varX.pow(1 / 3.0) else (7.787 * varX) + (16 / 116.0)
            val finalY = if (varY > 0.008856) varY.pow(1 / 3.0) else (7.787 * varY) + (16 / 116.0)
            val finalZ = if (varZ > 0.008856) varZ.pow(1 / 3.0) else (7.787 * varZ) + (16 / 116.0)

            val L = (116 * finalY) - 16
            val a = 500 * (finalX - finalY)
            val b = 200 * (finalY - finalZ)

            return floatArrayOf(L.toFloat(), a.toFloat(), b.toFloat())
        }

        // Get meat status based on LAB values
        private fun getMeatStatusFromLab(labValues: FloatArray): String {
            val distances = mapOf(
                "Fresh" to deltaE(labValues, REF),
                "Fresh 1Hr" to deltaE(labValues, ONE_HOUR),
                "Fresh 2Hrs" to deltaE(labValues, TWO_HOUR),
                "Fresh 3Hrs" to deltaE(labValues, THREE_HOUR),
                "Fresh 4Hrs" to deltaE(labValues, FOUR_HOUR),
                "Fresh 5Hrs" to deltaE(labValues, FIVE_HOUR),
                "Fresh 6Hrs" to deltaE(labValues, SIX_HOUR),
                "Not Fresh" to deltaE(labValues, SEVEN_HOUR)
            )

            val minDistanceEntry = distances.minByOrNull { it.value }
            return minDistanceEntry?.key ?: "Unknown"
        }

        // Convert HSV to LAB and get meat status
        fun getMeatStatus(meatType: String, hsvValues: FloatArray): String {
            // Convert HSV to RGB
            val rgbValues = hsvToRgb(hsvValues[0], hsvValues[1], hsvValues[2])

            // Convert RGB to XYZ
            val xyzValues = rgbToXyz(rgbValues[0], rgbValues[1], rgbValues[2])

            // Convert XYZ to LAB
            val labValues = xyzToLab(xyzValues[0], xyzValues[1], xyzValues[2])

            // Get meat status from LAB values
            return getMeatStatusFromLab(labValues)
        }

        // Calculate the Euclidean distance between two LAB colors
        private fun deltaE(lab1: FloatArray, lab2: FloatArray): Float {
            val dL = lab1[0] - lab2[0]
            val da = lab1[1] - lab2[1]
            val db = lab1[2] - lab2[2]

            return sqrt(dL.pow(2) + da.pow(2) + db.pow(2))
        }
    }
}
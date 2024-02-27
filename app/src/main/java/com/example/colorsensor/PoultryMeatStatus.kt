package com.example.colorsensor

import kotlin.math.*

class PoultryMeatStatus {
    companion object {
        private val CLASS_A_T1 = floatArrayOf(72.39f, 5.14f, -1.41f)
        private val CLASS_A_T2 = floatArrayOf(70.69f, 6.92f, -1.3f)
        private val CLASS_A_T3 = floatArrayOf(70.28f, 7.71f, -1.07f)
        private val CLASS_B_T1 = floatArrayOf(68.19f, 1.04f, -4.34f)
        private val CLASS_B_T2 = floatArrayOf(66.7f, 1.5f, -4.49f)
        private val CLASS_B_T3 = floatArrayOf(68.38f, 1.36f, -4.15f)
        private val CLASS_C_T1 = floatArrayOf(68.76f, 1.21f, -4.25f)
        private val CLASS_C_T2 = floatArrayOf(68.26f, 1.49f, -4.19f)
        private val CLASS_C_T3 = floatArrayOf(67.78f, 1.86f, -4.09f)
        private val CLASS_D_T1 = floatArrayOf(67.93f, 1.29f, -4.13f)
        private val CLASS_D_T2 = floatArrayOf(67.13f, 1.48f, -4.21f)
        private val CLASS_D_T3 = floatArrayOf(66.63f, 1.98f, -4.33f)

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

        // Get meat status from LAB values.
        private fun getMeatStatusFromLab(labValues: FloatArray): String {
            val distances = mapOf(
                "A" to minOf(
                    deltaE(labValues, CLASS_A_T1),
                    deltaE(labValues, CLASS_A_T2),
                    deltaE(labValues, CLASS_A_T3)
                ),
                "B" to minOf(
                    deltaE(labValues, CLASS_B_T1),
                    deltaE(labValues, CLASS_B_T2),
                    deltaE(labValues, CLASS_B_T3)
                ),
                "C" to minOf(
                    deltaE(labValues, CLASS_C_T1),
                    deltaE(labValues, CLASS_C_T2),
                    deltaE(labValues, CLASS_C_T3)
                ),
                "D" to minOf(
                    deltaE(labValues, CLASS_D_T1),
                    deltaE(labValues, CLASS_D_T2),
                    deltaE(labValues, CLASS_D_T3)
                )
            )

            val closestClass = distances.minByOrNull { it.value }?.key ?: "Unknown"

            return if (closestClass in listOf("A", "B", "C")) "$closestClass-Fresh" else "$closestClass-Not Fresh"
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

            // Get meat status from LAB values
            val meatStatus = getMeatStatusFromLab(labValues)

            return Triple(meatStatus, labValues, xyzValues)
        }

        // Calculate the Euclidean distance between two LAB colors.
        private fun deltaE(lab1: FloatArray, lab2: FloatArray): Float {
            val dL = lab1[0] - lab2[0]
            val da = lab1[1] - lab2[1]
            val db = lab1[2] - lab2[2]
            return sqrt(dL * dL + da * da + db * db)
        }
    }
}
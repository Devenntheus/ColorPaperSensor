package com.example.colorsensor

import kotlin.math.pow
import kotlin.math.sqrt

class PlanBPoultryMeatStatus {

    companion object {
        private val CLASS_A = floatArrayOf(185.43f, 170.24f, 176.54f)
        private val CLASS_B = floatArrayOf(163.77f, 164.61f, 172.78f)
        private val CLASS_C = floatArrayOf(165.69f, 165.83f, 173.89f)
        private val CLASS_D = floatArrayOf(163f, 163.03f, 171.19f)

        data class Lab(val L: Float, val a: Float, val b: Float)

        data class Xyz(val X: Float, val Y: Float, val Z: Float)

        data class Rgb(val R: Int, val G: Int, val B: Int)

        enum class MeatStatus {
            CLASS_A, CLASS_B, CLASS_C, CLASS_D
        }

        fun Lab.toXyz(): Xyz {
            val fy = (L + 16) / 116.0
            val fx = a / 500.0 + fy
            val fz = fy - b / 200.0

            val epsilon = 0.008856
            val kappa = 903.3

            val xr = if (fx > epsilon) fx.pow(3) else (fx - 16 / 116.0) / 7.787
            val yr = if (fy > epsilon) ((L + 16) / 116.0).pow(3) else (fy - 16 / 116.0) / 7.787
            val zr = if (fz > epsilon) fz.pow(3) else (fz - 16 / 116.0) / 7.787

            val X = xr * 95.047
            val Y = yr * 100.0
            val Z = zr * 108.883

            return Xyz(X.toFloat(), Y.toFloat(), Z.toFloat())
        }

        fun Xyz.toRgb(): Rgb {
            val x = X / 100.0
            val y = Y / 100.0
            val z = Z / 100.0

            val r = (x * 3.2406 + y * -1.5372 + z * -0.4986) * 255.0
            val g = (x * -0.9689 + y * 1.8758 + z * 0.0415) * 255.0
            val b = (x * 0.0557 + y * -0.2040 + z * 1.0570) * 255.0

            return Rgb(r.toInt(), g.toInt(), b.toInt())
        }

        fun getMeatStatusFromLab(lab: Lab): MeatStatus {
            val xyz = lab.toXyz()

            // Define your thresholds for each class
            val thresholds = mapOf(
                MeatStatus.CLASS_A to CLASS_A,
                MeatStatus.CLASS_B to CLASS_B,
                MeatStatus.CLASS_C to CLASS_C,
                MeatStatus.CLASS_D to CLASS_D
            )

            // Calculate the Euclidean distance between the input LAB values and each threshold
            val distances = thresholds.mapValues { (_, threshold) ->
                calculateEuclideanDistance(lab, Lab(threshold[0], threshold[1], threshold[2]))
            }

            // Find the minimum distance and return the corresponding MeatStatus
            val minDistanceEntry = distances.minByOrNull { it.value }!!
            return minDistanceEntry.key
        }

        fun Xyz.toXyzFloatArray(): FloatArray {
            return floatArrayOf(X, Y, Z)
        }

        fun Rgb.toRgbFloatArray(): FloatArray {
            return floatArrayOf(R.toFloat(), G.toFloat(), B.toFloat())
        }
        fun getMeatStatus(meatType: String, rgbValues: Triple<Int, Int, Int>?): Triple<String, FloatArray, FloatArray> {
            if (rgbValues == null) {
                throw IllegalArgumentException("RGB values cannot be null.")
            }

            val xyz = Xyz(
                (rgbValues.first.toFloat() / 255.0 * 100.0).toFloat(),
                (rgbValues.second.toFloat() / 255.0 * 100.0).toFloat(),
                (rgbValues.third.toFloat() / 255.0 * 100.0).toFloat()
            )

            val lab = Lab(0f, 0f, 0f) // You might want to calculate LAB from XYZ, but for simplicity, using a placeholder

            // Define your thresholds for each class
            val thresholds = mapOf(
                MeatStatus.CLASS_A to CLASS_A,
                MeatStatus.CLASS_B to CLASS_B,
                MeatStatus.CLASS_C to CLASS_C,
                MeatStatus.CLASS_D to CLASS_D
            )

            // Calculate the Euclidean distance between the input RGB values and each threshold
            val distances = thresholds.mapValues { (_, threshold) ->
                calculateEuclideanDistance(
                    Rgb(rgbValues.first, rgbValues.second, rgbValues.third),
                    Rgb(threshold[0].toInt(), threshold[1].toInt(), threshold[2].toInt())
                )
            }

            // Find the minimum distance and return the corresponding MeatStatus
            val minDistanceEntry = distances.minByOrNull { it.value }!!

            // Assign string representation based on MeatStatus
            val meatStatusString = when (minDistanceEntry.key) {
                MeatStatus.CLASS_A -> "Fresh"
                MeatStatus.CLASS_B, MeatStatus.CLASS_C -> "Moderately Fresh"
                MeatStatus.CLASS_D -> "Borderline Spoilage"
            }

            // Correct the conversion from xyz to rgb
            val rgbFromXyz = xyz.toRgb()

            return Triple(meatStatusString, rgbFromXyz.toRgbFloatArray(), xyz.toXyzFloatArray())
        }

        private fun calculateEuclideanDistance(lab1: Lab, lab2: Lab): Float {
            val dL = lab1.L - lab2.L
            val da = lab1.a - lab2.a
            val db = lab1.b - lab2.b
            return sqrt(dL.pow(2) + da.pow(2) + db.pow(2)).toFloat()
        }

        private fun calculateEuclideanDistance(rgb1: Rgb, rgb2: Rgb): Float {
            val dR = rgb1.R - rgb2.R
            val dG = rgb1.G - rgb2.G
            val dB = rgb1.B - rgb2.B
            return sqrt((dR * dR + dG * dG + dB * dB).toFloat())
        }
    }
}

import kotlin.math.*

class PlanAPoultryMeatStatus {
    companion object {
        // Average Class Lab values with ranges for accuracy
        private val CLASS_A = Pair(
            floatArrayOf(73.62f, 9.09f, 1.24f),
            floatArrayOf(69.5658f, 4.0f, -2.7558f)
        )

        private val CLASS_B = Pair(
            floatArrayOf(69.5657f, 3.9999f, -2.7559f),
            floatArrayOf(67.6208f, 1.4967f, -4.2375f)
        )

        private val CLASS_C = Pair(
            floatArrayOf(67.6207f, 1.4966f, -4.2376f),
            floatArrayOf(64.73f, -0.9167f, -6.7233f)
        )

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
                "Fresh" to deltaE(labValues, CLASS_A.first, CLASS_A.second),
                "Moderately Fresh" to deltaE(labValues, CLASS_B.first, CLASS_B.second),
                "Borderline Spoilage" to deltaE(labValues, CLASS_C.first, CLASS_C.second)
            )

            val closestClass = distances.minByOrNull { it.value }?.key ?: "Unknown"
            return closestClass
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
        private fun deltaE(lab1: FloatArray, lab2From: FloatArray, lab2To: FloatArray): Float {
            val dL = lab1[0] - (lab2From[0] + lab2To[0]) / 2
            val da = lab1[1] - (lab2From[1] + lab2To[1]) / 2
            val db = lab1[2] - (lab2From[2] + lab2To[2]) / 2
            return sqrt(dL * dL + da * da + db * db)
        }
    }
}
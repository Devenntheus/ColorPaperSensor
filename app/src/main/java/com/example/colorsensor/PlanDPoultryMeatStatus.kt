import kotlin.math.*

class PlanDPoultryMeatStatus {
    data class XYZRange(val from: FloatArray, val to: FloatArray)
    companion object {
        // Average Class XYZ values with ranges for accuracy
        private val CLASS_A = XYZRange(
            floatArrayOf(44.92f, 44.86f, 49.8f),
            floatArrayOf(39.4775f, 40.1775f, 46.205f)
        )

        private val CLASS_B = XYZRange(
            floatArrayOf(39.4774f, 40.1774f, 46.2049f),
            floatArrayOf(36.535f, 37.4675f, 44.495f)
        )

        private val CLASS_C = XYZRange(
            floatArrayOf(36.5349f, 37.4674f, 44.4949f),
            floatArrayOf(33.58f, 34.44f, 41.38f)
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

        // Get meat status from XYZ values.
        private fun getMeatStatusFromXYZ(xyzValues: FloatArray): String {
            val distances = mapOf(
                "Fresh" to deltaE(xyzValues, CLASS_A.from, CLASS_A.to),
                "Moderately Fresh" to deltaE(xyzValues, CLASS_B.from, CLASS_B.to),
                "Borderline Spoilage" to deltaE(xyzValues, CLASS_C.from, CLASS_C.to)
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

            // Get meat status from XYZ values
            val meatStatus = getMeatStatusFromXYZ(xyzValues)

            return Triple(meatStatus, labValues, xyzValues)
        }

        // Calculate the Euclidean distance between two XYZ colors.
        private fun deltaE(xyz1: FloatArray, xyz2From: FloatArray, xyz2To: FloatArray): Float {
            val dX = xyz1[0] - (xyz2From[0] + xyz2To[0]) / 2
            val dY = xyz1[1] - (xyz2From[1] + xyz2To[1]) / 2
            val dZ = xyz1[2] - (xyz2From[2] + xyz2To[2]) / 2
            return sqrt(dX * dX + dY * dY + dZ * dZ)
        }
    }
}
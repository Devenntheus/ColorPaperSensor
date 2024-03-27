import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.widget.ImageView
import com.example.colorsensor.MainMenuActivity
import com.example.colorsensor.R
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class PlanFPoultryMeatStatus {

    data class LabRange(val from: FloatArray, val to: FloatArray)

    companion object {
        // Average Class LAB values with ranges for accuracy
        private val CLASS_A = LabRange(
            floatArrayOf(73.6200f, 9.0900f, 1.2400f),
            floatArrayOf(69.5658f, 4.0000f, -2.7558f)
        )

        private val CLASS_B = LabRange(
            floatArrayOf(69.5657f, 3.9999f, -2.7559f),
            floatArrayOf(67.6208f, 1.4967f, -4.2375f)
        )

        private val CLASS_C = LabRange(
            floatArrayOf(67.6207f, 1.4966f, -4.2376f),
            floatArrayOf(64.7300f, -0.9167f, -6.7233f)
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

        // Get meat status from LAB values using if-else conditions.
        private fun getMeatStatusFromLAB(labValues: FloatArray): String {
            val l = labValues[0]
            val a = labValues[1]
            val b = labValues[2]

            if (isCloseToClass(l, a, b, CLASS_A))
                return "Fresh"
            else if (isCloseToClass(l, a, b, CLASS_B))
                return "Moderately Fresh"
            else if (isCloseToClass(l, a, b, CLASS_C))
                return "Borderline Spoilage"
            else
                return "Unknown"
        }

        // Check if LAB values are close to a specific class.
        private fun isCloseToClass(l: Float, a: Float, b: Float, labRange: LabRange): Boolean {
            val lDiff = abs(l - labRange.from[0])
            val aDiff = abs(a - labRange.from[1])
            val bDiff = abs(b - labRange.from[2])

            return lDiff <= 2f && aDiff <= 2f && bDiff <= 2f
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
            val meatStatus = getMeatStatusFromLAB(labValues)

            return Triple(meatStatus, labValues, xyzValues)
        }
    }
}
package com.example.colorsensor

import android.util.Log

class PlanBPoultryMeatStatus {

    private val REF = floatArrayOf(73.6f, 9.59f, 3.23f)
    private val ONE_HOUR = floatArrayOf(78.36f, 2.8f, 1.03f)
    private val TWO_HOUR = floatArrayOf(75.57f, 6.76f, 2.23f)
    private val THREE_HOUR = floatArrayOf(74.91f, 7.44f, 2.4f)
    private val FOUR_HOUR = floatArrayOf(76.57f, 5.38f, 1.75f)
    private val FIVE_HOUR = floatArrayOf(77.28f, 4.68f, 1.64f)
    private val SIX_HOUR = floatArrayOf(76.6f, 4.85f, 1.52f)
    private val SEVEN_HOUR = floatArrayOf(77.43f, -0.22f, -1.17f)

    // Member variable to store meat status
    var meatStatus: String = ""

    private fun rgbToHsv(rgb: FloatArray): FloatArray {
        val max = rgb.maxOrNull()!!
        val min = rgb.minOrNull()!!
        val delta = max - min

        // Calculate Hue
        var h = when {
            delta == 0f -> 0f
            max == rgb[0] -> 60 * (((rgb[1] - rgb[2]) / delta) % 6)
            max == rgb[1] -> 60 * ((rgb[2] - rgb[0]) / delta + 2)
            else -> 60 * ((rgb[0] - rgb[1]) / delta + 4)
        }

        if (h < 0) h += 360f

        // Calculate Saturation
        val s = if (max != 0f) delta / max else 0f

        // Calculate Value
        val v = max

        return floatArrayOf(h, s * 100, v * 100)
    }

    fun classifyMeatStatus(hsvValues: FloatArray): String {
        val capturedHue = hsvValues[0]

        // Calculate absolute differences between the captured hue and reference hues
        val differences = mapOf(
            "ONE_HOUR" to Math.abs(capturedHue - ONE_HOUR[0]),
            "TWO_HOUR" to Math.abs(capturedHue - TWO_HOUR[0]),
            "THREE_HOUR" to Math.abs(capturedHue - THREE_HOUR[0]),
            "FOUR_HOUR" to Math.abs(capturedHue - FOUR_HOUR[0]),
            "FIVE_HOUR" to Math.abs(capturedHue - FIVE_HOUR[0]),
            "SIX_HOUR" to capturedHue - SIX_HOUR[0], // Use range check instead of absolute value
            "SEVEN_HOUR" to capturedHue - SEVEN_HOUR[0] // Use range check instead of absolute value
        )

        // Log intermediate values for debugging
        Log.d("Captured Hue", capturedHue.toString())
        differences.forEach { (hour, difference) ->
            Log.d("Difference $hour", difference.toString())
        }

        // Find the key with the minimum difference
        val closestHour = differences.minByOrNull { it.value }?.key ?: ""

        return when (closestHour) {
            "ONE_HOUR" -> "Fresh for ONE_HOUR"
            "TWO_HOUR" -> "Fresh for TWO_HOUR"
            "THREE_HOUR" -> "Fresh for THREE_HOUR"
            "FOUR_HOUR" -> "Fresh for FOUR_HOUR"
            "FIVE_HOUR" -> "Fresh for FIVE_HOUR"
            "SIX_HOUR" -> if (capturedHue in SIX_HOUR[0]..SEVEN_HOUR[0]) "Not fresh for SIX_HOUR" else "Not fresh for SIX_HOUR"
            "SEVEN_HOUR" -> if (capturedHue in SIX_HOUR[0]..SEVEN_HOUR[0]) "Not fresh for SEVEN_HOUR" else "Not fresh for SEVEN_HOUR"
            else -> "Not fresh"
        }
    }

    // PlanBPultryMeatStatus code
    fun getMeatStatusString(hsvValues: FloatArray): String {
        meatStatus = classifyMeatStatus(hsvValues)
        Log.d("Meat Status", meatStatus) // Use Log to print the status
        return meatStatus
    }

}

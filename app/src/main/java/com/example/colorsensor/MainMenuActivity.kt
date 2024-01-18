package com.example.colorsensor

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.cardview.widget.CardView

object GlobalData {
    var meatType: String? = null
}

class MainMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        val textView = findViewById<TextView>(R.id.AppTitleTextView)

        val colors = intArrayOf(
            Color.RED, Color.parseColor("#FFA500"), Color.YELLOW,
            Color.GREEN, Color.BLUE, Color.parseColor("#4B0082"), Color.parseColor("#8A2BE2")
        )

        val animator = ValueAnimator.ofFloat(0f, (colors.size).toFloat()).apply {
            repeatMode = ValueAnimator.RESTART
            repeatCount = ValueAnimator.INFINITE
            duration = 3000 // Set the duration for color transition
            interpolator = LinearInterpolator() // Use linear interpolation for smooth transition
            addUpdateListener { animation ->
                val colorIndex = animation.animatedValue as Float
                val startIndex = colorIndex.toInt() % colors.size
                val endIndex = (startIndex + 1) % colors.size
                val fraction = colorIndex - colorIndex.toInt()

                val startColor = colors[startIndex]
                val endColor = colors[endIndex]

                val currentColor = ArgbEvaluator().evaluate(fraction, startColor, endColor) as Int
                textView.setTextColor(currentColor)
            }
        }
        animator.start()

        val porkCardView = findViewById<CardView>(R.id.PorkCardView)
        porkCardView.setOnClickListener {
            // Set the meat type in the global data
            GlobalData.meatType = "Pork"
            startCaptureImageActivity()
        }

        val beefCardView = findViewById<CardView>(R.id.BeefCardView)
        beefCardView.setOnClickListener {
            GlobalData.meatType = "Beef"
            startCaptureImageActivity()
        }

        val muttonCardView = findViewById<CardView>(R.id.MuttonCardView)
        muttonCardView.setOnClickListener {
            GlobalData.meatType = "Mutton"
            startCaptureImageActivity()
        }

        val poultryCardView = findViewById<CardView>(R.id.PoultryCardView)
        poultryCardView.setOnClickListener {
            GlobalData.meatType = "Poultry"
            startCaptureImageActivity()
        }
    }

    private fun startCaptureImageActivity() {
        val intent = Intent(this, CaptureImageActivity::class.java)
        startActivity(intent)
    }
}
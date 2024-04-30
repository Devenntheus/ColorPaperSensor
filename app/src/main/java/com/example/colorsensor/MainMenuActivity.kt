package com.example.colorsensor

import android.Manifest
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.animation.LinearInterpolator
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object GlobalData {
    var meatType: String? = null
}

class MainMenuActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_REQUEST_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        // Check camera permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request camera permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST_CODE
            )
        }

        val textView = findViewById<TextView>(R.id.AppTitleTextView)

        // Array of colors to be used for title text color transition
        val colors = intArrayOf(
            Color.RED, Color.parseColor("#FFA500"), Color.YELLOW,
            Color.GREEN, Color.BLUE, Color.parseColor("#4B0082"), Color.parseColor("#8A2BE2")
        )

        // Create a value animator for smooth color transition of the title text
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

                // Interpolate between start and end color based on fraction
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

        val poultryCardView = findViewById<CardView>(R.id.PoultryCardView)
        poultryCardView.setOnClickListener {
            GlobalData.meatType = "Poultry"
            startCaptureImageActivity()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Camera permission granted, proceed with app functionality
            } else {
                // Camera permission denied, show a message or handle accordingly
                Toast.makeText(this, "Camera permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to open CaptureImageActivity
    private fun startCaptureImageActivity() {
        val intent = Intent(this, CaptureImageActivity::class.java)
        startActivity(intent)
    }

    // Function to show a dialog indicating under maintenance
    private fun showUnderMaintenanceDialog(callback: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.under_development_dialog, null)
        val closeButtonImageView = dialogView.findViewById<ImageView>(R.id.CloseImageButton)
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        closeButtonImageView.isEnabled = true

        closeButtonImageView.setOnClickListener {

            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
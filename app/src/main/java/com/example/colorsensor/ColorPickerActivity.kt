package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import java.io.File

class ColorPickerActivity : AppCompatActivity() {
    private lateinit var cursorView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        val imagePath = intent.getStringExtra("capturedImagePath")
        val orientation = intent.getIntExtra("orientation", ExifInterface.ORIENTATION_UNDEFINED)

        if (imagePath != null) {
            val imageView: ImageView = findViewById(R.id.CapturedImageView)
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val rotatedBitmap = rotateBitmap(bitmap, orientation)

            imageView.setImageBitmap(rotatedBitmap)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            cursorView = findViewById<View>(R.id.CursorPicker)
            imageView.setOnTouchListener { _, event ->
                val maxX = imageView.width - cursorView.width
                val maxY = imageView.height - cursorView.height
                val confirmCheckImageButton = findViewById<ImageView>(R.id.ConfirmCheckImageButton)

                confirmCheckImageButton.setOnClickListener {
                    // Show progress dialog
                    showProgressDialog()
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        // Update cursor position based on click/drag within the ImageView
                        val newX = event.x - cursorView.width / 2
                        val newY = event.y - cursorView.height - 50
                        cursorView.x = newX.coerceIn(0f, maxX.toFloat())
                        cursorView.y = newY.coerceIn(0f, maxY.toFloat())

                        // Show and enable ConfirmCheckImageView on touch move
                        confirmCheckImageButton.visibility = View.VISIBLE
                        confirmCheckImageButton.isEnabled = true
                    }
                }
                true
            }
        } else {
            // Handle the case where imagePath is null
            Toast.makeText(this, "Error: Captured image path is null.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90F)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180F)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270F)
            else -> return bitmap // No rotation needed
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Function to show progress dialog
    private fun showProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()

        // You can customize the message here
        textViewMessage.text = "Processing..."

        // Dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog.dismiss()
            // Add any additional logic to handle the completion of your task
        }, 5000) // Adjust the delay time as needed
    }
}

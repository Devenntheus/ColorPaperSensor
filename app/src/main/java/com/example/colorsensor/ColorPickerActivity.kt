package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
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
        val orientation = intent.getIntExtra("orientation", ExifInterface.ORIENTATION_NORMAL)

        if (imagePath != null) {
            val imageView: ImageView = findViewById(R.id.CapturedImageView)
            val bitmap = BitmapFactory.decodeFile(imagePath)
            val rotatedBitmap = bitmap;

            imageView.setImageBitmap(rotatedBitmap)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            cursorView = findViewById<View>(R.id.CursorPicker)
            imageView.setOnTouchListener { _, event ->
                val maxX = imageView.width - cursorView.width
                val maxY = imageView.height - cursorView.height
                val confirmCheckImageButton = findViewById<ImageView>(R.id.ConfirmCheckImageButton)

                confirmCheckImageButton.setOnClickListener {
                    /*// Show the first progress dialog
                    showProgressDialog {
                        // This block will be executed after the first dialog is dismissed
                        // Add logic to display the second dialog or perform other actions
                        showSecondDialog()
                    }*/

                    // Create an Intent to start the HistoryDetailsActivity
                    val intent = Intent(this, HistoryDetailsActivity::class.java)

                    // Optionally, you can add extra data to the intent if needed
                    // intent.putExtra("key", "value")

                    // Start the activity
                    startActivity(intent)
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

    // Function to show progress dialog
    private fun showProgressDialog(callback: () -> Unit) {
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
            // Execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 3000) // Adjust the delay time as needed
    }

    private fun showSecondDialog() {
        // Create and show the second dialog or perform other actions
        val secondDialogView = layoutInflater.inflate(R.layout.meat_description_dialog, null)
        // Customize second dialog as needed
        val secondAlertDialog = AlertDialog.Builder(this)
            .setView(secondDialogView)
            .setCancelable(false)
            .create()

        // Show the second dialog
        secondAlertDialog.show()

        // Find the CloseImageButton in the second dialog view
        val closeImageButton = secondDialogView.findViewById<ImageButton>(R.id.CloseImageButton)

        // Set OnClickListener for the CloseImageButton
        closeImageButton.setOnClickListener {
            // Dismiss the second dialog when the CloseImageButton is clicked
            secondAlertDialog.dismiss()
        }

        // Optionally, you can still use the Handler for the delay logic
        Handler().postDelayed({
            // Add any additional logic to handle the completion of the second task
        }, 3000) // Adjust the delay time as needed
    }


}

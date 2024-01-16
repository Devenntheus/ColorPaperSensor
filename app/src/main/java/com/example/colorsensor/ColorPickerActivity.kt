package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast

class ColorPickerActivity : AppCompatActivity() {
    private lateinit var cursorView: View
    private lateinit var cursorFocusView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        val imagePath = intent.getStringExtra("capturedImagePath")

        if (imagePath != null) {
            val imageView: ImageView = findViewById(R.id.CapturedImageView)
            val bitmap = BitmapFactory.decodeFile(imagePath)
            imageView.setImageBitmap(bitmap)
            imageView.scaleType = ImageView.ScaleType.CENTER_CROP

            cursorView = findViewById<View>(R.id.CursorPicker)
            cursorFocusView = findViewById<View>(R.id.CursorFocusView)

            val confirmCheckImageButton = findViewById<ImageView>(R.id.ConfirmCheckImageButton)

            data class Coordinates(var x: Float = 0f, var y: Float = 0f)

            val touchEventCoordinates = Coordinates()

            imageView.setOnTouchListener { _, touchEvent ->
                val maxX = imageView.width - cursorView.width
                val maxY = imageView.height - cursorView.height

                when (touchEvent.action) {
                    MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                        val newX = touchEvent.x - cursorView.width / 2
                        val newY = touchEvent.y - cursorView.height
                        cursorView.x = newX.coerceIn(0f, maxX.toFloat())
                        cursorView.y = newY.coerceIn(0f, maxY.toFloat())

                        // Move the entire CursorPicker frame layout
                        findViewById<FrameLayout>(R.id.CursorPicker).x = cursorView.x
                        findViewById<FrameLayout>(R.id.CursorPicker).y = cursorView.y

                        // Update the cursor color based on the hex value under the cursor
                        updateCursorColorUnderCursor(imageView, cursorView)

                        confirmCheckImageButton.visibility = View.VISIBLE
                        confirmCheckImageButton.isEnabled = true

                        // Update the touch event coordinates
                        touchEventCoordinates.x = touchEvent.x
                        touchEventCoordinates.y = touchEvent.y
                    }
                }

                true
            }

            confirmCheckImageButton.setOnClickListener {
                // show progress dialog
                showProgressDialog {
                    // use the center coordinates inside the CursorFocusView
                    val centerX = cursorView.x + cursorView.width / 2
                    val centerY = cursorView.y + cursorView.height / 2


                    // use the stored touch event coordinates
                    val color = getColorAtCursor(bitmap, centerX.toInt(), centerY.toInt())

                    // dismiss the progress dialog and show the color dialog
                    showColorDialog(color)
                }
            }

        } else {
            Toast.makeText(this, "Error: Captured image path is null.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun updateCursorColorUnderCursor(imageView: ImageView, cursorView: View) {
        val centerX = cursorView.x + cursorView.width / 2
        val centerY = cursorView.y + cursorView.height / 2

        // Get the color under the cursor
        val color = getColorAtCursor(
            (imageView.drawable as BitmapDrawable).bitmap,
            centerX.toInt(),
            centerY.toInt()
        )

        // Set the color of the CursorOutlineView based on the color under the cursor
        val cursorOutlineView = findViewById<View>(R.id.CursorOutlineView)

        // Create a GradientDrawable with the desired shape and set the stroke color
        val gradientDrawable = GradientDrawable()
        gradientDrawable.shape = GradientDrawable.OVAL
        gradientDrawable.setStroke(10, Color.parseColor(color)) // Set the stroke color based on the color under the cursor

        // Set a transparent color for the background inside
        gradientDrawable.setColor(Color.TRANSPARENT)

        // Set the GradientDrawable as the background of the CursorOutlineView
        cursorOutlineView.background = gradientDrawable
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

    private fun showColorDialog(color: String) {
        val dialogView = layoutInflater.inflate(R.layout.meat_description_dialog, null)
        val colorImageView = dialogView.findViewById<ImageView>(R.id.ConfirmImageView)
        val colorNameTextView = dialogView.findViewById<TextView>(R.id.ColorNameTextView)
        val hexCodeTextView = dialogView.findViewById<TextView>(R.id.HexCodeTextView)

        // Create a GradientDrawable and set its color based on the hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(color))
        gradientDrawable.shape = GradientDrawable.OVAL

        // Apply the drawable to the ImageView
        colorImageView.setImageDrawable(gradientDrawable)

        // Set color name (you can customize this logic based on color)
        val colorName = getColorName(color)
        colorNameTextView.text = colorName

        // Set the hex code
        hexCodeTextView.text = color

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()

        Handler().postDelayed({
            alertDialog.dismiss()
        }, 3000)
    }

    private fun getColorAtCursor(bitmap: Bitmap, x: Int, y: Int): String {
        // ensure that the coordinates are within the bounds of the bitmap
        val validX = x.coerceIn(0, bitmap.width - 1)
        val validY = y.coerceIn(0, bitmap.height - 1)

        // get the color of the center pixel inside the cursor
        val centerPixel = bitmap.getPixel(validX, validY)
        val red = Color.red(centerPixel)
        val green = Color.green(centerPixel)
        val blue = Color.blue(centerPixel)

        return String.format("#%02X%02X%02X", red, green, blue)
    }

    private fun getColorName(hexColor: String): String {
        val colorMap = mapOf(
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            // Add more color mappings as needed
        )

        return colorMap[hexColor] ?: "Unknown"
    }
}
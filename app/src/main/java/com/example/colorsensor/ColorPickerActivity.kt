package com.example.colorsensor

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView

class ColorPickerActivity : AppCompatActivity() {
    private lateinit var cursorView: View

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        val imageBytes = intent.getByteArrayExtra("capturedImage")
        val orientation = intent.getIntExtra("orientation", ExifInterface.ORIENTATION_UNDEFINED)

        val imageView: ImageView = findViewById(R.id.CapturedImageView)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes?.size ?: 0)
        val rotatedBitmap = rotateBitmap(bitmap, orientation)

        imageView.setImageBitmap(rotatedBitmap)
        imageView.scaleType = ImageView.ScaleType.CENTER_CROP

        cursorView = findViewById<View>(R.id.CursorPicker)
        imageView.setOnTouchListener { _, event ->
            val maxX = imageView.width - cursorView.width
            val maxY = imageView.height - cursorView.height

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    //update cursor position based on click point within the ImageView
                    val newX = event.x - cursorView.width / 2
                    val newY = event.y - cursorView.height - 50
                    cursorView.x = newX.coerceIn(0f, maxX.toFloat())
                    cursorView.y = newY.coerceIn(0f, maxY.toFloat())
                }
                MotionEvent.ACTION_MOVE -> {
                    //update cursor position as user drags within ImageView
                    val newX = event.x - cursorView.width / 2
                    val newY = event.y - cursorView.height - 50
                    cursorView.x = newX.coerceIn(0f, maxX.toFloat())
                    cursorView.y = newY.coerceIn(0f, maxY.toFloat())
                }
            }
            true
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
}
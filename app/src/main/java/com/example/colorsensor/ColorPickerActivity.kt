package com.example.colorsensor

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
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
        val imageView: ImageView = findViewById(R.id.CapturedImageView)
        val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes?.size ?: 0)
        imageView.setImageBitmap(bitmap)

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
}
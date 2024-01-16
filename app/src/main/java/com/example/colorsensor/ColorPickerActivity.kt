package com.example.colorsensor

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
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

class ColorPickerActivity : AppCompatActivity() {
    private lateinit var crossHairImageView: ImageView
    private lateinit var capturedImageView: ImageView
    private lateinit var confirmCheckImageButton: ImageView
    private lateinit var confirmImageView: ImageView
    private lateinit var imageFilePath: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_color_picker)

        crossHairImageView = findViewById(R.id.CrosshairImageView)
        capturedImageView = findViewById(R.id.CapturedImageView)
        confirmCheckImageButton = findViewById(R.id.ConfirmCheckImageButton)
        confirmImageView = findViewById(R.id.ConfirmImageView)

        //get the image path from the intent
        imageFilePath = intent.getStringExtra("capturedImagePath") ?: ""

        //load the image into CapturedImageView
        displayCapturedImage()

        //set onTouchListener to move CrosshairImageView
        setCrosshairTouchListener()

        confirmCheckImageButton.setOnClickListener {
            //get the hex color under the crosshair
            val hexColor = getHexColorUnderCrosshair()

            //show progress dialog and then display color dialog
            showProgressDialog {
                showColorDialog(hexColor)
            }
        }
    }

    private fun displayCapturedImage() {
        if (imageFilePath.isNotEmpty()) {
            val bitmap = BitmapFactory.decodeFile(imageFilePath)
            capturedImageView.setImageBitmap(bitmap)
            capturedImageView.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setCrosshairTouchListener() {
        val frameLayout = findViewById<FrameLayout>(R.id.ImageFrameLayout)

        frameLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    //update the position of crosshair
                    val newX = event.rawX - crossHairImageView.width / 2
                    val newY = event.rawY - crossHairImageView.height - 150f

                    //ensure the crosshair stays within the frame layout
                    if (newX >= 0 && newX <= frameLayout.width - crossHairImageView.width &&
                        newY >= 0 && newY <= frameLayout.height - crossHairImageView.height
                    ) {
                        crossHairImageView.x = newX
                        crossHairImageView.y = newY

                        //update ConfirmCheckImageButton visibility and enable state
                        confirmCheckImageButton.visibility = View.VISIBLE
                        confirmCheckImageButton.isEnabled = true
                    }
                }
            }
            true
        }
    }



    private fun getHexColorUnderCrosshair(): String {
        val bitmap = getBitmapFromImageView(capturedImageView)
        val x = crossHairImageView.x.toInt() + crossHairImageView.width / 2
        val y = crossHairImageView.y.toInt() + crossHairImageView.height / 2
        return getHexColorFromBitmap(bitmap, x, y)
    }

    private fun getBitmapFromImageView(imageView: ImageView): Bitmap {
        imageView.isDrawingCacheEnabled = true
        imageView.buildDrawingCache(true)
        val bitmap = Bitmap.createBitmap(imageView.drawingCache)
        imageView.isDrawingCacheEnabled = false
        return bitmap
    }

    private fun getHexColorFromBitmap(bitmap: Bitmap, x: Int, y: Int): String {
        val pixel = bitmap.getPixel(x, y)
        return String.format("#%06X", 0xFFFFFF and pixel)
    }

    private fun getColorName(hexColor: String): String {
        val colorMap = mapOf(
            "#FF0000" to "Red",
            "#00FF00" to "Green",
            "#0000FF" to "Blue",
            //add more color mappings as needed
        )

        return colorMap[hexColor] ?: "Unknown"
    }

    //function to show progress dialog
    private fun showProgressDialog(callback: () -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.progress_dialog, null)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val textViewMessage = dialogView.findViewById<TextView>(R.id.textViewMessage)

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        alertDialog.show()

        //you can customize the message here
        textViewMessage.text = getString(R.string.process)

        //dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 1000) //adjust the delay time as needed
    }

    private fun showColorDialog(color: String) {
        val dialogView = layoutInflater.inflate(R.layout.meat_description_dialog, null)
        val closeButtonImageView = dialogView.findViewById<ImageView>(R.id.CloseImageButton)
        val colorImageView = dialogView.findViewById<ImageView>(R.id.ConfirmImageView)
        val meatTypeTextView = dialogView.findViewById<TextView>(R.id.MeatTypeTextView)
        val colorNameTextView = dialogView.findViewById<TextView>(R.id.ColorNameTextView)
        val hexCodeTextView = dialogView.findViewById<TextView>(R.id.HexCodeTextView)

        //create a GradientDrawable and set its color based on the hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(color))
        gradientDrawable.shape = GradientDrawable.OVAL

        //apply the drawable to the ImageView
        colorImageView.setImageDrawable(gradientDrawable)

        //set meat type
        val meatType = GlobalData.meatType
        meatTypeTextView.text = meatType.toString()

        //set color name (you can customize this logic based on color)
        val colorName = getColorName(color)
        colorNameTextView.text = colorName

        //set the hex code
        hexCodeTextView.text = color

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setCancelable(false)

        val alertDialog = alertDialogBuilder.create()

        //set close button click listener
        closeButtonImageView.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }
}
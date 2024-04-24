package com.example.colorsensor

import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.media.Image
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.TypedValue
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.collection.floatFloatMapOf

class MeatDescriptionActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meat_description)

        showProgressDialog {}
            // Retrieve data from intent extras
            val meatImageString = intent.getStringExtra("meatImage")
            val meatStatus = intent.getStringExtra("meatStatus")
            val meatType = intent.getStringExtra("meatType")
            val colorName = intent.getStringExtra("colorName")
            val hexCode = intent.getStringExtra("hexCode")
            val labValues = intent.getStringExtra("labValues")

            // Example of displaying data in TextViews (replace with your UI elements)
            val meatImageTextView = findViewById<ImageView>(R.id.meatImageView)
            val meatStatusTextView = findViewById<TextView>(R.id.statusTextView)
            val meatTypeTextView = findViewById<TextView>(R.id.MeatTypeTextView)
            val colorNameTextView = findViewById<TextView>(R.id.ColorNameTextView)
            val hexCodeTextView = findViewById<TextView>(R.id.HexCodeTextView)
            val labValuesTextView = findViewById<TextView>(R.id.LabValuesTextView)
            val capturedImageView = findViewById<ImageView>(R.id.ShowColorImage)
            val referenceImageView = findViewById<ImageView>(R.id.ShowReferenceColorImage)
            val cameraImageView = findViewById<ImageView>(R.id.cameraImageView)
            val homeImageView = findViewById<ImageView>(R.id.homeImageView)


            // Set click listener for cameraImageView
            cameraImageView.setOnClickListener {
                // Launch the Camera Activity with intent
                val intent = Intent(this, CaptureImageActivity::class.java)
                intent.putExtra("meatType", GlobalData.meatType.toString()) // Pass meat type to CameraActivity if needed
                startActivity(intent)
            }

            // Set click listener for homeImageView
            homeImageView.setOnClickListener {
                // Redirect to MainMenuActivity
                val intent = Intent(this, MainMenuActivity::class.java)
                startActivity(intent)
                finish() // Optional: finish the current activity
            }

            // Decode and display the image
            meatImageTextView.setImageBitmap(decodeBase64ToBitmap(meatImageString))
            meatStatusTextView.text = "$meatStatus"
            meatTypeTextView.text = "$meatType"
            colorNameTextView.text = "$colorName"
            hexCodeTextView.text = "$hexCode"
            labValuesTextView.text = "$labValues"

            // Set text color of meatStatusTextView based on the hex code
            setTextColorBasedOnHexCode(meatStatusTextView, hexCode)

            meatImageTextView.setImageBitmap(
                decodeBase64ToBitmap(
                    meatImageString,
                    cornerRadiusDp = 5f
                )
            )


            setCapturedColor(capturedImageView, hexCode)
            setReferenceColor(referenceImageView, meatType)


        hideProgressDialog();
    }

    // Function to decode base64 string to Bitmap
    private fun decodeBase64ToBitmap(meatImageString: String?, cornerRadiusDp: Float = 5f): Bitmap? {
        if (meatImageString == null) {
            return null
        }

        // Decode base64 string to byte array
        val imageBytes = Base64.decode(meatImageString, Base64.DEFAULT)

        // Decode byte array to Bitmap
        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // Apply border radius
        return addRoundedBorderToBitmap(decodedBitmap, cornerRadiusDp)
    }

    private fun addRoundedBorderToBitmap(bitmap: Bitmap, cornerRadiusDp: Float): Bitmap {
        // Calculate corner radius in pixels
        val cornerRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            cornerRadiusDp,
            Resources.getSystem().displayMetrics
        )

        // Create a new bitmap with added border
        val roundedBitmap = Bitmap.createBitmap(
            bitmap.width,
            bitmap.height,
            bitmap.config
        )

        // Create a canvas with the new bitmap
        val canvas = Canvas(roundedBitmap)

        // Draw the original bitmap on the canvas
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        // Create a paint for drawing the border
        val paint = Paint()
        paint.isAntiAlias = true
        paint.color = Color.BLACK // You can change the border color here
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            5f, // Border width in dp
            Resources.getSystem().displayMetrics
        )

        // Create a rect with rounded corners for the border
        val rect = RectF(
            paint.strokeWidth / 2,
            paint.strokeWidth / 2,
            (bitmap.width - paint.strokeWidth / 2),
            (bitmap.height - paint.strokeWidth / 2)
        )
        canvas.drawRoundRect(rect, cornerRadiusPx, cornerRadiusPx, paint)

        return roundedBitmap
    }

    // Function to set text color based on hex code
    private fun setTextColorBasedOnHexCode(textView: TextView, hexCode: String?) {
        if (hexCode != null) {
            // Set the text color of the TextView based on the hex code
            textView.setTextColor(Color.parseColor(hexCode))
        }
    }

    private fun setCapturedColor(capturedImageView: ImageView, hexCode: String?) {
        if (hexCode != null) {
            // Set background color of the capturedImageView
            capturedImageView.setBackgroundColor(Color.parseColor(hexCode))
        }
    }

    private fun setReferenceColor(referenceImageView: ImageView, meatType: String?) {
        // Set background color of the ImageView based on meat status
        if (meatType == "Fresh") {
            referenceImageView.setBackgroundColor(Color.rgb(185, 170, 177))
        } else if (meatType == "Moderately Fresh") {
            referenceImageView.setBackgroundColor(Color.rgb(165, 165, 173))
        } else if (meatType == "Borderline Spoilage") {
            referenceImageView.setBackgroundColor(Color.rgb(163, 163, 171))
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

        alertDialog = alertDialogBuilder.create()

        alertDialog?.show()

        //you can customize the message here
        textViewMessage.text = getString(R.string.loading)

        //dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog?.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 5000) //adjust the delay time as needed
    }

    // Function to hide progress dialog
    private fun hideProgressDialog() {

        alertDialog?.dismiss()
    }
}


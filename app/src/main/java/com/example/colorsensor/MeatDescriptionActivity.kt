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
        val captureImageString = intent.getStringExtra("meatImage")
        val meatStatus = intent.getStringExtra("meatStatus")
        val meatType = intent.getStringExtra("meatType")
        val colorName = intent.getStringExtra("colorName")
        val hexCode = intent.getStringExtra("hexCode")
        val redValue = intent.getStringExtra("redValue")
        val labValues = intent.getStringExtra("labValues")

        // Example of displaying data in TextViews (replace with your UI elements)
        val captureImageView = findViewById<ImageView>(R.id.captureImageView)
        val meatStatusTextView = findViewById<TextView>(R.id.statusTextView)
        val meatTypeTextView = findViewById<TextView>(R.id.MeatTypeTextView)
        val colorNameTextView = findViewById<TextView>(R.id.ColorNameTextView)
        val hexCodeTextView = findViewById<TextView>(R.id.HexCodeTextView)
        val redValueTextView = findViewById<TextView>(R.id.RedTextView)
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
        captureImageView.setImageBitmap(decodeBase64ToBitmap(captureImageString))
        meatStatusTextView.text = "$meatStatus"
        meatTypeTextView.text = "$meatType"
        colorNameTextView.text = "$colorName"
        hexCodeTextView.text = "$hexCode"
        redValueTextView.text = "$redValue"
        labValuesTextView.text = "$labValues"

        // Set text color of meatStatusTextView based on the hex code
        setTextColorBasedOnHexCode(meatStatusTextView, hexCode)
        setCapturedColor(capturedImageView, hexCode)
        setReferenceColor(referenceImageView, meatStatus, meatType)
        hideProgressDialog();
    }

    // Function to decode base64 string to Bitmap
    private fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String == null) {
            return null
        }

        // Decode base64 string to byte array
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

        // Decode byte array to Bitmap
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
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

    private fun setReferenceColor(referenceImageView: ImageView, meatStatus: String?, meatType: String?) {
        if (meatType == "Poultry"){
            // Set background color of the ImageView based on meat status
            if (meatStatus == "Fresh") {
                referenceImageView.setBackgroundColor(Color.rgb(191, 174, 176))
            } else if (meatStatus == "Borderline Spoilage") {
                referenceImageView.setBackgroundColor(Color.rgb(182, 173, 176))
            } else if (meatStatus == "Spoiled") {
                referenceImageView.setBackgroundColor(Color.rgb(175, 173, 177))
            }
        } else if (meatType == "Pork"){
            // Set background color of the ImageView based on meat status
            if (meatStatus == "Fresh") {
                referenceImageView.setBackgroundColor(android.graphics.Color.rgb(135, 80, 94))
            } else if (meatStatus == "Moderately Fresh") {
                referenceImageView.setBackgroundColor(android.graphics.Color.rgb(136, 93, 105))
            } else if (meatStatus == "Borderline Spoilage") {
                referenceImageView.setBackgroundColor(android.graphics.Color.rgb(119, 88, 100))
            }
        } else if (meatType == "Beef"){
        // Set background color of the ImageView based on meat status
        if (meatStatus == "Fresh") {
            referenceImageView.setBackgroundColor(android.graphics.Color.rgb(193, 180, 182))
        } else if (meatStatus == "Moderately Fresh") {
            referenceImageView.setBackgroundColor(android.graphics.Color.rgb(189, 177, 180))
        } else if (meatStatus == "Borderline Spoilage") {
            referenceImageView.setBackgroundColor(android.graphics.Color.rgb(184, 179, 183))
        } else if (meatStatus == "Spoiled") {
            referenceImageView.setBackgroundColor(android.graphics.Color.rgb(176, 176, 180))
        }
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
package com.example.colorsensor

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class HistoryDetailsActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_v2_history_details)


        // Receive the ID of the selected item from the intent
        val itemId = intent.getStringExtra("id")
        Log.d("HistoryDetailsActivity", "Received itemId: $itemId")

        // Use the ID to fetch additional details from Firebase or display as needed
        fetchDataFromFirebase(itemId)

        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        // Set click listener for homeImageView
        val homeImageView = findViewById<ImageView>(R.id.homeImageView)
        homeImageView.setOnClickListener {
            // Redirect to MainMenuActivity
            val intent = Intent(this, MainMenuActivity::class.java)
            startActivity(intent)
            finish() // Optional: finish the current activity
        }
    }

    private fun fetchDataFromFirebase(itemId: String?) {
        if (itemId == null) {
            // Handle the case where the item ID is null
            return
        }

        showProgressDialog {  }
        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("History").document(itemId)

        Log.d("HistoryDetailsActivity", "Fetched itemId: $itemId")

        documentReference.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    try {
                        Log.d("HistoryDetailsActivity", "Retrieving Meat Description")
                        // Retrieve data from the documentSnapshot
                        val meatType = documentSnapshot.getString("meatType")
                        val meatStatus = documentSnapshot.getString("meatStatus")
                        val color = documentSnapshot.getString("color")
                        val hexCode = documentSnapshot.getString("hexCode")
                        val redValue = documentSnapshot.getString("redValue")
                        val labValue = documentSnapshot.getString("labValue")
                        val meatImageString = documentSnapshot.getString("meatImage")

                        // Example of displaying data in TextViews (replace with your UI elements)
                        val meatImageView = findViewById<ImageView>(R.id.meatImageView)
                        val meatStatusTextView = findViewById<TextView>(R.id.statusTextView)
                        val meatTypeTextView = findViewById<TextView>(R.id.MeatTypeTextView)
                        val colorNameTextView = findViewById<TextView>(R.id.ColorNameTextView)
                        val hexCodeTextView = findViewById<TextView>(R.id.HexCodeTextView)
                        val redTextView = findViewById<TextView>(R.id.RedTextView)
                        val labValuesTextView = findViewById<TextView>(R.id.LabValuesTextView)
                        val capturedImageView = findViewById<ImageView>(R.id.ShowColorImage)
                        val referenceImageView = findViewById<ImageView>(R.id.ShowReferenceColorImage)

                        Log.d("HistoryDetailsActivity", "Displaying Meat Description (Status:  $meatStatus, Type: $meatType, Color: $color, Hex Code: $hexCode, LAB Value: $labValue, RED Value: $redValue, Image String: $meatImageString")

                        meatStatusTextView.text = "$meatStatus"
                        meatTypeTextView.text = "$meatType"
                        colorNameTextView.text = "$color"
                        hexCodeTextView.text = "$hexCode"
                        labValuesTextView.text = "$labValue"
                        redTextView.text = "$redValue"

                        Log.d("HistoryDetailsActivity", "Retrieving Meat Description")

                        // Set text color of meatStatusTextView based on the hex code
                        setTextColorBasedOnHexCode(meatStatusTextView, hexCode)
                        Log.d("HistoryDetailsActivity", "Setting Text Color")
                        meatImageView.setImageBitmap(decodeBase64ToBitmap(meatImageString))

                        setCapturedColor(capturedImageView, hexCode)
                        setReferenceColor(referenceImageView, meatStatus, meatType)

                        hideProgressDialog();

                        Log.d("HistoryDetailsActivity", "SUCCESSFUL RETRIEVAL")
                    } catch (e: NoSuchFileException) {
                        Log.d("HistoryDetailsActivity", "ERROR CATCH RETRIEVAL")
                        // Handle the NoSuchFileException
                        // This could involve logging the error, displaying a message to the user, or taking other appropriate action
                        e.printStackTrace() // Log the exception for debugging
                    } finally {
                        Log.d("HistoryDetailsActivity", "ERROR FINALLY RETRIEVAL")
                        hideProgressDialog() // Ensure the loading dialog is dismissed
                    }
                }
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

    // Function to set text color based on hex code
    private fun setTextColorBasedOnHexCode(textView: TextView, hexCode: String?) {
        if (hexCode != null) {
            // Set the text color of the TextView based on the hex code
            textView.setTextColor(Color.parseColor(hexCode))
        }
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
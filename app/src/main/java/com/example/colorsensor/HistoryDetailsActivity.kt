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
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class HistoryDetailsActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_details)

        // Receive the ID of the selected item from the intent
        val itemId = intent.getStringExtra("id")

        // Use the ID to fetch additional details from Firebase or display as needed
        fetchDataFromFirebase(itemId)

        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }
    }

    // Function to fetch data from Firebase based on item ID
    private fun fetchDataFromFirebase(itemId: String?) {
        if (itemId == null) {
            // Handle the case where the item ID is null
            return
        }

        showProgressDialog {  }
        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("History").document(itemId)

        documentReference.get()
            .addOnSuccessListener { documentSnapshot ->
                hideProgressDialog() // Dismiss the loading dialog
                if (documentSnapshot.exists()) {
                    // Retrieve data from the documentSnapshot
                    val meatType = documentSnapshot.getString("meatType")
                    val color = documentSnapshot.getString("color")
                    val hexCode = documentSnapshot.getString("hexCode")
                    val meatStatus = documentSnapshot.getString("meatStatus")
                    val meatImageString = documentSnapshot.getString("meatImage")

                    // Now you can use these variables to update your UI in HistoryDetailsActivity
                    val meatTypeTextView = findViewById<TextView>(R.id.typeTextView)
                    val colorTextView = findViewById<TextView>(R.id.colorTextView)
                    val hexCodeTextView = findViewById<TextView>(R.id.hexCodeTextView)
                    val meatStatusTextView = findViewById<TextView>(R.id.statusTextView)
                    val colorImageView = findViewById<ImageView>(R.id.colorImageView)
                    val meatImageView = findViewById<ImageView>(R.id.meatImageView)

                    meatTypeTextView.text = meatType
                    colorTextView.text = color
                    hexCodeTextView.text = hexCode
                    meatStatusTextView.text = meatStatus

                    // Set the background color of colorImageView based on the hex code
                    setBackgroundColorBasedOnHexCode(colorImageView, hexCode)

                    // Set text color of meatStatusTextView based on the hex code
                    setTextColorBasedOnHexCode(meatStatusTextView, hexCode)

                    // Decode and display the image
                    meatImageView.setImageBitmap(decodeBase64ToBitmap(meatImageString))
                    hideProgressDialog()
                }
            }
    }

    // Function to set the drawable color based on hex code
    private fun setBackgroundColorBasedOnHexCode(imageView: ImageView, hexCode: String?) {
        if (hexCode != null) {
            // Create a GradientDrawable and set its color based on the hex code
            val gradientDrawable = GradientDrawable()
            gradientDrawable.setColor(Color.parseColor(hexCode))
            gradientDrawable.shape = GradientDrawable.OVAL

            // Apply the drawable to the ImageView
            imageView.setImageDrawable(gradientDrawable)
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
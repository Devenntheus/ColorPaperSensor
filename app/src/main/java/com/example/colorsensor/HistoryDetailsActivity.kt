package com.example.colorsensor

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class HistoryDetailsActivity : AppCompatActivity() {

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

    private fun fetchDataFromFirebase(itemId: String?) {
        if (itemId == null) {
            // Handle the case where the item ID is null
            return
        }

        val db = FirebaseFirestore.getInstance()
        val documentReference = db.collection("History").document(itemId)

        documentReference.get()
            .addOnSuccessListener { documentSnapshot ->
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

                    meatTypeTextView.text = meatType
                    colorTextView.text = color
                    hexCodeTextView.text = hexCode
                    meatStatusTextView.text = meatStatus

                    // Decode and display the image
                    val meatImageView = findViewById<ImageView>(R.id.meatImageView)
                    meatImageView.setImageBitmap(decodeBase64ToBitmap(meatImageString))
                }
            }
    }

    private fun decodeBase64ToBitmap(base64String: String?): Bitmap? {
        if (base64String == null) {
            return null
        }

        // Decode base64 string to byte array
        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)

        // Decode byte array to Bitmap
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
}


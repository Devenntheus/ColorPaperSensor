package com.example.colorsensor

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView

class HistoryDetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history_details)

        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            // Create an Intent to start the new activity
            val intent = Intent(this, HistoryActivity::class.java)

            // Optionally, you can add extra data to the intent if needed
            // intent.putExtra("key", "value")

            // Start the activity
            startActivity(intent)
        }
    }
}
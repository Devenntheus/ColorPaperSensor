package com.example.colorsensor;
import SampleAdapter
import SampleData
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)


        val db = Firebase.firestore
        val recyclerView: RecyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create instances of SampleData
        val sampleData1 = SampleData("Type1", "Color1", "#FF0000", "Status1")
        val sampleData2 = SampleData("Type2", "Color2", "#00FF00", "Status2")
        val sampleData3 = SampleData("Type3", "Color3", "#0000FF", "Status3")

        // Create a list of SampleData
        val sampleDataList = listOf(sampleData1, sampleData2, sampleData3)

        // Create an instance of SampleAdapter with the correct parameters
        val adapter = SampleAdapter(this, sampleDataList)

        // Set the adapter for the RecyclerView
        recyclerView.adapter = adapter

        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            // Create an Intent to start the new activity
            val intent = Intent(this, CaptureImageActivity::class.java)

            // Optionally, you can add extra data to the intent if needed
            // intent.putExtra("key", "value")

            // Start the activity
            startActivity(intent)
        }
    }
}

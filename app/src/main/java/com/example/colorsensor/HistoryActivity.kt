package com.example.colorsensor
import SampleAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)

        val db = FirebaseFirestore.getInstance()
        val historyList = arrayListOf<HistoryData>() // Specify the type for the ArrayList

        val recyclerView: RecyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        db.collection("History").get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val history: HistoryData? = document.toObject(HistoryData::class.java)
                        history?.let {
                            historyList.add(it)
                        }
                    }
                    recyclerView.adapter = SampleAdapter(this, historyList)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
            }

        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            // Create an Intent to start the new activity
            val intent = Intent(this, CaptureImageActivity::class.java)

            // Start the activity
            startActivity(intent)
        }
    }
}

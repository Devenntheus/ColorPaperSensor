package com.example.colorsensor
import SampleAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)

        showProgressDialog {  }
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
                    // Sort the historyList in descending order based on time and date
                    historyList.sortByDescending { it.time }
                    historyList.sortByDescending { it.date }

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
        textViewMessage.text = getString(R.string.loading)

        //dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog.dismiss()
            //execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 5000) //adjust the delay time as needed
    }
}

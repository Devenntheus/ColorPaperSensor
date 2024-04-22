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
import com.google.firebase.firestore.Query
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.LinearLayout

class HistoryActivity : AppCompatActivity() {

    private var alertDialog: AlertDialog? = null
    private lateinit var deviceId: String // Device ID variable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)

        // Get the device ID or token during activity creation
        deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        Log.d("PhoneID", "Phone ID: $deviceId")

        showProgressDialog {
            // This block will be executed after the progress dialog is shown
        }

        val db = FirebaseFirestore.getInstance()
        val historyList = arrayListOf<HistoryData>()

        val recyclerView: RecyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Query to fetch only history items with the matching phone ID
        val query: Query = db.collection("History")
            .whereEqualTo("phoneId", deviceId) // Assuming "phoneId" is the field in your Firestore documents

        query.get()
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

                    // Dismiss the progress dialog here
                    dismissProgressDialog()
                } else {
                    // Make the noRecordLayout visible
                    findViewById<LinearLayout>(R.id.noRecordLayout).visibility = View.VISIBLE
                    Log.d("No Record", "Phone ID: $deviceId")

                    // Dismiss the progress dialog
                    dismissProgressDialog()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, exception.toString(), Toast.LENGTH_SHORT).show()
                // Dismiss the progress dialog in case of failure as well
                dismissProgressDialog()
            }


        val backImageView = findViewById<ImageView>(R.id.backImageView)
        backImageView.setOnClickListener {
            // Create an Intent to start the new activity
            val intent = Intent(this, CaptureImageActivity::class.java)

            // Start the activity
            startActivity(intent)
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

        // You can customize the message here
        textViewMessage.text = getString(R.string.loading)

        // Dismiss the dialog after a certain delay or when the task is completed
        Handler().postDelayed({
            alertDialog?.dismiss()
            // Execute the callback when the first dialog is dismissed
            callback.invoke()
        }, 100000) // Adjust the delay time as needed
    }

    // Function to dismiss progress dialog
    private fun dismissProgressDialog() {
        // Dismiss the progress dialog if it is currently showing
        alertDialog?.dismiss()
    }
}

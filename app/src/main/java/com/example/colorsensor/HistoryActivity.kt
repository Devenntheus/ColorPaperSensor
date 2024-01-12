package com.example.colorsensor;
import SampleAdapter
import SampleData
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.R

class HistoryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)

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
    }
}

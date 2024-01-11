package com.example.colorsensor;
import SampleAdapter
import SampleData
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.R

class SampleHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SampleAdapter

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sample_history)

        recyclerView = findViewById(R.id.historyRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Create instances of SampleData
        val sampleData1 = SampleData("Type1", "Color1", "#FF0000", "Status1")
        val sampleData2 = SampleData("Type2", "Color2", "#00FF00", "Status2")
        val sampleData3 = SampleData("Type3", "Color3", "#0000FF", "Status3")

        // Create a list of SampleData
        val sampleDataList = listOf(sampleData1, sampleData2, sampleData3)

        adapter = SampleAdapter(sampleDataList)
        recyclerView.adapter = adapter
    }
}

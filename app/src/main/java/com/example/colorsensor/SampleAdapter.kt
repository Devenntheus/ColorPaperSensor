// Import necessary Android libraries and classes for working with views and RecyclerView
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.HistoryData
import com.example.colorsensor.HistoryDetailsActivity
import com.example.colorsensor.R

// Define a custom RecyclerView adapter for displaying history data
class SampleAdapter(private val context: Context, private val historyList: ArrayList<HistoryData>) :
    RecyclerView.Adapter<SampleAdapter.ViewHolder>() {

    // Define the ViewHolder class for holding references to views within each item of the RecyclerView
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        val colorTextView: TextView = itemView.findViewById(R.id.colorTextView)
        val hexCodeTextView: TextView = itemView.findViewById(R.id.hexCodeTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
        val idTextView: TextView = itemView.findViewById(R.id.idTextView)
        val imageView: ImageView = itemView.findViewById(R.id.colorImageView)
    }

    // Called when RecyclerView needs a new ViewHolder to represent an item
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Inflate the layout for each item of the RecyclerView
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.show_history, parent, false)
        return ViewHolder(view)
    }

    // Called to bind data to the views within each item of the RecyclerView
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentHolder = historyList[position]

        // Set values for TextViews
        holder.idTextView.visibility = View.INVISIBLE
        holder.typeTextView.text = currentHolder.meatType
        holder.colorTextView.text = currentHolder.color
        holder.hexCodeTextView.text = currentHolder.hexCode
        holder.dateTextView.text = currentHolder.date
        holder.timeTextView.text = currentHolder.time
        holder.statusTextView.text = currentHolder.meatStatus

        // Set text color of statusTextView based on hex code
        currentHolder.hexCode?.let { setTextColorBasedOnHexCode(holder.statusTextView, it) }

        // Set color of imageView based on hex code
        currentHolder.hexCode?.let { setColorBasedOnHexCode(holder.imageView, it) }

        // Set OnClickListener for RecyclerView item
        holder.itemView.setOnClickListener {
            // Create an Intent to start HistoryDetailsActivity
            val intent = Intent(context, HistoryDetailsActivity::class.java)

            // Pass extra data (history ID) to the intent
            intent.putExtra("id", currentHolder.id)

            // Start the activity
            context.startActivity(intent)
        }
    }

    // Set text color of a TextView based on hex code
    private fun setTextColorBasedOnHexCode(textView: TextView, hexCode: String) {
        textView.setTextColor(Color.parseColor(hexCode))
    }

    // Set color of an ImageView based on hex code
    private fun setColorBasedOnHexCode(imageView: ImageView, hexCode: String) {
        // Create a gradient drawable and set its color based on hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(hexCode))
        gradientDrawable.shape = GradientDrawable.OVAL

        // Apply the drawable to the ImageView
        imageView.setImageDrawable(gradientDrawable)
    }

    // Return the total number of items in the data set
    override fun getItemCount(): Int = historyList.size
}

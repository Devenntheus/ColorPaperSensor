import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.HistoryData
import com.example.colorsensor.HistoryDetailsActivity
import com.example.colorsensor.R

class SampleAdapter(private val context: Context, private val historyList: ArrayList<HistoryData>) :
    RecyclerView.Adapter<SampleAdapter.ViewHolder>() {

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.show_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val currentHolder = historyList[position]

        holder.idTextView.visibility = View.INVISIBLE
        holder.typeTextView.text = historyList[position].meatType
        holder.colorTextView.text = historyList[position].color
        holder.hexCodeTextView.text = historyList[position].hexCode
        holder.statusTextView.text = historyList[position].meatStatus
        holder.dateTextView.text = historyList[position].date
        holder.timeTextView.text = historyList[position].time


        // Set an OnClickListener for the item in the RecyclerView
        holder.itemView.setOnClickListener {
            // Create an Intent to start the new activity
            val intent = Intent(context, HistoryDetailsActivity::class.java)

            // Optionally, you can add extra data to the intent if needed
            intent.putExtra("id", currentHolder.id)

            // Start the activity using the context passed to the adapter.
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = historyList.size
}



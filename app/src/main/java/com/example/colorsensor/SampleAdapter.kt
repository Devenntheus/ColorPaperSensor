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

        // Set other TextView values
        holder.idTextView.visibility = View.INVISIBLE
        holder.typeTextView.text = currentHolder.meatType
        holder.colorTextView.text = currentHolder.color
        holder.hexCodeTextView.text = currentHolder.hexCode
        holder.dateTextView.text = currentHolder.date
        holder.timeTextView.text = currentHolder.time
        holder.statusTextView.text = currentHolder.meatStatus

        // Set the text color of statusTextView based on the hex code
        currentHolder.hexCode?.let { setTextColorBasedOnHexCode(holder.statusTextView, it) }

        // Set the color of the ImageView based on the hex code
        currentHolder.hexCode?.let { setColorBasedOnHexCode(holder.imageView, it) }

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

    private fun setTextColorBasedOnHexCode(textView: TextView, hexCode: String) {
        // Set the text color of the TextView based on the hex code
        textView.setTextColor(Color.parseColor(hexCode))
    }

    private fun setColorBasedOnHexCode(imageView: ImageView, hexCode: String) {
        // Create a GradientDrawable and set its color based on the hex code
        val gradientDrawable = GradientDrawable()
        gradientDrawable.setColor(Color.parseColor(hexCode))
        gradientDrawable.shape = GradientDrawable.OVAL

        // Apply the drawable to the ImageView
        imageView.setImageDrawable(gradientDrawable)
    }
    override fun getItemCount(): Int = historyList.size
}

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.colorsensor.R

class SampleAdapter(private val data: List<SampleData>) :
    RecyclerView.Adapter<SampleAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typeTextView: TextView = itemView.findViewById(R.id.typeTextView)
        val colorTextView: TextView = itemView.findViewById(R.id.colorTextView)
        val hexCodeTextView: TextView = itemView.findViewById(R.id.hexCodeTextView)
        val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.show_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // Update the TextViews with data from SampleData
        holder.typeTextView.text = data[position].type
        holder.colorTextView.text = data[position].color
        holder.hexCodeTextView.text = data[position].hexCode
        holder.statusTextView.text = data[position].status
    }

    override fun getItemCount(): Int = data.size
}

data class SampleData(
    val type: String,
    val color: String,
    val hexCode: String,
    val status: String
)

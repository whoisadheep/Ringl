package com.ringl.missedcallhook.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ringl.missedcallhook.R
import com.ringl.missedcallhook.databinding.ItemLogBinding

data class CallLogEntry(
    val number: String,
    val time: String,
    val status: String
)

class LogAdapter(private val logs: List<CallLogEntry>) : RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLogBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = logs[position]
        holder.binding.tvNumber.text = log.number
        holder.binding.tvTime.text = log.time
        holder.binding.tvStatusBadge.text = log.status.uppercase()
        
        val color = if (log.status.equals("Replied", ignoreCase = true)) {
            holder.itemView.context.getColor(R.color.status_active)
        } else {
            holder.itemView.context.getColor(R.color.status_inactive)
        }
        holder.binding.tvStatusBadge.setBackgroundColor(color)
    }

    override fun getItemCount(): Int = logs.size
}

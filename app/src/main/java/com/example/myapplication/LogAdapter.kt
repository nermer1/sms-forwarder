package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogAdapter(private var logs: List<Map<String, Any>>) : RecyclerView.Adapter<LogAdapter.LogViewHolder>() {

    class LogViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStatusIcon: TextView = view.findViewById(R.id.tvStatusIcon)
        val tvLogTarget: TextView = view.findViewById(R.id.tvLogTarget)
        val tvLogTime: TextView = view.findViewById(R.id.tvLogTime)
        val tvLogMessage: TextView = view.findViewById(R.id.tvLogMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_log, parent, false)
        return LogViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        val log = logs[position]
        val isSuccess = (log["is_success"] as? Int) == 1
        val timestamp = log["timestamp"] as? Long ?: 0L
        
        holder.tvStatusIcon.text = if (isSuccess) "✅" else "❌"
        holder.tvLogTarget.text = "[${log["target_type"]}] ${log["target_dest"]}"
        
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        holder.tvLogTime.text = sdf.format(Date(timestamp))
        holder.tvLogMessage.text = log["message"]?.toString() ?: ""
    }

    override fun getItemCount() = logs.size

    fun updateLogs(newLogs: List<Map<String, Any>>) {
        logs = newLogs
        notifyDataSetChanged()
    }
}

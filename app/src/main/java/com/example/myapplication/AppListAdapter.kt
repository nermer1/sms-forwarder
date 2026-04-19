package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.models.AppInfo

class AppListAdapter(
    private var apps: List<AppInfo>
) : RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

    class AppViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivIcon: ImageView = view.findViewById(R.id.ivAppIcon)
        val tvName: TextView = view.findViewById(R.id.tvAppName)
        val tvPkg: TextView = view.findViewById(R.id.tvPackageName)
        val cbSelect: CheckBox = view.findViewById(R.id.cbAppSelect)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_info, parent, false)
        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.ivIcon.setImageDrawable(app.icon)
        holder.tvName.text = app.appName
        holder.tvPkg.text = app.packageName
        holder.cbSelect.isChecked = app.isSelected

        holder.itemView.setOnClickListener {
            app.isSelected = !app.isSelected
            notifyItemChanged(position)
        }
    }

    override fun getItemCount() = apps.size

    fun getSelectedPackages(): String {
        return apps.filter { it.isSelected }.joinToString(", ") { it.packageName }
    }
}
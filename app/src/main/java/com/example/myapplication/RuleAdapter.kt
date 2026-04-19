package com.example.myapplication

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.models.ForwardingRule
import com.google.android.material.switchmaterial.SwitchMaterial

class RuleAdapter(
    private var rules: List<ForwardingRule>,
    private val onRuleClick: (ForwardingRule) -> Unit,
    private val onToggleRule: (ForwardingRule, Boolean) -> Unit
) : RecyclerView.Adapter<RuleAdapter.RuleViewHolder>() {

    class RuleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRuleName: TextView = view.findViewById(R.id.tvRuleName)
        val tvRuleSummary: TextView = view.findViewById(R.id.tvRuleSummary)
        val swEnabled: SwitchMaterial = view.findViewById(R.id.swEnabled)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RuleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_rule, parent, false)
        return RuleViewHolder(view)
    }

    override fun onBindViewHolder(holder: RuleViewHolder, position: Int) {
        val rule = rules[position]
        holder.tvRuleName.text = rule.name
        
        val targetCount = rule.targets?.size ?: 0
        val senderFilter = rule.senderFilter ?: ""
        val keywordFilter = rule.keywordFilter ?: ""
        val targetAppPackages = rule.targetAppPackages ?: ""
        
        val filterDesc = if (senderFilter.isEmpty() && keywordFilter.isEmpty() && targetAppPackages.isEmpty()) "모든 메시지" else "필터 적용됨"
        holder.tvRuleSummary.text = "필터: $filterDesc | 타겟: $targetCount 개"
        
        holder.swEnabled.isChecked = rule.isEnabled
        
        holder.itemView.setOnClickListener { onRuleClick(rule) }
        holder.swEnabled.setOnCheckedChangeListener { _, isChecked ->
            onToggleRule(rule, isChecked)
        }
    }

    override fun getItemCount() = rules.size

    fun updateRules(newRules: List<ForwardingRule>) {
        rules = newRules
        notifyDataSetChanged()
    }
}
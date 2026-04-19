package com.example.myapplication

import android.content.Context
import com.example.myapplication.models.ForwardingRule
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object RuleManager {
    private const val PREF_NAME = "RulePrefs"
    private const val KEY_RULES = "forwarding_rules"
    private val gson = Gson()

    fun getRules(context: Context): MutableList<ForwardingRule> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_RULES, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<ForwardingRule>>() {}.type
        return gson.fromJson(json, type)
    }

    fun saveRules(context: Context, rules: List<ForwardingRule>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = gson.toJson(rules)
        prefs.edit().putString(KEY_RULES, json).apply()
    }

    fun addRule(context: Context, rule: ForwardingRule) {
        val rules = getRules(context)
        rules.add(rule)
        saveRules(context, rules)
    }

    fun updateRule(context: Context, updatedRule: ForwardingRule) {
        val rules = getRules(context)
        val index = rules.indexOfFirst { it.id == updatedRule.id }
        if (index != -1) {
            rules[index] = updatedRule
            saveRules(context, rules)
        }
    }

    fun deleteRule(context: Context, ruleId: String) {
        val rules = getRules(context)
        rules.removeAll { it.id == ruleId }
        saveRules(context, rules)
    }
}
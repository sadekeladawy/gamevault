package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray

class SearchHistoryManager(context: Context) {
    companion object {
        private const val PREFS_NAME = "gamevault_search_history_prefs"
        private const val KEY_HISTORY_JSON = "search_history_json"
        private const val MAX_HISTORY_ITEMS = 10
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<String>>(loadHistory())
    val history: StateFlow<List<String>> = _history.asStateFlow()

    private fun loadHistory(): List<String> {
        val jsonStr = prefs.getString(KEY_HISTORY_JSON, "[]") ?: "[]"
        return try {
            val jsonArray = JSONArray(jsonStr)
            val list = mutableListOf<String>()
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.optString(i)
                if (item.isNotBlank()) list.add(item)
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun addSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return

        val current = loadHistory().toMutableList()
        current.remove(trimmed) // Remove duplicate if exists so it moves to front
        current.add(0, trimmed)

        val trimmedList = current.take(MAX_HISTORY_ITEMS)
        saveHistory(trimmedList)
        _history.value = trimmedList
    }

    fun removeSearchQuery(query: String) {
        val current = loadHistory().toMutableList()
        current.remove(query.trim())
        saveHistory(current)
        _history.value = current
    }

    fun clearSearchHistory() {
        saveHistory(emptyList())
        _history.value = emptyList()
    }

    private fun saveHistory(list: List<String>) {
        val jsonArray = JSONArray()
        list.forEach { jsonArray.put(it) }
        prefs.edit().putString(KEY_HISTORY_JSON, jsonArray.toString()).apply()
    }
}

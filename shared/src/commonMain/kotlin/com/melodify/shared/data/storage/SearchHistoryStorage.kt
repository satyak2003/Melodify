package com.melodify.shared.data.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object SearchHistoryStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File
        get() = File(AppStorage.getStorageDir(), "search_history.json")


    fun saveQuery(query: String) {
        if (query.isBlank()) return
        try {
            val current = loadHistory().toMutableList()
            current.remove(query)
            current.add(0, query)
            val trimmed = current.take(10)
            file.writeText(json.encodeToString(trimmed))
        } catch (e: Exception) {
            println("Failed to save search query: ${e.message}")
        }
    }

    fun removeQuery(query: String) {
        try {
            val current = loadHistory().toMutableList()
            current.remove(query)
            file.writeText(json.encodeToString(current))
        } catch (e: Exception) {
            println("Failed to remove search query: ${e.message}")
        }
    }

    fun loadHistory(): List<String> {
        try {
            if (!file.exists()) return emptyList()
            return json.decodeFromString(file.readText())
        } catch (e: Exception) {
            return emptyList()
        }
    }

    fun clearHistory() {
        try {
            if (file.exists()) file.delete()
        } catch (ignored: Exception) {}
    }
}

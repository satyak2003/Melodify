package com.melodify.shared.data.storage

import com.melodify.shared.domain.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
data class PlayHistoryItem(
    val track: Track,
    val playCount: Int = 1,
    val lastPlayedAt: Long
)

@Serializable
data class HistoryData(
    val items: List<PlayHistoryItem> = emptyList(),
    val timelineStats: Map<String, Int> = emptyMap() // YYYY-MM-DD to minutes
)

object HistoryStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; explicitNulls = false }
    private val file: File
        get() = File(AppStorage.getStorageDir(), "play_history.json")

    private fun loadData(): HistoryData {
        try {
            if (!file.exists()) return HistoryData()
            return json.decodeFromString(file.readText())
        } catch (e: Exception) {
            return HistoryData()
        }
    }

    private fun saveData(data: HistoryData) {
        try {
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun addPlay(track: Track) {
        val current = loadData()
        val existingItem = current.items.find { it.track.id == track.id }
        
        val updatedList = if (existingItem != null) {
            val newList = current.items.toMutableList()
            newList.remove(existingItem)
            newList.add(existingItem.copy(playCount = existingItem.playCount + 1, lastPlayedAt = System.currentTimeMillis()))
            newList
        } else {
            current.items + PlayHistoryItem(track, 1, System.currentTimeMillis())
        }
        
        // Update timeline stats
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayStr = sdf.format(Date())
        val durationMins = (track.durationMs / 1000 / 60).toInt().coerceAtLeast(1)
        val updatedTimeline = current.timelineStats.toMutableMap()
        updatedTimeline[todayStr] = (updatedTimeline[todayStr] ?: 0) + durationMins
        
        saveData(HistoryData(updatedList, updatedTimeline))
    }

    fun getMostPlayed(limit: Int = 10): List<Track> {
        return loadData().items.sortedByDescending { it.playCount }.take(limit).map { it.track }
    }

    fun getRecent(limit: Int = 4): List<Track> {
        return loadData().items.sortedByDescending { it.lastPlayedAt }.take(limit).map { it.track }
    }
    
    fun getWeeklyTimeline(): Map<String, Int> {
        val current = loadData()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("EEE", Locale.getDefault())
        
        val result = mutableMapOf<String, Int>()
        val cal = Calendar.getInstance()
        
        // Go back 6 days + today = 7 days
        cal.add(Calendar.DAY_OF_YEAR, -6)
        
        for (i in 0..6) {
            val dateStr = sdf.format(cal.time)
            val dayName = displayFormat.format(cal.time)
            val minutes = current.timelineStats[dateStr] ?: 0
            result[dayName] = minutes
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return result
    }
}
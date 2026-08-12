package com.melodify.shared.data.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Serializable
data class DailyListeningData(
    val mondaySeconds: Long = 0L,
    val tuesdaySeconds: Long = 0L,
    val wednesdaySeconds: Long = 0L,
    val thursdaySeconds: Long = 0L,
    val fridaySeconds: Long = 0L,
    val saturdaySeconds: Long = 0L,
    val sundaySeconds: Long = 0L,
    val weekKey: String = "" // "YYYY-Www" e.g. "2026-W31"
)

object ListeningStatsStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val baseDir: File
        get() = AppStorage.getStorageDir()

    private val file: File
        get() = File(baseDir, "listening_stats.json")

    /** Returns ISO year-week string like "2026-W31" for the current week. */
    private fun currentWeekKey(): String {
        val cal = Calendar.getInstance()
        cal.minimalDaysInFirstWeek = 4
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        val year = cal.get(Calendar.YEAR)
        return "$year-W%02d".format(week)
    }

    fun addListeningTime(seconds: Long) {
        if (seconds <= 0) return
        try {
            val current = loadData() // auto-resets if week changed
            val dayName = SimpleDateFormat("EEE", Locale.US).format(Date()) // Mon, Tue, etc.
            val updated = when (dayName) {
                "Mon" -> current.copy(mondaySeconds = current.mondaySeconds + seconds)
                "Tue" -> current.copy(tuesdaySeconds = current.tuesdaySeconds + seconds)
                "Wed" -> current.copy(wednesdaySeconds = current.wednesdaySeconds + seconds)
                "Thu" -> current.copy(thursdaySeconds = current.thursdaySeconds + seconds)
                "Fri" -> current.copy(fridaySeconds = current.fridaySeconds + seconds)
                "Sat" -> current.copy(saturdaySeconds = current.saturdaySeconds + seconds)
                "Sun" -> current.copy(sundaySeconds = current.sundaySeconds + seconds)
                else -> current
            }
            file.writeText(json.encodeToString(updated))
        } catch (e: Exception) {
            println("Failed to update listening stats: ${e.message}")
        }
    }

    fun getWeeklyMinutesMap(): Map<String, Int> {
        val data = loadData()
        return mapOf(
            "Mon" to (data.mondaySeconds / 60).toInt(),
            "Tue" to (data.tuesdaySeconds / 60).toInt(),
            "Wed" to (data.wednesdaySeconds / 60).toInt(),
            "Thu" to (data.thursdaySeconds / 60).toInt(),
            "Fri" to (data.fridaySeconds / 60).toInt(),
            "Sat" to (data.saturdaySeconds / 60).toInt(),
            "Sun" to (data.sundaySeconds / 60).toInt()
        )
    }

    private fun loadData(): DailyListeningData {
        val thisWeek = currentWeekKey()
        try {
            if (!file.exists()) {
                // Write fresh data with this week's key
                val fresh = DailyListeningData(weekKey = thisWeek)
                file.writeText(json.encodeToString(fresh))
                return fresh
            }
            val stored = json.decodeFromString<DailyListeningData>(file.readText())
            // If the stored week key is different from this week, reset everything
            if (stored.weekKey != thisWeek) {
                val reset = DailyListeningData(weekKey = thisWeek)
                file.writeText(json.encodeToString(reset))
                return reset
            }
            return stored
        } catch (e: Exception) {
            return DailyListeningData(weekKey = thisWeek)
        }
    }
}

package com.melodify.shared.data.storage

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
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
    val sundaySeconds: Long = 0L
)

object ListeningStatsStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val baseDir: File
        get() {
            val userHome = System.getProperty("user.home")
            val dir = if (userHome != null && userHome != "/" && userHome.isNotBlank()) {
                val f = File(userHome, ".melodify")
                if (f.exists() || f.mkdirs()) f else File(System.getProperty("java.io.tmpdir") ?: ".", ".melodify")
            } else {
                File(System.getProperty("java.io.tmpdir") ?: ".", ".melodify")
            }
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val file: File
        get() = File(baseDir, "listening_stats.json")

    fun addListeningTime(seconds: Long) {
        if (seconds <= 0) return
        try {
            val current = loadData()
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
        try {
            if (!file.exists()) return DailyListeningData()
            return json.decodeFromString(file.readText())
        } catch (e: Exception) {
            return DailyListeningData()
        }
    }
}

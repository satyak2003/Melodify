package com.melodify.shared.data.storage

import com.melodify.shared.domain.model.Queue
import com.melodify.shared.domain.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class StoredLastPlayed(
    val currentTrack: Track? = null,
    val queueTracks: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

object LastPlayedStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; explicitNulls = false }
    private val file: File
        get() = File(AppStorage.getStorageDir(), "last_played.json")


    fun saveLastPlayed(track: Track?, queue: Queue, positionMs: Long, durationMs: Long) {
        try {
            if (track == null) return
            val data = StoredLastPlayed(
                currentTrack = track,
                queueTracks = queue.tracks,
                currentIndex = queue.currentIndex,
                positionMs = positionMs,
                durationMs = durationMs
            )
            file.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("Failed to save last played track: ${e.message}")
        }
    }

    fun loadLastPlayed(): StoredLastPlayed? {
        try {
            if (!file.exists()) return null
            return json.decodeFromString(file.readText())
        } catch (e: Exception) {
            println("Failed to load last played track: ${e.message}")
            return null
        }
    }
}

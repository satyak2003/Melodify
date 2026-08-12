package com.melodify.shared.domain

import com.melodify.shared.data.storage.AppStorage
import com.melodify.shared.data.storage.LibraryStorage
import com.melodify.shared.domain.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class PlayHistory(
    val playCounts: Map<String, Int> = emptyMap(), // trackId -> count
    val recentTrackIds: List<String> = emptyList(), // last 50 track IDs played
    val artistPlayCounts: Map<String, Int> = emptyMap() // artistName -> count
)

object RecommendationEngine {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val file: File get() = File(AppStorage.getStorageDir(), "play_history.json")

    private var cachedHistory: PlayHistory? = null

    fun recordPlay(track: Track) {
        val history = loadHistory()
        val updatedCounts = history.playCounts.toMutableMap()
        updatedCounts[track.id] = (updatedCounts[track.id] ?: 0) + 1

        val updatedArtistCounts = history.artistPlayCounts.toMutableMap()
        track.artists.forEach { artist ->
            updatedArtistCounts[artist.name] = (updatedArtistCounts[artist.name] ?: 0) + 1
        }

        val updatedRecent = (listOf(track.id) + history.recentTrackIds).distinct().take(50)

        val updated = PlayHistory(
            playCounts = updatedCounts,
            recentTrackIds = updatedRecent,
            artistPlayCounts = updatedArtistCounts
        )
        cachedHistory = updated
        try {
            file.writeText(json.encodeToString(updated))
        } catch (e: Exception) {
            println("Failed to save play history: ${e.message}")
        }
    }

    fun getRecommendations(excludeIds: Set<String> = emptySet(), count: Int = 20): List<Track> {
        val history = loadHistory()
        val library = LibraryStorage.loadLibrary()
        val allTracks = (library.localPlaylists + library.spotifyPlaylists)
            .flatMap { it.tracks }
            .distinctBy { it.id }
            .filter { it.id !in excludeIds }

        if (allTracks.isEmpty()) return emptyList()

        // Score each track
        val scored = allTracks.map { track ->
            var score = 0.0
            // Boost by play count (familiarity)
            score += (history.playCounts[track.id] ?: 0) * 2.0
            // Boost by artist affinity
            track.artists.forEach { artist ->
                score += (history.artistPlayCounts[artist.name] ?: 0) * 1.5
            }
            // Heavily boost if the track is liked
            if (library.likedTracks.any { it.id == track.id }) {
                score += 15.0
            }
            // Boost if the artist is in liked tracks
            val hasLikedArtist = library.likedTracks.any { liked -> liked.artists.any { it.name in track.artists.map(com.melodify.shared.domain.model.Artist::name) } }
            if (hasLikedArtist) {
                score += 5.0
            }
            
            // Small penalty for very recently played (avoid repeats)
            val recentIndex = history.recentTrackIds.indexOf(track.id)
            if (recentIndex in 0..4) score -= 3.0 // just played, deprioritize
            else if (recentIndex in 5..15) score -= 1.0 // somewhat recent
            
            track to score
        }

        // Sort by score descending, add some randomness
        return scored
            .sortedByDescending { it.second + (Math.random() * 2.0) } // slight randomization
            .take(count)
            .map { it.first }
    }

    private fun loadHistory(): PlayHistory {
        cachedHistory?.let { return it }
        return try {
            if (file.exists()) {
                val loaded = json.decodeFromString<PlayHistory>(file.readText())
                cachedHistory = loaded
                loaded
            } else PlayHistory()
        } catch (e: Exception) {
            PlayHistory()
        }
    }
}

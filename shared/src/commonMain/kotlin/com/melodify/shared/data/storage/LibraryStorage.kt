package com.melodify.shared.data.storage

import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class StoredLibraryData(
    val spotifyPlaylists: List<Playlist> = emptyList(),
    val youtubePlaylists: List<Playlist> = emptyList(),
    val localPlaylists: List<Playlist> = emptyList(),
    val likedTracks: List<Track> = emptyList(),
    val jellyfinTracks: List<Track> = emptyList()
)

object LibraryStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; explicitNulls = false }
    private val libraryFile: File
        get() = File(AppStorage.getStorageDir(), "library_data.json")


    fun saveLibrary(spotifyPlaylists: List<Playlist>, localPlaylists: List<Playlist>, likedTracks: List<Track>, jellyfinTracks: List<Track> = emptyList()) {
        val current = loadLibrary()
        saveLibrary(spotifyPlaylists, current.youtubePlaylists, localPlaylists, likedTracks, jellyfinTracks)
    }

    fun saveLibrary(spotifyPlaylists: List<Playlist>, youtubePlaylists: List<Playlist>, localPlaylists: List<Playlist>, likedTracks: List<Track>, jellyfinTracks: List<Track> = emptyList()) {
        try {
            val data = StoredLibraryData(spotifyPlaylists, youtubePlaylists, localPlaylists, likedTracks, jellyfinTracks)
            libraryFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("Failed to save library: ${e.message}")
        }
    }

    fun loadLibrary(): StoredLibraryData {
        try {
            if (!libraryFile.exists()) return StoredLibraryData()
            return json.decodeFromString(libraryFile.readText())
        } catch (e: Exception) {
            println("Failed to load library: ${e.message}")
            return StoredLibraryData()
        }
    }

    fun updateTrackYoutubeId(trackId: String, youtubeVideoId: String) {
        val data = loadLibrary()
        
        val updatedSpotify = data.spotifyPlaylists.map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { 
                if (it.id == trackId) it.copy(youtubeVideoId = youtubeVideoId) else it 
            })
        }
        val updatedYoutube = data.youtubePlaylists.map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { 
                if (it.id == trackId) it.copy(youtubeVideoId = youtubeVideoId) else it 
            })
        }
        val updatedLocal = data.localPlaylists.map { playlist ->
            playlist.copy(tracks = playlist.tracks.map { 
                if (it.id == trackId) it.copy(youtubeVideoId = youtubeVideoId) else it 
            })
        }
        val updatedLiked = data.likedTracks.map { 
            if (it.id == trackId) it.copy(youtubeVideoId = youtubeVideoId) else it 
        }
        
        saveLibrary(updatedSpotify, updatedYoutube, updatedLocal, updatedLiked, data.jellyfinTracks)
    }
}

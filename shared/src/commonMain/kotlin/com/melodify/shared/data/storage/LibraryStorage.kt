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
    val localPlaylists: List<Playlist> = emptyList(),
    val likedTracks: List<Track> = emptyList()
)

object LibraryStorage {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; explicitNulls = false }
    private val libraryFile: File
        get() = File(AppStorage.getStorageDir(), "library_data.json")


    fun saveLibrary(spotifyPlaylists: List<Playlist>, localPlaylists: List<Playlist>, likedTracks: List<Track>) {
        try {
            val data = StoredLibraryData(spotifyPlaylists, localPlaylists, likedTracks)
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
}

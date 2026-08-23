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
    val jellyfinTracks: List<Track> = emptyList(),
    val downloadedTracks: List<Track> = emptyList()
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
            val current = loadLibrary()
            val data = StoredLibraryData(spotifyPlaylists, youtubePlaylists, localPlaylists, likedTracks, jellyfinTracks, current.downloadedTracks)
            libraryFile.writeText(json.encodeToString(data))
        } catch (e: Exception) {
            println("Failed to save library: ${e.message}")
        }
    }

    fun loadLibrary(): StoredLibraryData {
        try {
            if (!libraryFile.exists()) return StoredLibraryData()
            val data = json.decodeFromString<StoredLibraryData>(libraryFile.readText())
            
            // Migrate YouTube playlists that might have been accidentally saved in spotifyPlaylists in older versions
            val misplacedYoutubePlaylists = data.spotifyPlaylists.filter { it.source == com.melodify.shared.domain.model.PlaylistSource.YOUTUBE }
            if (misplacedYoutubePlaylists.isNotEmpty()) {
                val updatedSpotify = data.spotifyPlaylists.filter { it.source != com.melodify.shared.domain.model.PlaylistSource.YOUTUBE }
                
                // Merge with existing youtubePlaylists to prevent duplicates by ID
                val mergedYoutube = (data.youtubePlaylists + misplacedYoutubePlaylists).distinctBy { it.id }
                
                val migratedData = data.copy(
                    spotifyPlaylists = updatedSpotify,
                    youtubePlaylists = mergedYoutube
                )
                // Save the migrated data so the fix persists
                saveLibrary(
                    spotifyPlaylists = migratedData.spotifyPlaylists,
                    youtubePlaylists = migratedData.youtubePlaylists,
                    localPlaylists = migratedData.localPlaylists,
                    likedTracks = migratedData.likedTracks,
                    jellyfinTracks = migratedData.jellyfinTracks
                )
                return migratedData
            }
            return data
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
        val updatedDownloaded = data.downloadedTracks.map {
            if (it.id == trackId) it.copy(youtubeVideoId = youtubeVideoId) else it
        }
        val dataWithDownloaded = data.copy(
            spotifyPlaylists = updatedSpotify,
            youtubePlaylists = updatedYoutube,
            localPlaylists = updatedLocal,
            likedTracks = updatedLiked,
            downloadedTracks = updatedDownloaded
        )
        try {
            libraryFile.writeText(json.encodeToString(dataWithDownloaded))
        } catch (e: Exception) {}
    }

    fun addDownloadedTrack(track: Track) {
        val current = loadLibrary()
        val existing = current.downloadedTracks.find { it.id == track.id }
        if (existing == null) {
            val updated = current.copy(downloadedTracks = current.downloadedTracks + track)
            try {
                libraryFile.writeText(json.encodeToString(updated))
            } catch(e: Exception) {}
        }
    }
}
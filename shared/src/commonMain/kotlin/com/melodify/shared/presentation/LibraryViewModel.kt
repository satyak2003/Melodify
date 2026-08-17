package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.api.spotify.SpotifyApi
import com.melodify.shared.api.spotify.SpotifyAuthHelper
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.PlaylistSource
import com.melodify.shared.domain.model.Track
import com.melodify.shared.api.spotify.SpotifyPlaylist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.io.BufferedReader
import java.io.InputStreamReader
import com.melodify.shared.navigation.DeepLinkHandler
import com.melodify.shared.data.storage.LibraryStorage

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val spotifyApi: SpotifyApi,
) : ViewModel() {


    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()



    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()

    private var currentCodeVerifier: String? = null

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val stored = LibraryStorage.loadLibrary()
                _uiState.value = LibraryUiState.Success(
                    localPlaylists = stored.localPlaylists,
                    spotifyPlaylists = stored.spotifyPlaylists,
                    youtubePlaylists = stored.youtubePlaylists,
                    likedTracks = stored.likedTracks,
                    jellyfinTracks = stored.jellyfinTracks
                )
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error(e.message ?: "Failed to load library")
            }
        }
    }



    fun createLocalPlaylist(title: String, description: String? = null) {
        if (title.isBlank()) return
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        val newPlaylist = Playlist(
            id = "local_${System.currentTimeMillis()}",
            title = title,
            description = description,
            source = PlaylistSource.YOUTUBE
        )

        val updatedLocal = state.localPlaylists + newPlaylist
        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        var found = false
        val updatedLocal = state.localPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                found = true
                if (playlist.tracks.any { it.id == track.id }) playlist
                else playlist.copy(
                    tracks = playlist.tracks + track,
                    trackCount = playlist.tracks.size + 1
                )
            } else playlist
        }

        val updatedSpotify = state.spotifyPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                found = true
                if (playlist.tracks.any { it.id == track.id }) playlist
                else playlist.copy(
                    tracks = playlist.tracks + track,
                    trackCount = playlist.tracks.size + 1
                )
            } else playlist
        }

        val updatedYoutube = state.youtubePlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                found = true
                if (playlist.tracks.any { it.id == track.id }) playlist
                else playlist.copy(
                    tracks = playlist.tracks + track,
                    trackCount = playlist.tracks.size + 1
                )
            } else playlist
        }

        if (found) {
            _uiState.value = state.copy(
                localPlaylists = updatedLocal, 
                spotifyPlaylists = updatedSpotify,
                youtubePlaylists = updatedYoutube
            )
            LibraryStorage.saveLibrary(updatedSpotify, updatedYoutube, updatedLocal, state.likedTracks)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        val updatedLocal = state.localPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val newTracks = playlist.tracks.filterNot { it.id == trackId }
                playlist.copy(
                    tracks = newTracks,
                    trackCount = newTracks.size
                )
            } else playlist
        }

        val updatedSpotify = state.spotifyPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val newTracks = playlist.tracks.filterNot { it.id == trackId }
                playlist.copy(
                    tracks = newTracks,
                    trackCount = newTracks.size
                )
            } else playlist
        }

        val updatedYoutube = state.youtubePlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                val newTracks = playlist.tracks.filterNot { it.id == trackId }
                playlist.copy(
                    tracks = newTracks,
                    trackCount = newTracks.size
                )
            } else playlist
        }

        _uiState.value = state.copy(localPlaylists = updatedLocal, spotifyPlaylists = updatedSpotify, youtubePlaylists = updatedYoutube)
        LibraryStorage.saveLibrary(updatedSpotify, updatedYoutube, updatedLocal, state.likedTracks)
    }

    fun deletePlaylist(playlistId: String) {
        val state = _uiState.value as? LibraryUiState.Success ?: return
        val updatedLocal = state.localPlaylists.filter { it.id != playlistId }
        val updatedSpotify = state.spotifyPlaylists.filter { it.id != playlistId }
        val updatedYoutube = state.youtubePlaylists.filter { it.id != playlistId }

        _uiState.value = state.copy(localPlaylists = updatedLocal, spotifyPlaylists = updatedSpotify, youtubePlaylists = updatedYoutube)
        LibraryStorage.saveLibrary(updatedSpotify, updatedYoutube, updatedLocal, state.likedTracks)
    }

    fun importLocalMusicFiles(paths: List<String>) {
        if (paths.isEmpty()) return
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        val newTracks = paths.mapIndexed { index, path ->
            val file = java.io.File(path)
            val nameWithoutExt = file.nameWithoutExtension
            val title = nameWithoutExt.substringBefore("-").trim().ifEmpty { nameWithoutExt }
            val artist = if (nameWithoutExt.contains("-")) nameWithoutExt.substringAfter("-").trim() else "Local Audio"

            Track(
                id = "local_file_${System.currentTimeMillis()}_$index",
                title = title,
                artists = listOf(com.melodify.shared.domain.model.Artist(id = "local_artist_$index", name = artist)),
                album = null,
                thumbnailUrl = null,
                durationMs = 0L,
                source = com.melodify.shared.domain.model.TrackSource.LOCAL,
                localPath = file.absolutePath
            )
        }


        val existingLocal = state.localPlaylists
        val localFilesPlaylist = existingLocal.firstOrNull { it.id == "local_music_files" }

        val updatedLocal = if (localFilesPlaylist != null) {
            existingLocal.map { pl ->
                if (pl.id == "local_music_files") {
                    pl.copy(
                        tracks = pl.tracks + newTracks,
                        trackCount = pl.tracks.size + newTracks.size
                    )
                } else pl
            }
        } else {
            val newPl = Playlist(
                id = "local_music_files",
                title = "Local Music",
                description = "Imported local audio files",
                tracks = newTracks,
                trackCount = newTracks.size,
                source = PlaylistSource.YOUTUBE
            )
            existingLocal + newPl
        }

        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
    }

    fun importLocalMusicFilesWithMetadata(metadataList: List<LocalTrackMetadata>) {
        if (metadataList.isEmpty()) return
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        val newTracks = metadataList.mapIndexed { index, meta ->
            Track(
                id = "local_file_${System.currentTimeMillis()}_$index",
                title = meta.title,
                artists = listOf(com.melodify.shared.domain.model.Artist(id = "local_artist_$index", name = meta.artist)),
                album = meta.album?.let { com.melodify.shared.domain.model.Album(id = "local_album_$index", title = it, thumbnailUrl = meta.artPath) },
                thumbnailUrl = meta.artPath,
                durationMs = meta.durationMs,
                source = com.melodify.shared.domain.model.TrackSource.LOCAL,
                localPath = meta.path
            )
        }

        val existingLocal = state.localPlaylists
        val localFilesPlaylist = existingLocal.firstOrNull { it.id == "local_music_files" }

        val updatedLocal = if (localFilesPlaylist != null) {
            existingLocal.map { pl ->
                if (pl.id == "local_music_files") {
                    pl.copy(
                        tracks = pl.tracks + newTracks,
                        trackCount = pl.tracks.size + newTracks.size
                    )
                } else pl
            }
        } else {
            val newPl = Playlist(
                id = "local_music_files",
                title = "Local Music",
                description = "Imported local audio files",
                tracks = newTracks,
                trackCount = newTracks.size,
                source = PlaylistSource.YOUTUBE
            )
            existingLocal + newPl
        }

        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
    }




    private var currentRedirectUri: String = "http://127.0.0.1:8080/callback"

    /**
     * Start the Spotify login process. Returns the URL to open in the browser.
     */


    fun importYouTubePlaylists(token: String? = null) {
        viewModelScope.launch {
            try {
                _importProgress.value = ImportProgress(0, 100, "Fetching YouTube Playlists...")
                val res = musicRepository.fetchUserPlaylists(token)
                val ytPlaylists = res.getOrThrow()
                
                if (ytPlaylists.isEmpty()) {
                    _importProgress.value = null
                    return@launch
                }
                
                val totalPlaylists = ytPlaylists.size
                var importedPlaylistsCount = 0
                
                val newPlaylists = ytPlaylists.mapNotNull { ytPlaylist ->
                    try {
                        _importProgress.value = ImportProgress(importedPlaylistsCount, totalPlaylists, ytPlaylist.title)
                        val tracks = musicRepository.getYouTubePlaylistTracks(ytPlaylist.id, token).getOrThrow()
                        
                        val currentStateLocal = _uiState.value
                        val existingYoutubeList = (currentStateLocal as? LibraryUiState.Success)?.youtubePlaylists ?: emptyList()
                        val ytPlaylistId = "yt_${ytPlaylist.id}"
                        val existingPlaylist = existingYoutubeList.find { it.id == ytPlaylistId }
                        val existingTracks = existingPlaylist?.tracks ?: emptyList()
                        val existingTrackIds = existingTracks.map { it.id }.toSet()
                        val newTracks = tracks.filter { it.id !in existingTrackIds }
                        val finalTracks = existingTracks + newTracks

                        importedPlaylistsCount++
                        Playlist(
                            id = ytPlaylistId,
                            title = ytPlaylist.title,
                            description = "Imported from YouTube Music",
                            thumbnailUrl = ytPlaylist.thumbnailUrl ?: finalTracks.firstOrNull()?.thumbnailUrl,
                            trackCount = finalTracks.size,
                            tracks = finalTracks,
                            source = PlaylistSource.YOUTUBE
                        )
                    } catch (e: Exception) {
                        println("Failed to import YouTube playlist ${ytPlaylist.title}: ${e.message}")
                        null
                    }
                }
                
                val currentState = _uiState.value
                val localList = (currentState as? LibraryUiState.Success)?.localPlaylists ?: emptyList()
                val spotifyList = (currentState as? LibraryUiState.Success)?.spotifyPlaylists ?: emptyList()
                val youtubeList = (currentState as? LibraryUiState.Success)?.youtubePlaylists ?: emptyList()
                val likedList = (currentState as? LibraryUiState.Success)?.likedTracks ?: emptyList()
                
                val updatedYoutubePlaylistsMap = youtubeList.associateBy { it.id }.toMutableMap()
                newPlaylists.forEach { p -> updatedYoutubePlaylistsMap[p.id] = p }
                val updatedYoutube = updatedYoutubePlaylistsMap.values.toList()
                
                _uiState.value = LibraryUiState.Success(
                    localPlaylists = localList,
                    youtubePlaylists = updatedYoutube,
                    spotifyPlaylists = spotifyList,
                    likedTracks = likedList
                )
                LibraryStorage.saveLibrary(spotifyList, updatedYoutube, localList, likedList)
                _importProgress.value = null
            } catch (e: Exception) {
                _importProgress.value = null
                _uiState.value = LibraryUiState.Error("Failed to import YouTube playlists: ${e.message}")
            }
        }
    }

    fun importPlaylistFromLink(link: String) {
        viewModelScope.launch {
            try {
                if (link.contains("youtube.com") || link.contains("youtu.be")) {
                    val listMatch = Regex("list=([a-zA-Z0-9_-]+)").find(link)
                    val playlistId = listMatch?.groupValues?.get(1)
                        ?: throw IllegalArgumentException("Invalid YouTube playlist link (must contain list=...)")

                    _importProgress.value = ImportProgress(0, 1, "YouTube Playlist")
                    val tracks = musicRepository.getYouTubePlaylistTracks(playlistId).getOrThrow()
                    if (tracks.isEmpty()) {
                        throw Exception("No tracks found in YouTube playlist or playlist is private")
                    }

                    val newPlaylist = Playlist(
                        id = "yt_$playlistId",
                        title = "YouTube Playlist ($playlistId)",
                        description = "Imported from YouTube Music",
                        thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl,
                        trackCount = tracks.size,
                        tracks = tracks
                    )

                    val currentStorage = LibraryStorage.loadLibrary()
                    val updatedYoutubePlaylistsMap = currentStorage.youtubePlaylists.associateBy { it.id }.toMutableMap()
                    updatedYoutubePlaylistsMap[newPlaylist.id] = newPlaylist
                    val updatedYoutube = updatedYoutubePlaylistsMap.values.toList()
                    LibraryStorage.saveLibrary(
                        youtubePlaylists = updatedYoutube,
                        localPlaylists = currentStorage.localPlaylists,
                        spotifyPlaylists = currentStorage.spotifyPlaylists,
                        likedTracks = currentStorage.likedTracks
                    )
                    _importProgress.value = null
                    _uiState.value = LibraryUiState.Success(
                        localPlaylists = currentStorage.localPlaylists,
                        youtubePlaylists = updatedYoutube,
                        spotifyPlaylists = currentStorage.spotifyPlaylists,
                        likedTracks = currentStorage.likedTracks
                    )
                    return@launch
                }

                // Extract ID from link, e.g. https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=...
                val idMatch = Regex("playlist/([a-zA-Z0-9]+)").find(link)
                val playlistId = idMatch?.groupValues?.get(1)
                    ?: throw IllegalArgumentException("Invalid Spotify or YouTube playlist link")

                // Use the official Spotify API directly
                val (spotifyPlaylist, rawTracks) = try {
                    spotifyApi.getPlaylist(playlistId)
                } catch (e: Exception) {
                    throw Exception("Failed to fetch Spotify playlist: ${e.message}")
                }

                val playlistName = spotifyPlaylist.name
                val totalTracks = rawTracks.size
                var importedTracks = 0

                _importProgress.value = ImportProgress(0, totalTracks, playlistName)

                // Filter out local files and null tracks (they have empty id or null track)
                val validRawTracks = rawTracks.filter { it.id?.isNotBlank() == true && it.name?.isNotBlank() == true }

                // Map Spotify tracks directly without blocking on YouTube searches
                val tracks = validRawTracks.mapIndexed { index, spotifyTrack ->
                    importedTracks++
                    _importProgress.value = ImportProgress(
                        imported = importedTracks,
                        total = totalTracks,
                        currentPlaylist = playlistName,
                        currentTrack = spotifyTrack.name ?: "Unknown Track"
                    )

                    val mappedTrack = Track(
                        id = if (spotifyTrack.id?.isNotBlank() == true) spotifyTrack.id else "sp_${playlistId}_$index",
                        title = spotifyTrack.name ?: "Unknown Track",
                        artists = spotifyTrack.artists?.mapNotNull { it }?.map { com.melodify.shared.domain.model.Artist(it.id ?: "", it.name ?: "Unknown") } ?: emptyList(),
                        album = spotifyTrack.album?.let { com.melodify.shared.domain.model.Album(it.id ?: "", it.name ?: "", it.images?.firstOrNull()?.url) },
                        thumbnailUrl = spotifyTrack.album?.images?.firstOrNull()?.url ?: spotifyPlaylist.images.firstOrNull()?.url,
                        durationMs = spotifyTrack.durationMs ?: 0L,
                        source = com.melodify.shared.domain.model.TrackSource.SPOTIFY,
                        spotifyId = spotifyTrack.id
                    )
                    musicRepository.enhanceTrack(mappedTrack)
                }

                val newPlaylist = Playlist(
                    id = playlistId,
                    title = playlistName,
                    description = spotifyPlaylist.description,
                    thumbnailUrl = spotifyPlaylist.images.firstOrNull()?.url ?: tracks.firstOrNull()?.thumbnailUrl,
                    trackCount = tracks.size,
                    tracks = tracks,
                    source = PlaylistSource.SPOTIFY,
                    spotifyId = playlistId
                )

                _importProgress.value = null
                val currentState = _uiState.value
                val existingPlaylists = if (currentState is LibraryUiState.Success) currentState.spotifyPlaylists else emptyList()
                val localList = if (currentState is LibraryUiState.Success) currentState.localPlaylists else emptyList()
                val likedList = if (currentState is LibraryUiState.Success) currentState.likedTracks else emptyList()
                val updatedSpotifyPlaylistsMap = existingPlaylists.associateBy { it.id }.toMutableMap()
                updatedSpotifyPlaylistsMap[newPlaylist.id] = newPlaylist
                val updatedSpotify = updatedSpotifyPlaylistsMap.values.toList()

                _uiState.value = LibraryUiState.Success(
                    localPlaylists = localList,
                    spotifyPlaylists = updatedSpotify,
                    likedTracks = likedList,
                )
                LibraryStorage.saveLibrary(updatedSpotify, localList, likedList)


            } catch (e: Exception) {
                _importProgress.value = null
                _uiState.value = LibraryUiState.Error("Failed to import playlist: ${e.message}")
            }
        }
    }

    fun importFromCsv(csvContent: String) {
        viewModelScope.launch {
            try {
                val lines = csvContent.lines().filter { it.isNotBlank() }
                if (lines.size <= 1) throw Exception("CSV file is empty or invalid")

                // Skip header (first line)
                val trackLines = lines.drop(1)
                val totalTracks = trackLines.size
                var importedTracksCount = 0

                // Group tracks by Playlist name (column index 3)
                val playlistsMap = mutableMapOf<String, MutableList<Track>>()

                // Use Regex to split by comma, ignoring commas inside quotes
                val csvRegex = Regex(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)")

                trackLines.forEachIndexed { index, line ->
                    importedTracksCount++
                    val columns = line.split(csvRegex).map { it.trim().removeSurrounding("\"") }
                    
                    if (columns.size >= 4) {
                        val trackName = columns[0]
                        val artistName = columns[1]
                        val albumName = columns[2]
                        val playlistName = columns[3]
                        val spotifyId = if (columns.size > 6) columns[6] else ""
                        
                        _importProgress.value = ImportProgress(
                            imported = importedTracksCount,
                            total = totalTracks,
                            currentPlaylist = playlistName,
                            currentTrack = trackName
                        )

                        val mappedTrack = Track(
                            id = if (spotifyId.isNotBlank()) spotifyId else "csv_${playlistName}_$index",
                            title = trackName,
                            artists = listOf(com.melodify.shared.domain.model.Artist(id = "", name = artistName)),
                            album = com.melodify.shared.domain.model.Album(id = "", title = albumName, thumbnailUrl = null),
                            thumbnailUrl = null,
                            durationMs = 0L,
                            source = com.melodify.shared.domain.model.TrackSource.SPOTIFY,
                            spotifyId = if (spotifyId.isNotBlank()) spotifyId else null
                        )
                        kotlinx.coroutines.delay(300) // Prevent Deezer API rate limits
                        val enhancedTrack = musicRepository.enhanceTrack(mappedTrack)
                        
                        if (!playlistsMap.containsKey(playlistName)) {
                            playlistsMap[playlistName] = mutableListOf()
                        }
                        playlistsMap[playlistName]?.add(enhancedTrack)
                    }
                }

                _importProgress.value = null
                
                val currentState = _uiState.value
                val existingPlaylists = if (currentState is LibraryUiState.Success) currentState.spotifyPlaylists else emptyList()
                val localList = if (currentState is LibraryUiState.Success) currentState.localPlaylists else emptyList()
                val likedList = if (currentState is LibraryUiState.Success) currentState.likedTracks else emptyList()
                val updatedSpotifyPlaylistsMap = existingPlaylists.associateBy { it.id }.toMutableMap()

                playlistsMap.forEach { (playlistName, tracks) ->
                    val newPlaylist = Playlist(
                        id = "csv_${playlistName.replace(Regex("[^A-Za-z0-9]"), "_")}",
                        title = playlistName,
                        description = "Imported from TuneMyMusic CSV",
                        thumbnailUrl = tracks.firstOrNull()?.thumbnailUrl,
                        trackCount = tracks.size,
                        tracks = tracks,
                        source = PlaylistSource.SPOTIFY,
                        spotifyId = null
                    )
                    updatedSpotifyPlaylistsMap[newPlaylist.id] = newPlaylist
                }
                
                val updatedSpotify = updatedSpotifyPlaylistsMap.values.toList()

                _uiState.value = LibraryUiState.Success(
                    localPlaylists = localList,
                    spotifyPlaylists = updatedSpotify,
                    likedTracks = likedList,
                )
                LibraryStorage.saveLibrary(
                    spotifyPlaylists = updatedSpotify,
                    localPlaylists = localList,
                    likedTracks = likedList
                )

            } catch (e: Exception) {
                _importProgress.value = null
                _uiState.value = LibraryUiState.Error("Failed to import from CSV: ${e.message}")
            }
        }
    }

    fun toggleLike(track: Track) {
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            val isLiked = currentState.likedTracks.any { it.id == track.id }
            val newLikedTracks = if (isLiked) {
                currentState.likedTracks.filter { it.id != track.id }
            } else {
                currentState.likedTracks + track
            }
            
            _uiState.value = currentState.copy(likedTracks = newLikedTracks)
            LibraryStorage.saveLibrary(
                currentState.spotifyPlaylists,
                currentState.localPlaylists,
                newLikedTracks
            )
        }
    }

    fun createPlaylist(title: String) {
        viewModelScope.launch {
            // Create local playlist in DB
        }
    }
}

sealed class LibraryUiState {
    object Loading : LibraryUiState()
    data class Success(
        val localPlaylists: List<Playlist>,
        val spotifyPlaylists: List<Playlist>,
        val youtubePlaylists: List<Playlist> = emptyList(),
        val likedTracks: List<Track>,
        val jellyfinTracks: List<Track> = emptyList(),
    ) : LibraryUiState()
    data class Error(val message: String) : LibraryUiState()
}

data class ImportProgress(
    val imported: Int,
    val total: Int,
    val currentPlaylist: String = "",
    val currentTrack: String = "",
) {
    val percentage: Float get() = if (total > 0) imported.toFloat() / total.toFloat() else 0f
}

data class LocalTrackMetadata(
    val path: String,
    val title: String,
    val artist: String,
    val album: String?,
    val durationMs: Long,
    val artPath: String?
)

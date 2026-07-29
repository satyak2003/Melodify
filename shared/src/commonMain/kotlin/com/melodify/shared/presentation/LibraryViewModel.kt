package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.api.spotify.SpotifyApi
import com.melodify.shared.api.spotify.SpotifyAuthHelper
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.PlaylistSource
import com.melodify.shared.domain.model.Track
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
import com.melodify.shared.api.spotify.SpotifyTokenStorage
import com.melodify.shared.data.storage.LibraryStorage

class LibraryViewModel(
    private val musicRepository: MusicRepository,
    private val spotifyApi: SpotifyApi,
) : ViewModel() {


    private val _uiState = MutableStateFlow<LibraryUiState>(LibraryUiState.Loading)
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _isSpotifyConnected = MutableStateFlow(false)
    val isSpotifyConnected: StateFlow<Boolean> = _isSpotifyConnected.asStateFlow()

    private val _importProgress = MutableStateFlow<ImportProgress?>(null)
    val importProgress: StateFlow<ImportProgress?> = _importProgress.asStateFlow()

    private var currentCodeVerifier: String? = null

    init {
        loadLibrary()
        restoreSpotifySession()
        
        // Observe DeepLinkHandler for incoming OAuth callbacks (Android)
        viewModelScope.launch {
            DeepLinkHandler.deepLinks.collectLatest { url ->
                if (url.startsWith(SpotifyAuthHelper.REDIRECT_URI)) {
                    onSpotifyAuthCallback(url)
                }
            }
        }
    }

    private fun restoreSpotifySession() {
        viewModelScope.launch {
            val session = SpotifyTokenStorage.loadToken()
            val savedAccess = session.accessToken
            val savedRefresh = session.refreshToken

            if (!savedAccess.isNullOrBlank()) {
                spotifyApi.accessToken = savedAccess
                _isSpotifyConnected.value = true

                if (!savedRefresh.isNullOrBlank()) {
                    try {
                        val newToken = spotifyApi.refreshToken(savedRefresh, SpotifyAuthHelper.CLIENT_ID)
                        spotifyApi.accessToken = newToken.accessToken
                        val newRefresh = newToken.refreshToken ?: savedRefresh
                        SpotifyTokenStorage.saveToken(newToken.accessToken, newRefresh)
                    } catch (e: Exception) {
                        println("Spotify token refresh info: ${e.message}, using stored access token")
                    }
                }

                // If stored library is empty, trigger initial import
                val storedData = LibraryStorage.loadLibrary()
                if (storedData.spotifyPlaylists.isEmpty() && storedData.likedTracks.isEmpty()) {
                    importSpotifyPlaylists()
                }
            }
        }
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = LibraryUiState.Loading
            try {
                val stored = LibraryStorage.loadLibrary()
                _uiState.value = LibraryUiState.Success(
                    localPlaylists = stored.localPlaylists,
                    spotifyPlaylists = stored.spotifyPlaylists,
                    likedTracks = stored.likedTracks,
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
            source = PlaylistSource.LOCAL
        )

        val updatedLocal = state.localPlaylists + newPlaylist
        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
    }

    fun addTrackToPlaylist(playlistId: String, track: Track) {
        val state = _uiState.value
        if (state !is LibraryUiState.Success) return

        val updatedLocal = state.localPlaylists.map { playlist ->
            if (playlist.id == playlistId) {
                if (playlist.tracks.any { it.id == track.id }) playlist
                else playlist.copy(
                    tracks = playlist.tracks + track,
                    trackCount = playlist.tracks.size + 1
                )
            } else playlist
        }

        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
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

        _uiState.value = state.copy(localPlaylists = updatedLocal)
        LibraryStorage.saveLibrary(state.spotifyPlaylists, updatedLocal, state.likedTracks)
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
                source = PlaylistSource.LOCAL
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
    fun startSpotifyLogin(redirectUri: String = "http://127.0.0.1:8080/callback"): String {
        val verifier = SpotifyAuthHelper.generateCodeVerifier()
        currentCodeVerifier = verifier
        currentRedirectUri = redirectUri
        SpotifyTokenStorage.saveCodeVerifier(verifier)
        SpotifyTokenStorage.saveRedirectUri(redirectUri)

        if (redirectUri.startsWith("http://127.0.0.1")) {
            startLocalCallbackServer()
        }

        return SpotifyAuthHelper.buildAuthUrl(verifier, redirectUri)
    }

    private fun startLocalCallbackServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                ServerSocket(8080).use { server ->
                    server.soTimeout = 60000 // 1 minute timeout
                    val socket = server.accept()
                    val reader = BufferedReader(InputStreamReader(socket.inputStream))
                    val requestLine = reader.readLine()

                    if (requestLine != null && requestLine.startsWith("GET /callback?code=")) {
                        val path = requestLine.split(" ")[1]
                        val fullUrl = "http://127.0.0.1:8080$path"

                        val response = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><body><h2>Spotify Login Successful!</h2><p>You can close this tab and return to Melodify.</p><script>window.close()</script></body></html>"
                        socket.getOutputStream().write(response.toByteArray())
                        socket.getOutputStream().flush()

                        withContext(Dispatchers.Main) {
                            onSpotifyAuthCallback(fullUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                println("Local server failed or timed out: ${e.message}")
            }
        }
    }

    /**
     * Called when user completes Spotify OAuth flow on Android via deep link.
     */
    fun handleSpotifyAuthCode(code: String) {
        val codeVerifier = currentCodeVerifier ?: SpotifyTokenStorage.loadCodeVerifier()
        val redirectUri = SpotifyTokenStorage.loadRedirectUri() ?: currentRedirectUri
        if (codeVerifier == null) {
            _uiState.value = LibraryUiState.Error("Spotify verifier missing. Please try logging in again.")
            return
        }

        viewModelScope.launch {
            try {
                val token = spotifyApi.exchangeCodeForToken(
                    code = code,
                    codeVerifier = codeVerifier,
                    redirectUri = redirectUri,
                    clientId = SpotifyAuthHelper.CLIENT_ID
                )

                spotifyApi.accessToken = token.accessToken
                SpotifyTokenStorage.saveToken(token.accessToken, token.refreshToken)
                _isSpotifyConnected.value = true
                currentCodeVerifier = null
                importSpotifyPlaylists()
            } catch (e: Exception) {
                _uiState.value = LibraryUiState.Error("Spotify login failed: ${e.message}")
            }
        }
    }

    /**
     * Called when user completes Spotify OAuth flow via full redirect URL.
     */
    fun onSpotifyAuthCallback(redirectUrl: String) {
        val code = SpotifyAuthHelper.parseAuthCode(redirectUrl)
        if (code == null) {
            _uiState.value = LibraryUiState.Error("Could not parse auth code from URL.")
            return
        }
        handleSpotifyAuthCode(code)
    }



    fun logoutSpotify() {
        spotifyApi.accessToken = null
        SpotifyTokenStorage.clearToken()
        _isSpotifyConnected.value = false
        val currentState = _uiState.value
        if (currentState is LibraryUiState.Success) {
            _uiState.value = currentState.copy(spotifyPlaylists = emptyList(), likedTracks = emptyList())
        }
    }


    /**
     * Import all playlists from Spotify.
     * Shows progress and matches each track to YouTube.
     */
    fun importSpotifyPlaylists() {
        viewModelScope.launch {
            try {
                val playlists = spotifyApi.getUserPlaylists()
                val totalTracks = playlists.sumOf { it.tracksInfo?.total ?: 0 }
                var importedTracks = 0

                _importProgress.value = ImportProgress(0, totalTracks, "Fetching playlists...")

                val importedPlaylists = playlists.mapNotNull { spotifyPlaylist ->
                    try {
                        _importProgress.value = ImportProgress(
                            imported = importedTracks,
                            total = totalTracks,
                            currentPlaylist = spotifyPlaylist.name
                        )

                        val rawTracks = try {
                            spotifyApi.getAllPlaylistTracks(spotifyPlaylist.id)
                        } catch (e: Exception) {
                            spotifyPlaylist.tracksInfo?.items?.mapNotNull { it.track } ?: emptyList()
                        }

                        // Filter out local files (empty id) and null tracks
                        val validRawTracks = rawTracks.filter { it.name.isNotBlank() }

                        val tracks = validRawTracks.mapIndexed { index, spotifyTrack ->
                            importedTracks++
                            _importProgress.value = ImportProgress(
                                imported = importedTracks,
                                total = totalTracks,
                                currentPlaylist = spotifyPlaylist.name,
                                currentTrack = spotifyTrack.name
                            )

                            Track(
                                id = if (spotifyTrack.id.isNotBlank()) spotifyTrack.id else "sp_${spotifyPlaylist.id}_$index",
                                title = spotifyTrack.name,
                                artists = spotifyTrack.artists.map { com.melodify.shared.domain.model.Artist(it.id, it.name) },
                                album = spotifyTrack.album?.let { com.melodify.shared.domain.model.Album(it.id, it.name, it.images.firstOrNull()?.url) },
                                thumbnailUrl = spotifyTrack.album?.images?.firstOrNull()?.url ?: spotifyPlaylist.images.firstOrNull()?.url,
                                durationMs = spotifyTrack.durationMs,
                                source = com.melodify.shared.domain.model.TrackSource.SPOTIFY,
                                spotifyId = spotifyTrack.id
                            )
                        }

                        Playlist(
                            id = spotifyPlaylist.id,
                            title = spotifyPlaylist.name,
                            description = spotifyPlaylist.description,
                            thumbnailUrl = spotifyPlaylist.images.firstOrNull()?.url,
                            trackCount = tracks.size,
                            tracks = tracks,
                            source = PlaylistSource.SPOTIFY,
                            spotifyId = spotifyPlaylist.id
                        )
                    } catch (e: Exception) {
                        println("Skipping playlist ${spotifyPlaylist.name} due to error: ${e.message}")
                        null
                    }
                }


                _importProgress.value = null
                val currentState = _uiState.value
                val localList = (currentState as? LibraryUiState.Success)?.localPlaylists ?: emptyList()
                val likedList = (currentState as? LibraryUiState.Success)?.likedTracks ?: emptyList()
                
                _uiState.value = LibraryUiState.Success(
                    localPlaylists = localList,
                    spotifyPlaylists = importedPlaylists,
                    likedTracks = likedList,
                )
                LibraryStorage.saveLibrary(importedPlaylists, localList, likedList)

            } catch (e: Exception) {
                _importProgress.value = null
                _uiState.value = LibraryUiState.Error("Failed to import Spotify playlists: ${e.message}")
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
                    val updatedLocal = currentStorage.localPlaylists + newPlaylist
                    LibraryStorage.saveLibrary(
                        localPlaylists = updatedLocal,
                        spotifyPlaylists = currentStorage.spotifyPlaylists,
                        likedTracks = currentStorage.likedTracks
                    )
                    _importProgress.value = null
                    _uiState.value = LibraryUiState.Success(
                        localPlaylists = updatedLocal,
                        spotifyPlaylists = currentStorage.spotifyPlaylists,
                        likedTracks = currentStorage.likedTracks
                    )
                    return@launch
                }

                // Extract ID from link, e.g. https://open.spotify.com/playlist/37i9dQZF1DXcBWIGoYBM5M?si=...
                val idMatch = Regex("playlist/([a-zA-Z0-9]+)").find(link)
                val playlistId = idMatch?.groupValues?.get(1)
                    ?: throw IllegalArgumentException("Invalid Spotify or YouTube playlist link")

                val spotifyPlaylist = spotifyApi.getPlaylist(playlistId)
                val totalTracks = spotifyPlaylist.tracksInfo?.total ?: 0
                var importedTracks = 0

                _importProgress.value = ImportProgress(0, totalTracks, spotifyPlaylist.name)

                val rawTracks = try {
                    spotifyApi.getAllPlaylistTracks(spotifyPlaylist.id)
                } catch (e: Exception) {
                    spotifyPlaylist.tracksInfo?.items?.mapNotNull { it.track } ?: emptyList()
                }

                // Filter out local files and null tracks (they have empty id or null track)
                val validRawTracks = rawTracks.filter { it.id.isNotBlank() && it.name.isNotBlank() }

                // Map Spotify tracks directly without blocking on YouTube searches
                val tracks = validRawTracks.mapIndexed { index, spotifyTrack ->
                    importedTracks++
                    _importProgress.value = ImportProgress(
                        imported = importedTracks,
                        total = totalTracks,
                        currentPlaylist = spotifyPlaylist.name,
                        currentTrack = spotifyTrack.name
                    )

                    Track(
                        id = if (spotifyTrack.id.isNotBlank()) spotifyTrack.id else "sp_${spotifyPlaylist.id}_$index",
                        title = spotifyTrack.name,
                        artists = spotifyTrack.artists.map { com.melodify.shared.domain.model.Artist(it.id, it.name) },
                        album = spotifyTrack.album?.let { com.melodify.shared.domain.model.Album(it.id, it.name, it.images.firstOrNull()?.url) },
                        thumbnailUrl = spotifyTrack.album?.images?.firstOrNull()?.url ?: spotifyPlaylist.images.firstOrNull()?.url,
                        durationMs = spotifyTrack.durationMs,
                        source = com.melodify.shared.domain.model.TrackSource.SPOTIFY,
                        spotifyId = spotifyTrack.id
                    )
                }

                val newPlaylist = Playlist(
                    id = spotifyPlaylist.id,
                    title = spotifyPlaylist.name,
                    description = spotifyPlaylist.description,
                    thumbnailUrl = spotifyPlaylist.images.firstOrNull()?.url,
                    trackCount = tracks.size,
                    tracks = tracks,
                    source = PlaylistSource.SPOTIFY,
                    spotifyId = spotifyPlaylist.id
                )

                _importProgress.value = null
                val currentState = _uiState.value
                val existingPlaylists = if (currentState is LibraryUiState.Success) currentState.spotifyPlaylists else emptyList()
                val localList = if (currentState is LibraryUiState.Success) currentState.localPlaylists else emptyList()
                val likedList = if (currentState is LibraryUiState.Success) currentState.likedTracks else emptyList()
                val updatedSpotify = existingPlaylists + newPlaylist

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
        val likedTracks: List<Track>,
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

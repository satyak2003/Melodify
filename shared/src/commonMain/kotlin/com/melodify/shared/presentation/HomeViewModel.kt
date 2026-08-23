package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.api.radio.FmStation
import com.melodify.shared.data.storage.LastPlayedStorage
import com.melodify.shared.data.storage.LibraryStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val jumpBackInTracks: List<Track> = emptyList(),
        val lastPlayedTrack: Track? = null,
        val lastPlayedPositionMs: Long = 0L,
        val lastPlayedDurationMs: Long = 0L,
        val recommendedTracks: List<Track> = emptyList(),
        val mostPlayedTracks: List<Track> = emptyList(),
        val moods: List<String> = listOf("Chill", "Happy", "Focus", "Party", "Workout", "Sleep"),
        val offlineTracks: List<Track> = emptyList(),
        val userPlaylists: List<Playlist> = emptyList(),
        val fmStations: List<FmStation> = emptyList(),
        val selectedMood: String? = null,
        val selectedMoodTracks: List<Track>? = null,
        val selectedFmCountry: String? = null
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val musicRepository: MusicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }


    
    fun loadFmStationsForCountry(countryCode: String?) {
        val state = _uiState.value as? HomeUiState.Success ?: return
        if (state.selectedFmCountry == countryCode) return

        _uiState.value = state.copy(selectedFmCountry = countryCode)
        viewModelScope.launch {
            val stations = musicRepository.getTopFmStations(10, countryCode).getOrDefault(emptyList())
            val currentState = _uiState.value as? HomeUiState.Success ?: return@launch
            _uiState.value = currentState.copy(fmStations = stations)
        }
    }

    fun loadMoodTracks(mood: String) {
        val state = _uiState.value as? HomeUiState.Success ?: return
        if (state.selectedMood == mood) {
            // Toggle off
            _uiState.value = state.copy(selectedMood = null, selectedMoodTracks = null)
            return
        }
        _uiState.value = state.copy(selectedMood = mood, selectedMoodTracks = null) // Loading state
        
        viewModelScope.launch {
            val tracks = musicRepository.getMoodTracks(mood, 15).getOrDefault(emptyList())
            val currentState = _uiState.value as? HomeUiState.Success ?: return@launch
            if (currentState.selectedMood == mood) {
                _uiState.value = currentState.copy(selectedMoodTracks = tracks)
            }
        }
    }

    fun refresh() {
        loadHome()
    }

    fun refreshLastPlayed() {
        val state = _uiState.value as? HomeUiState.Success ?: return
        val lastPlayed = LastPlayedStorage.loadLastPlayed()
        _uiState.value = state.copy(
            lastPlayedTrack = lastPlayed?.currentTrack,
            lastPlayedPositionMs = lastPlayed?.positionMs ?: 0L,
            lastPlayedDurationMs = lastPlayed?.durationMs ?: 0L
        )
    }

    
    suspend fun getMoodTracks(mood: String): List<Track> {
        return musicRepository.getMoodTracks(mood, 15).getOrDefault(emptyList())
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            
            try {
                val lastPlayed = LastPlayedStorage.loadLastPlayed()
                val recentTracksAsync = async<List<Track>> { musicRepository.getRecentTracks(4) }
                val mostPlayedAsync = async<List<Track>> { musicRepository.getMostPlayedTracks(10) }
                val fmStationsAsync = async<List<FmStation>> { musicRepository.getTopFmStations(10).getOrDefault(emptyList()) }
                
                // Get recommendations based on last played or a default search
                val recommendationsAsync = async<List<Track>> {
                    if (lastPlayed?.currentTrack != null) {
                        val track = lastPlayed.currentTrack
                        musicRepository.getRecommendations(track.artists.firstOrNull()?.name ?: "", track.title).getOrDefault(emptyList())
                    } else {
                        musicRepository.getHomeFeed().getOrDefault(emptyList())
                    }
                }
                
                val recentTracks = recentTracksAsync.await()
                val mostPlayed = mostPlayedAsync.await()
                val fmStations = fmStationsAsync.await()
                val recommendations = recommendationsAsync.await()

                val library = LibraryStorage.loadLibrary()
                val localFilesPlaylist = library.localPlaylists.firstOrNull { it.id == "local_music_files" }
                val offlineTracks = (localFilesPlaylist?.tracks ?: emptyList()) + library.downloadedTracks
                val userPlaylists = library.localPlaylists.filter { it.id != "local_music_files" }

                _uiState.value = HomeUiState.Success(
                    jumpBackInTracks = recentTracks,
                    lastPlayedTrack = lastPlayed?.currentTrack,
                    lastPlayedPositionMs = lastPlayed?.positionMs ?: 0L,
                    lastPlayedDurationMs = lastPlayed?.durationMs ?: 0L,
                    recommendedTracks = recommendations,
                    mostPlayedTracks = mostPlayed,
                    offlineTracks = offlineTracks,
                    userPlaylists = userPlaylists,
                    fmStations = fmStations
                )
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Failed to load home feed")
            }
        }
    }
}

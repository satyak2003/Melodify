package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import com.melodify.shared.data.storage.LastPlayedStorage
import com.melodify.shared.data.storage.ListeningStatsStorage


sealed class HomeUiState {
    data object Loading : HomeUiState()
    data class Success(
        val trending: List<Track>,
        val recentlyPlayed: List<Track> = emptyList(),
        val lastPlayedTrack: Track? = null,
        val lastPlayedPositionMs: Long = 0L,
        val lastPlayedDurationMs: Long = 0L,
        val weeklyStats: Map<String, Int> = emptyMap(),
        val recommendedTracks: List<Track> = emptyList()
    ) : HomeUiState()

    data class Error(val message: String) : HomeUiState()
}

class HomeViewModel(private val musicRepository: MusicRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHome()
    }

    fun refresh() {
        loadHome()
    }

    /** Refreshes the resume card without making another network request. */
    fun refreshLastPlayed() {
        val state = _uiState.value as? HomeUiState.Success ?: return
        val lastPlayed = LastPlayedStorage.loadLastPlayed()
        _uiState.value = state.copy(
            lastPlayedTrack = lastPlayed?.currentTrack,
            lastPlayedPositionMs = lastPlayed?.positionMs ?: 0L,
            lastPlayedDurationMs = lastPlayed?.durationMs ?: 0L
        )
    }

    private fun loadHome() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            val lastPlayed = LastPlayedStorage.loadLastPlayed()
            val stats = ListeningStatsStorage.getWeeklyMinutesMap()

            musicRepository.getHomeFeed()
                .onSuccess { tracks ->
                    if (tracks.isNotEmpty()) {
                        _uiState.value = HomeUiState.Success(
                            trending = tracks,
                            lastPlayedTrack = lastPlayed?.currentTrack,
                            lastPlayedPositionMs = lastPlayed?.positionMs ?: 0L,
                            lastPlayedDurationMs = lastPlayed?.durationMs ?: 0L,
                            weeklyStats = stats,
                            recommendedTracks = com.melodify.shared.domain.RecommendationEngine.getRecommendations(count = 20)
                        )
                    } else {
                        fetchFallbackTrending(lastPlayed, stats)
                    }
                }
                .onFailure {
                    fetchFallbackTrending(lastPlayed, stats)
                }
        }
    }

    private suspend fun fetchFallbackTrending(
        lastPlayed: com.melodify.shared.data.storage.StoredLastPlayed? = null,
        stats: Map<String, Int> = emptyMap()
    ) {
        musicRepository.search("Top Hits Trending")
            .onSuccess { result ->
                _uiState.value = HomeUiState.Success(
                    trending = result.tracks,
                    lastPlayedTrack = lastPlayed?.currentTrack,
                    lastPlayedPositionMs = lastPlayed?.positionMs ?: 0L,
                    lastPlayedDurationMs = lastPlayed?.durationMs ?: 0L,
                    weeklyStats = stats,
                    recommendedTracks = com.melodify.shared.domain.RecommendationEngine.getRecommendations(count = 20)
                )
            }
            .onFailure { e ->
                val msg = when {
                    e is java.net.UnknownHostException || e is java.net.ConnectException || e.message?.contains("Unable to resolve host") == true -> "You're offline — listen to your downloaded music!"
                    e is java.io.IOException -> "Connection lost. Check your network and try again."
                    else -> "Something went wrong. Tap retry to try again."
                }
                _uiState.value = HomeUiState.Error(msg)
            }
    }



}

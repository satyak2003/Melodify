package com.melodify.shared.domain.download

import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class DownloadQuality {
    NORMAL, FLAC
}

sealed class DownloadState {
    object Queued : DownloadState()
    data class Downloading(val progress: Float) : DownloadState()
    object Paused : DownloadState()
    object Completed : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

data class DownloadItem(
    val id: String,
    val track: Track,
    val quality: DownloadQuality,
    val state: DownloadState
)

object DownloadQueueManager {
    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    fun enqueue(track: Track, quality: DownloadQuality) {
        val id = "${track.id}_${quality.name}_${System.currentTimeMillis()}"
        val item = DownloadItem(id, track, quality, DownloadState.Queued)
        _downloads.update { it + item }
    }

    fun updateState(id: String, state: DownloadState) {
        _downloads.update { list ->
            list.map { if (it.id == id) it.copy(state = state) else it }
        }
    }

    fun pause(id: String) {
        updateState(id, DownloadState.Paused)
    }

    fun resume(id: String) {
        updateState(id, DownloadState.Queued)
    }

    fun remove(id: String) {
        _downloads.update { list -> list.filter { it.id != id } }
    }
}

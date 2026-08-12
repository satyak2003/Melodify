package com.melodify.shared.presentation

import com.melodify.shared.data.MusicRepository
import com.melodify.shared.data.storage.TrackDownloader
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DownloadManager(
    private val scope: CoroutineScope,
    private val musicRepository: MusicRepository
) {
    private val _downloadingTracks = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingTracks: StateFlow<Map<String, Float>> = _downloadingTracks.asStateFlow()

    fun downloadTrack(track: Track) {
        if (_downloadingTracks.value.containsKey(track.id) || TrackDownloader.isDownloaded(track)) return
        scope.launch {
            _downloadingTracks.value = _downloadingTracks.value + (track.id to 0f)
            TrackDownloader.downloadTrack(track, musicRepository) { progress ->
                _downloadingTracks.value = _downloadingTracks.value + (track.id to progress)
            }
            _downloadingTracks.value = _downloadingTracks.value - track.id
        }
    }
}
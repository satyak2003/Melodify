package com.melodify.shared.domain.download

import com.melodify.shared.data.MusicRepository
import com.melodify.shared.data.storage.JellyfinSettings
import com.melodify.shared.data.network.jellyfin.JellyfinClient
import com.melodify.shared.data.storage.TrackDownloader
import io.ktor.client.HttpClient
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

object DownloadWorker {
    private var job: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    fun start(musicRepository: MusicRepository) {
        if (job != null) return
        job = scope.launch {
            DownloadQueueManager.downloads.collect { list ->
                val queued = list.filter { it.state is DownloadState.Queued }
                for (item in queued) {
                    // Mark it as downloading immediately to prevent picking it up again in the next emission
                    DownloadQueueManager.updateState(item.id, DownloadState.Downloading(0f))
                    
                    val downloadJob = launch {
                        processDownload(item, musicRepository)
                    }
                    activeJobs[item.id] = downloadJob
                }

                // Handle Paused state cancellations
                val paused = list.filter { it.state is DownloadState.Paused }
                for (item in paused) {
                    activeJobs[item.id]?.cancel()
                    activeJobs.remove(item.id)
                }
            }
        }
    }

    private suspend fun processDownload(item: DownloadItem, musicRepository: MusicRepository) {
        if (item.quality == DownloadQuality.NORMAL) {
            try {
                val result = TrackDownloader.downloadTrack(item.track, musicRepository) { progress ->
                    DownloadQueueManager.updateState(item.id, DownloadState.Downloading(progress))
                }
                if (result.isSuccess) {
                    DownloadQueueManager.updateState(item.id, DownloadState.Completed)
                    com.melodify.shared.data.storage.LibraryStorage.addDownloadedTrack(result.getOrNull() ?: item.track)
                } else {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException) throw error
                    DownloadQueueManager.updateState(item.id, DownloadState.Failed(error?.message ?: "Unknown Error"))
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                DownloadQueueManager.updateState(item.id, DownloadState.Failed(e.message ?: "Unknown Error"))
            } finally {
                activeJobs.remove(item.id)
            }
        } else {
            // FLAC Download via Jellyfin
            try {
                val url = JellyfinSettings.serverUrl.value
                val user = JellyfinSettings.username.value
                val pass = JellyfinSettings.password.value
                val token = JellyfinSettings.apiToken.value
                
                if (url.isBlank() || user.isBlank() || pass.isBlank() || token.isBlank()) {
                    throw Exception("Jellyfin credentials/token not configured in Settings.")
                }

                DownloadQueueManager.updateState(item.id, DownloadState.Downloading(0.1f))
                
                val client = JellyfinClient(HttpClient())
                val streamUrl = client.getStreamUrl(url, item.track.id, token)

                val result = TrackDownloader.downloadFlacFromUrl(item.track, streamUrl) { progress ->
                    DownloadQueueManager.updateState(item.id, DownloadState.Downloading(progress))
                }

                if (result.isSuccess) {
                    DownloadQueueManager.updateState(item.id, DownloadState.Completed)
                    com.melodify.shared.data.storage.LibraryStorage.addDownloadedTrack(result.getOrNull() ?: item.track)
                } else {
                    val error = result.exceptionOrNull()
                    if (error is CancellationException) throw error
                    throw Exception(error?.message ?: "Unknown Error during FLAC download")
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                DownloadQueueManager.updateState(item.id, DownloadState.Failed(e.message ?: "Jellyfin Error"))
            } finally {
                activeJobs.remove(item.id)
            }
        }
    }
}

package com.melodify.shared.presentation

import com.melodify.shared.data.MusicRepository
import com.melodify.shared.data.storage.TrackDownloader
import com.melodify.shared.domain.model.Queue
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.PlayerState
import com.melodify.shared.domain.player.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlaybackManager(
    private val scope: CoroutineScope,
    private val musicRepository: MusicRepository,
    private val audioPlayer: AudioPlayer,
    private val onPlaybackStarted: (Track) -> Unit,
    private val onPlaybackError: (String, Track?) -> Unit,
    private val onTrackEnded: () -> Unit,
    private val onBuffering: (Track?) -> Unit,
    private val onPositionUpdate: (Long, Long, Track) -> Unit,
    private val onPrefetchNext: () -> Unit
) {
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private var positionJob: Job? = null
    private var playbackRequestId = 0L
    private var consecutiveFailures = 0
    private var prefetchedUrl: String? = null
    private var prefetchedTrackId: String? = null

    fun playTrack(track: Track, initialSeekMs: Long = 0L, isAuto: Boolean = false) {
        if (!isAuto) {
            consecutiveFailures = 0
        }
        startPlayingTrack(track, initialSeekMs)
    }

    fun togglePlayPause(currentTrack: Track?, playerState: PlayerState, hasMedia: Boolean) {
        if (audioPlayer.isPlaying.value) {
            audioPlayer.pause()
        } else if (currentTrack != null) {
            if (playerState is PlayerState.Paused && hasMedia) {
                audioPlayer.resume()
            } else {
                consecutiveFailures = 0
                val savedPos = (playerState as? PlayerState.Paused)?.positionMs ?: 0L
                startPlayingTrack(currentTrack, savedPos)
            }
        }
    }

    fun seekTo(positionMs: Long) = audioPlayer.seekTo(positionMs)
    fun setVolume(volume: Float) = audioPlayer.setVolume(volume)
    fun pause() = audioPlayer.pause()
    fun resume() = audioPlayer.resume()

    private fun startPlayingTrack(track: Track, initialSeekMs: Long = 0L) {
        positionJob?.cancel()
        val requestId = ++playbackRequestId
        
        scope.launch {
            try {
                onBuffering(track)
                _currentTrack.value = track

                // Check for local downloaded copy first
                var activeTrack = track
                val url = if (prefetchedTrackId == track.id && prefetchedUrl != null) {
                    prefetchedUrl!!
                } else {
                    val downloadedPath = TrackDownloader.getDownloadedPath(track)
                    if (downloadedPath != null) {
                        downloadedPath
                    } else if (track.isLocal && track.localPath != null) {
                        track.localPath
                    } else {
                        val videoId = track.youtubeVideoId ?: track.id
                        val res = musicRepository.getStreamUrl(videoId)
                        if (res.isSuccess) {
                            res.getOrThrow()
                        } else {
                            // Retry matching if initial stream lookup failed
                            val matched = musicRepository.matchSpotifyTrack(track.title, track.artistNames, track.durationMs).getOrThrow()
                            activeTrack = track.copy(
                                youtubeVideoId = matched.youtubeVideoId ?: matched.id,
                                thumbnailUrl = track.thumbnailUrl ?: matched.thumbnailUrl
                            )
                            musicRepository.getStreamUrl(activeTrack.youtubeVideoId ?: activeTrack.id).getOrThrow()
                        }
                    }
                }

                // Clear prefetch
                prefetchedTrackId = null
                prefetchedUrl = null

                // A slower previous stream lookup must never replace the newly selected queue item
                if (requestId != playbackRequestId) return@launch

                // Successful stream resolution resets consecutive failure count
                consecutiveFailures = 0
                onBuffering(activeTrack)

                // Start playback
                audioPlayer.play(url, activeTrack, initialSeekMs)
                onPlaybackStarted(activeTrack)

                // Track position in a coroutine
                var tickCount = 0
                var prefetched = false
                positionJob = scope.launch {
                    while (isActive && requestId == playbackRequestId) {
                        val pos = audioPlayer.positionMs.value
                        val dur = audioPlayer.durationMs.value
                        val currentTrack = _currentTrack.value ?: activeTrack
                        if (audioPlayer.isPlaying.value) {
                            onPositionUpdate(pos, dur, currentTrack)
                            tickCount++
                            if (tickCount % 2 == 0) {
                                // Add listening time every second (500ms * 2)
                            }
                            if (!prefetched && dur > 0 && pos > dur - 15000) {
                                onPrefetchNext()
                                prefetched = true
                            }
                        }
                        delay(500)
                    }
                }

            } catch (e: Exception) {
                if (requestId != playbackRequestId) return@launch
                println("Failed to stream track '${track.title}': ${e.message}")
                consecutiveFailures++
                val msg = e.message ?: ""
                val errorMsg = if (msg.contains("music.youtube.com") || msg.contains("UnknownHostException") || msg.contains("Failed to connect")) {
                    "You are offline, listen to downloaded audio"
                } else {
                    e.message ?: "Failed to play: ${track.title}"
                }
                onPlaybackError(errorMsg, track)
                
                // Cap automatic skipping to at most 2 consecutive failures to prevent endless cascades
                if (consecutiveFailures < 3) {
                    delay(1500)
                    if (requestId == playbackRequestId) {
                        onTrackEnded()
                    }
                } else {
                    println("Stopping auto-skip cascade after $consecutiveFailures consecutive stream failures.")
                }
            }
        }
    }

    fun prefetchNextTrackUrl(queue: Queue) {
        if (queue.tracks.isEmpty() || !queue.hasNext) return
        val nextTrack = queue.withNextTrack().currentTrack ?: return
        
        if (prefetchedTrackId == nextTrack.id) return // Already prefetched
        
        prefetchedTrackId = nextTrack.id
        scope.launch {
            try {
                val downloadedPath = TrackDownloader.getDownloadedPath(nextTrack)
                if (downloadedPath != null) {
                    prefetchedUrl = downloadedPath
                } else if (nextTrack.isLocal && nextTrack.localPath != null) {
                    prefetchedUrl = nextTrack.localPath
                } else {
                    val videoId = nextTrack.youtubeVideoId ?: nextTrack.id
                    val res = musicRepository.getStreamUrl(videoId)
                    if (res.isSuccess) {
                        prefetchedUrl = res.getOrThrow()
                    } else {
                        val matched = musicRepository.matchSpotifyTrack(nextTrack.title, nextTrack.artistNames, nextTrack.durationMs).getOrThrow()
                        prefetchedUrl = musicRepository.getStreamUrl(matched.youtubeVideoId ?: matched.id).getOrThrow()
                    }
                }
            } catch (e: Exception) {
                // Ignore prefetch errors
            }
        }
    }

    fun release() {
        positionJob?.cancel()
        audioPlayer.release()
    }
}
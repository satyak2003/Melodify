package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.api.lyrics.Lyrics
import com.melodify.shared.api.lyrics.LyricsApi
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.discord.DiscordRpc
import com.melodify.shared.domain.model.Queue
import com.melodify.shared.domain.model.RepeatMode
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.PlayerState
import com.melodify.shared.domain.player.AudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

import com.melodify.shared.data.storage.LastPlayedStorage
import com.melodify.shared.data.storage.TrackDownloader

enum class SleepOption(val label: String, val minutes: Int? = null) {
    OFF("Off"),
    MIN_15("15 Minutes", 15),
    MIN_30("30 Minutes", 30),
    MIN_45("45 Minutes", 45),
    MIN_60("60 Minutes", 60),
    END_OF_TRACK("End of Track")
}

class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val audioPlayer: AudioPlayer,
    private val discordRpc: DiscordRpc,
) : ViewModel() {

    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _queue = MutableStateFlow(Queue())
    val queue: StateFlow<Queue> = _queue.asStateFlow()

    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    private val _downloadingTracks = MutableStateFlow<Set<String>>(emptySet())
    val downloadingTracks: StateFlow<Set<String>> = _downloadingTracks.asStateFlow()

    private val _sleepOption = MutableStateFlow(SleepOption.OFF)
    val sleepOption: StateFlow<SleepOption> = _sleepOption.asStateFlow()

    private val _sleepRemainingMs = MutableStateFlow<Long?>(null)
    val sleepRemainingMs: StateFlow<Long?> = _sleepRemainingMs.asStateFlow()

    private var positionJob: Job? = null
    private var sleepTimerJob: Job? = null

    init {
        // Restore last played track if available
        try {
            val lastPlayed = LastPlayedStorage.loadLastPlayed()
            if (lastPlayed != null && lastPlayed.currentTrack != null) {
                val restoredQueue = Queue(tracks = lastPlayed.queueTracks, currentIndex = lastPlayed.currentIndex)
                _queue.value = restoredQueue
                _playerState.value = PlayerState.Paused(
                    track = lastPlayed.currentTrack,
                    positionMs = lastPlayed.positionMs,
                    durationMs = lastPlayed.durationMs,
                    queue = restoredQueue
                )
            }
        } catch (e: Exception) {
            println("Last played restoration info: ${e.message}")
        }

        // Observe player errors
        viewModelScope.launch {
            audioPlayer.playerError.collect { error ->
                if (error != null) {
                    val current = _queue.value.currentTrack
                    _playerState.value = PlayerState.Error(error, current)
                }
            }
        }
        // Observe buffering state
        viewModelScope.launch {
            audioPlayer.isBuffering.collect { buffering ->
                if (buffering) _playerState.value = PlayerState.Buffering(_queue.value.currentTrack)

            }
        }
        // Observe play/pause changes to keep PlayerState in sync
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                val current = _queue.value.currentTrack ?: return@collect
                val pos = audioPlayer.positionMs.value
                val dur = audioPlayer.durationMs.value
                _playerState.value = if (playing) {
                    PlayerState.Playing(current, pos, dur, _queue.value)
                } else {
                    if (_playerState.value !is PlayerState.Buffering &&
                        _playerState.value !is PlayerState.Error) {
                        PlayerState.Paused(current, pos, dur, _queue.value)
                    } else {
                        _playerState.value
                    }
                }
                LastPlayedStorage.saveLastPlayed(current, _queue.value, pos, dur)
            }
        }
    }

    fun downloadTrack(track: Track) {
        if (_downloadingTracks.value.contains(track.id) || TrackDownloader.isDownloaded(track)) return
        viewModelScope.launch {
            _downloadingTracks.value = _downloadingTracks.value + track.id
            TrackDownloader.downloadTrack(track, musicRepository)
            _downloadingTracks.value = _downloadingTracks.value - track.id
        }
    }

    // ── Sleep Timer ──────────────────────────────────────────────────────

    fun setSleepTimer(option: SleepOption) {
        _sleepOption.value = option
        sleepTimerJob?.cancel()
        _sleepRemainingMs.value = null

        if (option == SleepOption.OFF || option == SleepOption.END_OF_TRACK) {
            return
        }

        val minutes = option.minutes ?: return
        val totalMs = minutes * 60 * 1000L

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalMs
            while (isActive && remaining > 0) {
                _sleepRemainingMs.value = remaining
                delay(1000)
                remaining -= 1000
            }
            _sleepRemainingMs.value = 0L
            audioPlayer.pause()
            _sleepOption.value = SleepOption.OFF
            _sleepRemainingMs.value = null
        }
    }

    // ── Playback control ──────────────────────────────────────────────────

    fun playTrack(track: Track) {
        val newQueue = Queue(tracks = listOf(track), currentIndex = 0)
        _queue.value = newQueue
        startPlayingTrack(track)
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val idx = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.value = Queue(tracks = tracks, currentIndex = idx)
        startPlayingTrack(tracks[idx])
    }

    fun togglePlayPause() {
        val current = _queue.value.currentTrack
        if (audioPlayer.isPlaying.value) {
            audioPlayer.pause()
        } else if (current != null) {
            if (_playerState.value is PlayerState.Paused) {
                audioPlayer.resume()
            } else {
                startPlayingTrack(current)
            }
        }
    }

    fun seekTo(positionMs: Long) = audioPlayer.seekTo(positionMs)

    fun setVolume(volume: Float) = audioPlayer.setVolume(volume)

    // ── Queue control ─────────────────────────────────────────────────────

    fun moveQueueItemUp(index: Int) {
        val q = _queue.value
        if (index <= 0 || index !in q.tracks.indices) return
        val mutableTracks = q.tracks.toMutableList()
        val temp = mutableTracks[index]
        mutableTracks[index] = mutableTracks[index - 1]
        mutableTracks[index - 1] = temp

        val newCurrentIndex = when (q.currentIndex) {
            index -> index - 1
            index - 1 -> index
            else -> q.currentIndex
        }
        val updatedQueue = q.copy(tracks = mutableTracks, currentIndex = newCurrentIndex)
        _queue.value = updatedQueue
        LastPlayedStorage.saveLastPlayed(q.currentTrack, updatedQueue, audioPlayer.positionMs.value, audioPlayer.durationMs.value)
    }

    fun moveQueueItemDown(index: Int) {
        val q = _queue.value
        if (index < 0 || index >= q.tracks.lastIndex) return
        val mutableTracks = q.tracks.toMutableList()
        val temp = mutableTracks[index]
        mutableTracks[index] = mutableTracks[index + 1]
        mutableTracks[index + 1] = temp

        val newCurrentIndex = when (q.currentIndex) {
            index -> index + 1
            index + 1 -> index
            else -> q.currentIndex
        }
        val updatedQueue = q.copy(tracks = mutableTracks, currentIndex = newCurrentIndex)
        _queue.value = updatedQueue
        LastPlayedStorage.saveLastPlayed(q.currentTrack, updatedQueue, audioPlayer.positionMs.value, audioPlayer.durationMs.value)
    }

    fun playNext() {
        if (_sleepOption.value == SleepOption.END_OF_TRACK) {
            audioPlayer.pause()
            _sleepOption.value = SleepOption.OFF
            return
        }
        val q = _queue.value
        if (q.tracks.isEmpty()) return
        // If at end of queue with no repeat, just stop
        if (q.repeatMode == RepeatMode.OFF && q.currentIndex >= q.tracks.size - 1) {
            audioPlayer.pause()
            val track = q.currentTrack
            if (track != null) {
                _playerState.value = PlayerState.Paused(track, audioPlayer.durationMs.value, audioPlayer.durationMs.value, q)
            }
            return
        }
        val nextQ = q.withNextTrack()
        _queue.value = nextQ
        nextQ.currentTrack?.let { startPlayingTrack(it) }
    }

    fun playPrevious() {
        val q = _queue.value
        if (audioPlayer.positionMs.value > 3000L) {
            audioPlayer.seekTo(0L)
            return
        }
        val prevQ = q.withPreviousTrack()
        _queue.value = prevQ
        prevQ.currentTrack?.let { startPlayingTrack(it) }
    }

    fun skipToIndex(index: Int) {
        val q = _queue.value
        if (index in q.tracks.indices) {
            _queue.value = q.copy(currentIndex = index)
            startPlayingTrack(q.tracks[index])
        }
    }

    fun addToQueue(track: Track) {
        _queue.value = _queue.value.addTrack(track)
    }

    fun addTracksNext(tracks: List<Track>) {
        _queue.value = _queue.value.addTracksNext(tracks)
    }

    fun removeFromQueue(index: Int) {
        val q = _queue.value
        if (index !in q.tracks.indices) return
        val newTracks = q.tracks.toMutableList().also { it.removeAt(index) }
        val newIndex = when {
            index < q.currentIndex -> q.currentIndex - 1
            index == q.currentIndex && newTracks.isNotEmpty() ->
                q.currentIndex.coerceAtMost(newTracks.lastIndex)
            else -> q.currentIndex
        }
        _queue.value = q.copy(tracks = newTracks, currentIndex = newIndex)
        if (index == q.currentIndex && newTracks.isNotEmpty()) {
            startPlayingTrack(newTracks[newIndex])
        }
    }

    fun toggleShuffle() {
        val q = _queue.value
        _queue.value = q.withShuffleEnabled(!q.isShuffleEnabled)
    }

    fun cycleRepeatMode() {
        val q = _queue.value
        _queue.value = q.copy(repeatMode = q.repeatMode.next())
    }


    fun setSleepOption(option: SleepOption, durationMinutes: Int = 0) {
        _sleepOption.value = option
        sleepTimerJob?.cancel()
        _sleepRemainingMs.value = null

        if (option != SleepOption.OFF && option != SleepOption.END_OF_TRACK && durationMinutes > 0) {
            val totalMs = durationMinutes * 60 * 1000L
            _sleepRemainingMs.value = totalMs
            sleepTimerJob = viewModelScope.launch {
                var remaining = totalMs
                while (remaining > 0 && this.isActive) {
                    delay(1000)
                    remaining -= 1000
                    _sleepRemainingMs.value = remaining
                }

                audioPlayer.pause()
                _sleepOption.value = SleepOption.OFF
                _sleepRemainingMs.value = null
            }
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────


    private fun startPlayingTrack(track: Track) {
        positionJob?.cancel()
        viewModelScope.launch {
            try {
                _playerState.value = PlayerState.Buffering(track)


                // Check for local downloaded copy first
                val downloadedPath = TrackDownloader.getDownloadedPath(track)
                val url = if (downloadedPath != null) {
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
                        musicRepository.getStreamUrl(matched.youtubeVideoId ?: matched.id).getOrThrow()
                    }
                }

                // Start playback
                audioPlayer.play(url, track)

                // Save last played
                LastPlayedStorage.saveLastPlayed(track, _queue.value, 0L, track.durationMs)

                // Update Discord RPC
                if (discordRpc.isConnected()) {
                    discordRpc.updatePresence(track, isPlaying = true, positionMs = 0L)
                }

                // Track position in a coroutine
                var tickCount = 0
                positionJob = launch {
                    while (isActive) {
                        val pos = audioPlayer.positionMs.value
                        val dur = audioPlayer.durationMs.value
                        val q = _queue.value
                        if (audioPlayer.isPlaying.value) {
                            _playerState.value = PlayerState.Playing(track, pos, dur, q)
                            LastPlayedStorage.saveLastPlayed(track, q, pos, dur)
                            tickCount++
                            if (tickCount % 2 == 0) {
                                com.melodify.shared.data.storage.ListeningStatsStorage.addListeningTime(1)
                            }
                            // Auto-advance when track ends
                            if (dur > 0 && pos >= dur - 500) {
                                playNext()
                                break
                            }
                        }
                        delay(500)
                    }
                }

            } catch (e: Exception) {
                _playerState.value = PlayerState.Error(
                    e.message ?: "Failed to play: ${track.title}",
                    track
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        positionJob?.cancel()
        sleepTimerJob?.cancel()
        audioPlayer.release()
        discordRpc.clearPresence()
    }
}


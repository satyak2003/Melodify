package com.melodify.shared.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.melodify.shared.api.lyrics.Lyrics
import com.melodify.shared.api.lyrics.LyricsApi
import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.discord.DiscordRpc
import com.melodify.shared.domain.model.Queue
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.PlayerState
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.domain.player.AudioPlayer
import com.melodify.shared.data.storage.LastPlayedStorage
import com.melodify.shared.data.storage.ListeningStatsStorage
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerViewModel(
    private val musicRepository: MusicRepository,
    private val audioPlayer: AudioPlayer,
    private val discordRpc: DiscordRpc,
    private val lyricsApi: LyricsApi,
) : ViewModel() {

    // Managers
    private val sleepTimerManager = SleepTimerManager(
        scope = viewModelScope,
        onTimerComplete = { audioPlayer.pause() },
        isPlaying = { audioPlayer.isPlaying.value }
    )
    
    private val queueManager = QueueManager(audioPlayer)
    
    private val playbackManager = PlaybackManager(
        scope = viewModelScope,
        musicRepository = musicRepository,
        audioPlayer = audioPlayer,
        onPlaybackStarted = { track ->
            // Update Discord RPC
            if (discordRpc.isConnected()) {
                discordRpc.updatePresence(track, isPlaying = true, positionMs = 0)
            }
        },
        onPlaybackError = { error, track ->
            _playerState.value = PlayerState.Error(error, track)
        },
        onTrackEnded = { playNext() },
        onBuffering = { track ->
            track?.let { _playerState.value = PlayerState.Buffering(it) }
        },
        onPositionUpdate = { pos, dur, track ->
            val q = queueManager.queue.value
            _playerState.value = PlayerState.Playing(track, pos, dur, q)
            LastPlayedStorage.saveLastPlayed(track, q, pos, dur)
            ListeningStatsStorage.addListeningTime(1)
        },
        onPrefetchNext = { prefetchNextTrack() }
    )
    
    private val downloadManager = DownloadManager(viewModelScope, musicRepository)

    // State
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    val queue: StateFlow<Queue> = queueManager.queue
    
    private val _lyrics = MutableStateFlow<Lyrics?>(null)
    val lyrics: StateFlow<Lyrics?> = _lyrics.asStateFlow()

    val downloadingTracks: StateFlow<Map<String, Float>> = downloadManager.downloadingTracks
    val sleepOption: StateFlow<SleepOption> = sleepTimerManager.sleepOption
    val sleepRemainingMs: StateFlow<Long?> = sleepTimerManager.sleepRemainingMs

    private var lyricsJob: Job? = null

    init {
        // Restore last played track if available
        restoreLastPlayed()

        // Setup audio player callbacks
        setupAudioPlayerCallbacks()

        // Observe player state from playback manager
        observePlaybackState()

        // Observe lyrics
        observeLyrics()

        // Sync Listening Room
        observeSyncSession()
    }

    private fun restoreLastPlayed() {
        try {
            val lastPlayed = LastPlayedStorage.loadLastPlayed()
            if (lastPlayed != null && lastPlayed.currentTrack != null) {
                val restoredQueue = Queue(tracks = lastPlayed.queueTracks, currentIndex = lastPlayed.currentIndex)
                queueManager.setQueue(restoredQueue)
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
    }

    private fun setupAudioPlayerCallbacks() {
        audioPlayer.onTrackEnded = {
            viewModelScope.launch { playNext() }
        }
        audioPlayer.onSkipNext = {
            viewModelScope.launch { playNext() }
        }
        audioPlayer.onSkipPrevious = {
            viewModelScope.launch { playPrevious() }
        }
    }

    private fun observePlaybackState() {
        // Observe player errors
        viewModelScope.launch {
            audioPlayer.playerError.collect { error ->
                if (error != null) {
                    val current = queueManager.queue.value.currentTrack
                    _playerState.value = PlayerState.Error(error, current)
                }
            }
        }
        
        // Observe buffering state
        viewModelScope.launch {
            audioPlayer.isBuffering.collect { buffering ->
                if (buffering) {
                    val current = queueManager.queue.value.currentTrack
                    _playerState.value = PlayerState.Buffering(current)
                }
            }
        }
        
        // Observe play/pause changes for sync
        viewModelScope.launch {
            audioPlayer.isPlaying.collect { playing ->
                val current = queueManager.queue.value.currentTrack
                if (com.melodify.shared.domain.sync.SyncSessionManager.isHost.value) {
                    com.melodify.shared.domain.sync.SyncSessionManager.updateHostState(current, audioPlayer.positionMs.value, playing)
                }
            }
        }
    }

    private fun observeLyrics() {
        viewModelScope.launch {
            playerState.distinctUntilChangedBy { it.currentTrack?.id }
                .map { it.currentTrack }
                .collect { track ->
                    track?.let {
                        lyricsJob?.cancel()
                        lyricsJob = viewModelScope.launch {
                            try {
                                val result = lyricsApi.getLyrics(it.title, it.artist, it.album?.title, it.durationSeconds)
                                _lyrics.value = result
                            } catch (e: Exception) {
                                _lyrics.value = null
                            }
                        }
                    }
                }
        }
    }

    private fun observeSyncSession() {
        viewModelScope.launch {
            com.melodify.shared.domain.sync.SyncSessionManager.currentSession.collect { session ->
                if (session != null && !com.melodify.shared.domain.sync.SyncSessionManager.isHost.value) {
                    val targetTrack = session.activeTrack
                    if (targetTrack != null && targetTrack.id != queueManager.queue.value.currentTrack?.id) {
                        playbackManager.playTrack(targetTrack, session.positionMs)
                    }
                }
            }
        }
    }

    private fun prefetchNextTrack() {
        val q = queueManager.queue.value
        playbackManager.prefetchNextTrackUrl(q)
    }

    // ── Public API ──────────────────────────────────────────────────────────

    fun downloadTrack(track: Track) = downloadManager.downloadTrack(track)

    // Sleep Timer
    fun setSleepTimer(option: SleepOption) = sleepTimerManager.setSleepTimer(option)
    fun setSleepOption(option: SleepOption, durationMinutes: Int = 0) = sleepTimerManager.setSleepOption(option, durationMinutes)

    // Playback control
    fun playTrack(track: Track, initialSeekMs: Long = 0L) {
        queueManager.setQueue(Queue(tracks = listOf(track), currentIndex = 0))
        playbackManager.playTrack(track, initialSeekMs)
    }

    fun playTracks(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val idx = startIndex.coerceIn(0, tracks.lastIndex)
        queueManager.setQueue(Queue(tracks = tracks, currentIndex = idx))
        playbackManager.playTrack(tracks[idx])
    }

    fun togglePlayPause() {
        val current = queueManager.queue.value.currentTrack
        playbackManager.togglePlayPause(current, _playerState.value, audioPlayer.hasMedia.value)
    }

    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun setVolume(volume: Float) = playbackManager.setVolume(volume)

    // Queue control
    fun reorderQueue(fromIndex: Int, toIndex: Int) = queueManager.reorderQueue(fromIndex, toIndex)
    fun moveQueueItemUp(index: Int) = queueManager.moveQueueItemUp(index)
    fun moveQueueItemDown(index: Int) = queueManager.moveQueueItemDown(index)

    fun playNext() {
        if (sleepTimerManager.sleepOption.value == SleepOption.END_OF_TRACK) {
            audioPlayer.pause()
            sleepTimerManager.setSleepTimer(SleepOption.OFF)
            return
        }
        val nextQueue = queueManager.playNext(
            queueManager.queue.value,
            sleepTimerManager.sleepOption.value == SleepOption.END_OF_TRACK,
            com.melodify.shared.data.storage.ExperimentalSettingsStorage.isAutoPlayEnabled.value,
            { com.melodify.shared.domain.RecommendationEngine.getRecommendations(
                excludeIds = queueManager.queue.value.tracks.map { it.id }.toSet(),
                count = 10
            ) }
        )
        queueManager.setQueue(nextQueue)
        nextQueue.currentTrack?.let { playbackManager.playTrack(it) }
    }

    fun playPrevious() {
        val prevQueue = queueManager.playPrevious(queueManager.queue.value)
        queueManager.setQueue(prevQueue)
        prevQueue.currentTrack?.let { playbackManager.playTrack(it) }
    }

    fun skipToIndex(index: Int) {
        val newQueue = queueManager.skipToIndex(queueManager.queue.value, index)
        queueManager.setQueue(newQueue)
        newQueue.currentTrack?.let { playbackManager.playTrack(it) }
    }

    fun addToQueue(track: Track) = queueManager.setQueue(queueManager.addToQueue(queueManager.queue.value, track))
    fun addTracksNext(tracks: List<Track>) = queueManager.setQueue(queueManager.addTracksNext(queueManager.queue.value, tracks))
    fun removeFromQueue(index: Int) {
        val newQueue = queueManager.removeFromQueue(queueManager.queue.value, index)
        queueManager.setQueue(newQueue)
        if (index == queueManager.queue.value.currentIndex && newQueue.tracks.isNotEmpty()) {
            playbackManager.playTrack(newQueue.currentTrack!!)
        }
    }

    fun toggleShuffle() = queueManager.setQueue(queueManager.toggleShuffle(queueManager.queue.value))
    fun cycleRepeatMode() = queueManager.setQueue(queueManager.cycleRepeatMode(queueManager.queue.value))

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
        sleepTimerManager.cancel()
        lyricsJob?.cancel()
        audioPlayer.release()
        discordRpc.clearPresence()
    }
}
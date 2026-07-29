package com.melodify.shared.domain.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class AudioPlayer(private val context: Context) {
    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build(), true)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private var mediaSession: MediaSession? = try {
        MediaSession.Builder(context, player).build()
    } catch (e: Exception) {
        null
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null
    
    private val _isPlaying = MutableStateFlow(false)
    actual val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _positionMs = MutableStateFlow(0L)
    actual val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    actual val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    
    private val _isBuffering = MutableStateFlow(false)
    actual val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _playerError = MutableStateFlow<String?>(null)
    actual val playerError: StateFlow<String?> = _playerError.asStateFlow()

    private val _hasMedia = MutableStateFlow(false)
    actual val hasMedia: StateFlow<Boolean> = _hasMedia.asStateFlow()

    actual var onTrackEnded: (() -> Unit)? = null
    
    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) startPositionTracking() else positionJob?.cancel()
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.takeIf { it > 0 } ?: 0L
                } else if (playbackState == Player.STATE_ENDED) {
                    _isPlaying.value = false
                    onTrackEnded?.invoke()
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playerError.value = error.message
                _isPlaying.value = false
            }
        })
    }
    
    actual fun play(url: String, track: Track) {
        _playerError.value = null
        _hasMedia.value = true
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artistNames)
            .setAlbumTitle(track.album?.title)
            .setArtworkUri(track.thumbnailUrl?.let { Uri.parse(it) })
            .build()


        val uri = if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("content://")) {
            Uri.parse(url)
        } else {
            Uri.fromFile(java.io.File(url))
        }

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadata)
            .build()


        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()
    }
    
    actual fun resume() { player.play() }
    actual fun pause() { player.pause() }
    actual fun seekTo(positionMs: Long) { player.seekTo(positionMs) }
    actual fun stop() { _hasMedia.value = false; player.stop(); positionJob?.cancel() }
    actual fun setVolume(volume: Float) { player.volume = volume }
    
    actual fun release() {
        _hasMedia.value = false
        positionJob?.cancel()
        scope.cancel()
        mediaSession?.release()
        player.release()
    }
    
    private fun startPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                delay(500)
            }
        }
    }
}


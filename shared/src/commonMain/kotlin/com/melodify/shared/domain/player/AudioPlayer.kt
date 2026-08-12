package com.melodify.shared.domain.player

import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic audio player interface.
 * Android: implemented with Media3 ExoPlayer
 * Desktop: implemented with vlcj
 */
expect class AudioPlayer {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val isBuffering: StateFlow<Boolean>
    val playerError: StateFlow<String?>
    val hasMedia: StateFlow<Boolean>
    var onTrackEnded: (() -> Unit)?
    var onSkipNext: (() -> Unit)?
    var onSkipPrevious: (() -> Unit)?

    fun play(url: String, track: Track, initialSeekMs: Long = 0L)
    fun resume()
    fun pause()
    fun seekTo(positionMs: Long)
    fun stop()
    fun release()
    fun setVolume(volume: Float)  // 0.0 to 1.0
}

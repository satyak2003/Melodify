package com.melodify.shared.domain.player

import com.melodify.shared.domain.model.Track
import javafx.application.Platform
import javafx.embed.swing.JFXPanel
import javafx.scene.media.Media
import javafx.scene.media.MediaPlayer
import javafx.util.Duration
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.factory.discovery.NativeDiscovery
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent
import uk.co.caprica.vlcj.player.base.MediaPlayer as VlcMediaPlayer
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter

actual class AudioPlayer {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
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
    actual var onSkipNext: (() -> Unit)? = null
    actual var onSkipPrevious: (() -> Unit)? = null

    // JavaFX Media Engine
    private var fxMediaPlayer: MediaPlayer? = null
    private var fxInitialized = false

    // VLC Engine
    private var vlcAvailable = false
    private var vlcComponent: AudioPlayerComponent? = null

    init {
        // Initialize JavaFX Toolkit
        try {
            JFXPanel()
            fxInitialized = true
        } catch (e: Throwable) {
            println("JavaFX initialization info: ${e.message}")
        }

        // Try VLC discovery
        try {
            vlcAvailable = NativeDiscovery().discover()
            if (vlcAvailable) {
                vlcComponent = AudioPlayerComponent()
                setupVlcListeners()
            }
        } catch (e: Throwable) {
            vlcAvailable = false
        }
    }

    actual fun play(url: String, track: Track, initialSeekMs: Long) {
        _playerError.value = null
        stop()

        _hasMedia.value = true
        _isBuffering.value = true

        if (fxInitialized) {
            Platform.runLater {
                try {
                    val mediaUrl = if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("yt-dlp://")) {
                        StreamProxy.getProxyUrl(url)
                    } else if (url.startsWith("file:/")) {
                        url
                    } else {
                        java.io.File(url).toURI().toString()
                    }

                    val media = Media(mediaUrl)
                    val player = MediaPlayer(media)
                    fxMediaPlayer = player

                    player.setOnReady {
                        _durationMs.value = media.duration.toMillis().toLong()
                        _isBuffering.value = false
                        if (initialSeekMs > 0L) {
                            player.seek(javafx.util.Duration(initialSeekMs.toDouble()))
                            _positionMs.value = initialSeekMs
                        }
                        player.play()
                    }

                    player.setOnPlaying {
                        _isPlaying.value = true
                        _isBuffering.value = false
                        startFxPositionTracking()
                    }

                    player.setOnPaused {
                        _isPlaying.value = false
                        positionJob?.cancel()
                    }

                    player.setOnStopped {
                        _isPlaying.value = false
                        positionJob?.cancel()
                    }

                    player.setOnEndOfMedia {
                        _isPlaying.value = false
                        _positionMs.value = _durationMs.value
                        positionJob?.cancel()
                        onTrackEnded?.invoke()
                    }

                    player.setOnError {
                        val err = player.error?.message ?: "JavaFX playback error"
                        println("JavaFX Media error: $err")
                        if (vlcAvailable) {
                            playVlc(url, initialSeekMs)
                        } else {
                            _playerError.value = "Playback error: $err"
                            _isPlaying.value = false
                            _isBuffering.value = false
                        }
                    }
                } catch (e: Throwable) {
                    println("Failed to start JavaFX playback: ${e.message}")
                    if (vlcAvailable) {
                        playVlc(url, initialSeekMs)
                    } else {
                        _playerError.value = "Unable to play audio."
                        _isBuffering.value = false
                    }
                }
            }
            return
        }


        if (vlcAvailable) {
            playVlc(url, initialSeekMs)
        } else {
            _playerError.value = "Unable to play audio. Neither JavaFX Media nor 64-bit VLC is available."
            _isBuffering.value = false
        }
    }

    private fun playVlc(url: String, initialSeekMs: Long) {
        try {
            // Bypass StreamProxy for YouTube streams to avoid 403s and chunking issues
            val isYouTubeStream = url.contains("googlevideo.com") || url.contains("youtube.com")
            
            val mediaUrl = if (!isYouTubeStream && (url.startsWith("http://") || url.startsWith("https://"))) {
                StreamProxy.getProxyUrl(url)
            } else if (url.startsWith("yt-dlp://")) {
                StreamProxy.getProxyUrl(url)
            } else {
                url
            }
            val options = mutableListOf<String>()
            options.add(":network-caching=3000") // 3 seconds network cache
            if (isYouTubeStream) {
                options.add(":http-user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            }
            if (initialSeekMs > 0L) {
                options.add(":start-time=${initialSeekMs / 1000f}")
                _positionMs.value = initialSeekMs
            }
            vlcComponent?.mediaPlayer()?.media()?.play(mediaUrl, *options.toTypedArray())
        } catch (e: Throwable) {
            _playerError.value = "VLC Playback error: ${e.message}"
            _isBuffering.value = false
        }
    }

    actual fun resume() {
        val fxPlayer = fxMediaPlayer
        if (fxPlayer != null) {
            Platform.runLater { fxPlayer.play() }
        } else {
            vlcComponent?.mediaPlayer()?.controls()?.play()
        }
    }

    actual fun pause() {
        val fxPlayer = fxMediaPlayer
        if (fxPlayer != null) {
            Platform.runLater { fxPlayer.pause() }
        } else {
            vlcComponent?.mediaPlayer()?.controls()?.pause()
        }
    }

    actual fun seekTo(positionMs: Long) {
        val safePosition = positionMs.coerceAtLeast(0L)
        val fxPlayer = fxMediaPlayer
        if (fxPlayer != null) {
            Platform.runLater {
                fxPlayer.seek(Duration.millis(safePosition.toDouble()))
            }
            _positionMs.value = safePosition
        } else {
            vlcComponent?.mediaPlayer()?.controls()?.setTime(safePosition)
            _positionMs.value = safePosition
        }
    }

    actual fun stop() {
        _hasMedia.value = false
        positionJob?.cancel()
        val fxPlayer = fxMediaPlayer
        if (fxPlayer != null) {
            Platform.runLater {
                try {
                    fxPlayer.stop()
                    fxPlayer.dispose()
                } catch (ignored: Throwable) {}
            }
            fxMediaPlayer = null
        }
        try {
            vlcComponent?.mediaPlayer()?.controls()?.stop()
        } catch (ignored: Throwable) {}
        _isPlaying.value = false
        _isBuffering.value = false
    }

    actual fun setVolume(volume: Float) {
        val vol = volume.coerceIn(0f, 1f)
        val fxPlayer = fxMediaPlayer
        if (fxPlayer != null) {
            Platform.runLater { fxPlayer.volume = vol.toDouble() }
        }
        try {
            vlcComponent?.mediaPlayer()?.audio()?.setVolume((vol * 100).toInt())
        } catch (ignored: Throwable) {}
    }

    actual fun release() {
        try { stop() } catch (ignored: Throwable) {}
        scope.cancel()
        try { 
            vlcComponent?.release() 
        } catch (ignored: Throwable) {}
        vlcComponent = null
    }

    private fun startFxPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                fxMediaPlayer?.let { player ->
                    _positionMs.value = player.currentTime.toMillis().toLong()
                }
                delay(250)
            }
        }
    }

    private fun setupVlcListeners() {
        vlcComponent?.mediaPlayer()?.events()?.addMediaPlayerEventListener(object : MediaPlayerEventAdapter() {
            override fun playing(mediaPlayer: VlcMediaPlayer) {
                _isPlaying.value = true
                _isBuffering.value = false
                startVlcPositionTracking()
            }
            override fun paused(mediaPlayer: VlcMediaPlayer) {
                _isPlaying.value = false
            }
            override fun stopped(mediaPlayer: VlcMediaPlayer) {
                _isPlaying.value = false
                _isBuffering.value = false
            }
            override fun buffering(mediaPlayer: VlcMediaPlayer, newCache: Float) {
                _isBuffering.value = newCache < 100f
            }
            override fun error(mediaPlayer: VlcMediaPlayer) {
                _playerError.value = "VLC playback error"
                _isBuffering.value = false
                _isPlaying.value = false
            }
            override fun lengthChanged(mediaPlayer: VlcMediaPlayer, newLength: Long) {
                _durationMs.value = newLength
            }
            override fun finished(mediaPlayer: VlcMediaPlayer) {
                if (_durationMs.value <= 1000L) {
                    _playerError.value = "VLC playback error (Finished instantly)"
                }
                _isPlaying.value = false
                _isBuffering.value = false
                _positionMs.value = 0L
                positionJob?.cancel()
                onTrackEnded?.invoke()
            }
        })
    }

    private fun startVlcPositionTracking() {
        positionJob?.cancel()
        positionJob = scope.launch {
            while (isActive) {
                _positionMs.value = vlcComponent?.mediaPlayer()?.status()?.time() ?: 0L
                delay(500)
            }
        }
    }
}


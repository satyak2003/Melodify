@file:OptIn(UnstableApi::class)

package com.melodify.shared.domain.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import android.os.PowerManager
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

@OptIn(UnstableApi::class)
object AudioCacheManager {
    @Volatile
    private var cache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        return cache ?: synchronized(this) {
            cache ?: SimpleCache(
                File(context.cacheDir, "media_cache"),
                LeastRecentlyUsedCacheEvictor(200 * 1024 * 1024L),
                StandaloneDatabaseProvider(context)
            ).also { cache = it }
        }
    }
}

@OptIn(UnstableApi::class)
actual class AudioPlayer(private val context: Context) {

    private val cacheDataSourceFactory = CacheDataSource.Factory()
        .setCache(AudioCacheManager.getCache(context))
        .setUpstreamDataSourceFactory(
            DefaultDataSource.Factory(context, DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .setAllowCrossProtocolRedirects(true))
        )

    private val loadControl = androidx.media3.exoplayer.DefaultLoadControl.Builder()
        .setBufferDurationsMs(
            25000, // minBufferMs
            50000, // maxBufferMs
            1500,  // bufferForPlaybackMs
            3000   // bufferForPlaybackAfterRebufferMs
        ).build()

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(cacheDataSourceFactory))
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build(), true)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .setHandleAudioBecomingNoisy(true)
        .build()

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var positionJob: Job? = null
    
    private var transitionWakeLock: PowerManager.WakeLock? = null

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

    private val androidEqualizer = AndroidEqualizerManager()
    private val androidAudioOutput = AndroidAudioOutputManager(context)
    
    actual val equalizerManager: EqualizerManager = androidEqualizer
    actual val audioOutputManager: AudioOutputManager = androidAudioOutput

    init {
        androidAudioOutput.start()
        
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        transitionWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Melodify:TransitionWakeLock")
        transitionWakeLock?.setReferenceCounted(false)

        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                androidEqualizer.attachSession(audioSessionId)
            }
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
                if (isPlaying) {
                    startPositionTracking()
                } else {
                    _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                    positionJob?.cancel()
                }
            }
            override fun onPlaybackStateChanged(playbackState: Int) {
                _isBuffering.value = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_READY) {
                    _durationMs.value = player.duration.takeIf { it > 0 } ?: 0L
                } else if (playbackState == Player.STATE_ENDED) {
                    _positionMs.value = player.currentPosition.coerceAtLeast(0L)
                    _isPlaying.value = false
                    _hasMedia.value = false
                    if (player.playerError == null) {
                        transitionWakeLock?.acquire(30000L)
                        onTrackEnded?.invoke()
                    }
                }
            }
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _playerError.value = error.message
                _isPlaying.value = false
            }
        })
    }

    fun releaseTransitionWakeLock() {
        if (transitionWakeLock?.isHeld == true) {
            transitionWakeLock?.release()
        }
    }

    actual fun play(url: String, track: Track, initialSeekMs: Long) {
        releaseTransitionWakeLock()
        _playerError.value = null
        _hasMedia.value = true
        scope.launch(Dispatchers.Main) {
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
            if (initialSeekMs > 0) {
                player.seekTo(initialSeekMs)
                _positionMs.value = initialSeekMs
            }
            player.prepare()
            player.play()
        }
    }

    actual fun resume() { scope.launch(Dispatchers.Main) { player.play() } }
    actual fun pause() { scope.launch(Dispatchers.Main) { player.pause() } }
    actual fun seekTo(positionMs: Long) { scope.launch(Dispatchers.Main) { player.seekTo(positionMs) } }
    actual fun stop() {
        _hasMedia.value = false
        positionJob?.cancel()
        scope.launch(Dispatchers.Main) { player.stop() }
    }
    actual fun setVolume(volume: Float) { scope.launch(Dispatchers.Main) { player.volume = volume } }

    actual fun release() {
        _hasMedia.value = false
        positionJob?.cancel()
        androidAudioOutput.stop()
        androidEqualizer.release()
        scope.launch(Dispatchers.Main) { player.release() }
        scope.cancel()
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


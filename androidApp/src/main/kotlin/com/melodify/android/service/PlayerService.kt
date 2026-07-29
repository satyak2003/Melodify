package com.melodify.android.service

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melodify.shared.domain.player.AudioPlayer
import org.koin.android.ext.android.inject

class PlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val audioPlayer: AudioPlayer by inject()

    override fun onCreate() {
        super.onCreate()
        val player = audioPlayer.player
        mediaSession = MediaSession.Builder(this, player)
            .setCallback(MelodifySessionCallback())
            .build()

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                broadcastDiscordMobileStatus(isPlaying)
            }
        })
    }

    private fun broadcastDiscordMobileStatus(isPlaying: Boolean) {
        try {
            val item = audioPlayer.player.currentMediaItem ?: return
            val metadata = item.mediaMetadata

            // Broadcast Spotify-compatible Intent for Discord Android mobile status detection
            val metaIntent = Intent("com.spotify.music.metadatachanged").apply {
                putExtra("id", item.mediaId)
                putExtra("artist", metadata.artist?.toString() ?: "Unknown Artist")
                putExtra("album", metadata.albumTitle?.toString() ?: "Melodify")
                putExtra("track", metadata.title?.toString() ?: "Track")
                putExtra("length", audioPlayer.durationMs.value.toInt())
                putExtra("playing", isPlaying)
            }
            sendBroadcast(metaIntent)

            val stateIntent = Intent("com.spotify.music.playbackstatechanged").apply {
                putExtra("playing", isPlaying)
                putExtra("position", audioPlayer.positionMs.value)
            }
            sendBroadcast(stateIntent)
        } catch (e: Exception) {
            // Ignore broadcast exceptions
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private inner class MelodifySessionCallback : MediaSession.Callback
}


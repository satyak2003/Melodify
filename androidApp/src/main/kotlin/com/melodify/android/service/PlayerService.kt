package com.melodify.android.service

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.melodify.shared.domain.player.AudioPlayer
import org.koin.android.ext.android.inject

class PlayerService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val audioPlayer: AudioPlayer by inject()

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSession.Builder(this, audioPlayer.player)
            .setCallback(MelodifySessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private inner class MelodifySessionCallback : MediaSession.Callback
}


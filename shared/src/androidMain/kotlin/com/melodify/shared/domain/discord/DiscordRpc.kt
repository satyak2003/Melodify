package com.melodify.shared.domain.discord

import com.melodify.shared.domain.model.Track

actual class DiscordRpc {
    actual fun updatePresence(track: Track, isPlaying: Boolean, positionMs: Long) {
        // Discord Rich Presence is Desktop-only in Melodify
        // Android does not support official Discord IPC
    }
    actual fun clearPresence() {
        // No-op on Android
    }
    actual fun isConnected(): Boolean = false
}

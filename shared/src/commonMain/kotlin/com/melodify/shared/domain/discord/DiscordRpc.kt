package com.melodify.shared.domain.discord

import com.melodify.shared.domain.model.Track

expect class DiscordRpc {
    fun updatePresence(track: Track, isPlaying: Boolean, positionMs: Long)
    fun clearPresence()
    fun isConnected(): Boolean
}

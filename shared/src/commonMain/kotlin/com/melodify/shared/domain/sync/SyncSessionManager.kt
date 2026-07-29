package com.melodify.shared.domain.sync

import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

data class SyncSession(
    val sessionCode: String,
    val hostName: String,
    val activeTrack: Track?,
    val positionMs: Long,
    val isPlaying: Boolean,
    val connectedListenersCount: Int = 1
)

object SyncSessionManager {
    private val _currentSession = MutableStateFlow<SyncSession?>(null)
    val currentSession: StateFlow<SyncSession?> = _currentSession.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    fun createSession(hostName: String, currentTrack: Track?): SyncSession {
        val code = generateSessionCode()
        val session = SyncSession(
            sessionCode = code,
            hostName = hostName,
            activeTrack = currentTrack,
            positionMs = 0L,
            isPlaying = true,
            connectedListenersCount = 1
        )
        _isHost.value = true
        _currentSession.value = session
        return session
    }

    fun joinSession(code: String, listenerName: String): Boolean {
        val formattedCode = code.trim().uppercase()
        if (formattedCode.length < 4) return false
        _isHost.value = false
        _currentSession.value = SyncSession(
            sessionCode = formattedCode,
            hostName = "Host-$formattedCode",
            activeTrack = null,
            positionMs = 0L,
            isPlaying = false,
            connectedListenersCount = 2
        )
        return true
    }

    fun updateHostState(track: Track?, positionMs: Long, isPlaying: Boolean) {
        val existing = _currentSession.value ?: return
        if (_isHost.value) {
            _currentSession.value = existing.copy(
                activeTrack = track,
                positionMs = positionMs,
                isPlaying = isPlaying
            )
        }
    }

    fun leaveSession() {
        _currentSession.value = null
        _isHost.value = false
    }

    private fun generateSessionCode(): String {
        val charPool = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }
}

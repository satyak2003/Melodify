package com.melodify.shared.domain.sync

import com.melodify.shared.domain.model.Track
import com.melodify.shared.data.storage.SupabaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.random.Random

@Serializable
data class SyncSession(
    val sessionCode: String,
    val hostName: String,
    val activeTrack: Track? = null,
    val positionMs: Long = 0L,
    val isPlaying: Boolean = false,
    val connectedListenersCount: Int = 1
)

object SyncSessionManager {
    private val _currentSession = MutableStateFlow<SyncSession?>(null)
    val currentSession: StateFlow<SyncSession?> = _currentSession.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var syncJob: Job? = null
    
    private val jsonConfig = Json { ignoreUnknownKeys = true }

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
        startSyncing()
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
        startSyncing()
        return true
    }

    fun updateHostState(track: Track?, positionMs: Long, isPlaying: Boolean) {
        val existing = _currentSession.value ?: return
        if (_isHost.value) {
            val updated = existing.copy(
                activeTrack = track,
                positionMs = positionMs,
                isPlaying = isPlaying
            )
            _currentSession.value = updated
            // Push immediately to Firebase
            scope.launch {
                SupabaseApi.writeSession(existing.sessionCode, jsonConfig.encodeToString(updated))
            }
        }
    }

    fun leaveSession() {
        val code = _currentSession.value?.sessionCode
        if (_isHost.value && code != null) {
            scope.launch { SupabaseApi.deleteSession(code) }
        }
        _currentSession.value = null
        _isHost.value = false
        syncJob?.cancel()
        syncJob = null
    }
    
    private fun startSyncing() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (true) {
                val session = _currentSession.value
                if (session != null) {
                    if (_isHost.value) {
                        SupabaseApi.writeSession(session.sessionCode, jsonConfig.encodeToString(session))
                    } else {
                        val remoteData = SupabaseApi.getSession(session.sessionCode)
                        if (remoteData != null && remoteData != "null") {
                            try {
                                val remoteSession = jsonConfig.decodeFromString<SyncSession>(remoteData)
                                _currentSession.value = remoteSession
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }
                delay(2000L) // Poll every 2 seconds
            }
        }
    }

    private fun generateSessionCode(): String {
        val charPool = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return (1..6)
            .map { Random.nextInt(0, charPool.length) }
            .map(charPool::get)
            .joinToString("")
    }
}

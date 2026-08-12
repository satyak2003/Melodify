package com.melodify.shared.domain.discord

import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.*
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.serialization.json.*
import kotlinx.serialization.encodeToString

actual class DiscordRpc {
    private val CLIENT_ID = "1531981374993727559" // Melodify App ID
    private var pipe: RandomAccessFile? = null
    private var connected = false
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch { tryConnect() }
    }

    private suspend fun tryConnect() {
        if (connected) return
        withContext(Dispatchers.IO) {
            for (i in 0..9) {
                try {
                    val p = RandomAccessFile("\\\\.\\pipe\\discord-ipc-$i", "rw")
                    val handshake = buildJsonObject {
                        put("v", 1)
                        put("client_id", CLIENT_ID)
                    }.toString()
                    sendRaw(p, 0, handshake)
                    readRaw(p) // Read response

                    pipe = p
                    connected = true
                    break
                } catch (e: Exception) {
                    connected = false
                }
            }
        }
    }

    private fun sendRaw(p: RandomAccessFile, opcode: Int, data: String) {
        val bytes = data.toByteArray(Charsets.UTF_8)
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        header.putInt(opcode)
        header.putInt(bytes.size)
        p.write(header.array())
        p.write(bytes)
    }

    private fun readRaw(p: RandomAccessFile): String? {
        try {
            val header = ByteArray(8)
            p.readFully(header)
            val buffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            val opcode = buffer.int
            val length = buffer.int
            val data = ByteArray(length)
            p.readFully(data)
            return String(data, Charsets.UTF_8)
        } catch (e: Exception) {
            return null
        }
    }

    actual fun updatePresence(track: Track, isPlaying: Boolean, positionMs: Long) {
        scope.launch {
            if (!connected || pipe == null) {
                tryConnect()
            }
            val activePipe = pipe ?: return@launch
            try {
                val payload = buildJsonObject {
                    put("cmd", "SET_ACTIVITY")
                    put("nonce", System.currentTimeMillis().toString())
                    putJsonObject("args") {
                        put("pid", ProcessHandle.current().pid().toInt())
                        putJsonObject("activity") {
                            put("details", track.title.take(128))
                            put("state", track.artistNames.take(128))
                            putJsonObject("assets") {
                                put("large_image", "melodify_logo")
                                put("large_text", "Melodify Music Player")
                                put("small_image", if (isPlaying) "play" else "pause")
                                put("small_text", if (isPlaying) "Playing" else "Paused")
                            }
                            if (isPlaying && track.durationMs > 0) {
                                putJsonObject("timestamps") {
                                    val startTime = (System.currentTimeMillis() - positionMs) / 1000
                                    val endTime = startTime + (track.durationMs / 1000)
                                    put("start", startTime)
                                    put("end", endTime)
                                }
                            }
                            val session = com.melodify.shared.domain.sync.SyncSessionManager.currentSession.value
                            if (session != null) {
                                putJsonObject("party") {
                                    put("id", "party_${session.sessionCode}")
                                    putJsonArray("size") {
                                        add(session.connectedListenersCount)
                                        add(8)
                                    }
                                }
                                putJsonObject("secrets") {
                                    put("join", session.sessionCode)
                                }
                            }
                        }
                    }
                }
                sendRaw(activePipe, 1, payload.toString())
                readRaw(activePipe)
            } catch (e: Exception) {
                connected = false
                pipe = null
            }
        }
    }

    actual fun clearPresence() {
        scope.launch {
            val activePipe = pipe ?: return@launch
            try {
                val payload = buildJsonObject {
                    put("cmd", "SET_ACTIVITY")
                    put("nonce", System.currentTimeMillis().toString())
                    putJsonObject("args") {
                        put("pid", ProcessHandle.current().pid().toInt())
                        put("activity", JsonNull)
                    }
                }
                sendRaw(activePipe, 1, payload.toString())
                readRaw(activePipe)
            } catch (e: Exception) {
                connected = false
                pipe = null
            }
        }
    }

    actual fun isConnected(): Boolean = connected

    fun disconnect() {
        try {
            pipe?.close()
        } catch (ignored: Exception) {}
        pipe = null
        connected = false
        scope.cancel()
    }
}


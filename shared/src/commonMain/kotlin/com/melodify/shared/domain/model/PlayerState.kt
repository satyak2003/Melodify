package com.melodify.shared.domain.model

/**
 * Represents the player's current queue.
 */
data class Queue(
    val tracks: List<Track> = emptyList(),
    val currentIndex: Int = 0,
    val shuffledIndices: List<Int>? = null,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val isShuffleEnabled: Boolean = false,
) {
    val currentTrack: Track?
        get() {
            if (tracks.isEmpty()) return null
            return if (isShuffleEnabled && shuffledIndices != null) {
                val realIdx = shuffledIndices.getOrNull(currentIndex) ?: 0
                tracks.getOrNull(realIdx)
            } else {
                tracks.getOrNull(currentIndex)
            }
        }

    val maxIndex: Int
        get() = if (isShuffleEnabled && shuffledIndices != null) shuffledIndices.lastIndex else tracks.lastIndex

    val hasNext: Boolean get() = currentIndex < maxIndex || repeatMode != RepeatMode.OFF
    val hasPrevious: Boolean get() = currentIndex > 0 || repeatMode != RepeatMode.OFF
    val isEmpty: Boolean get() = tracks.isEmpty()

    fun withNextTrack(): Queue {
        if (tracks.isEmpty()) return this
        val max = maxIndex
        if (max <= 0 && repeatMode != RepeatMode.ALL) return this
        return when (repeatMode) {
            RepeatMode.ONE -> this
            RepeatMode.ALL -> copy(currentIndex = if (max >= 0) (currentIndex + 1) % (max + 1) else 0)
            RepeatMode.OFF -> if (currentIndex < max) copy(currentIndex = currentIndex + 1) else this
        }
    }

    fun withPreviousTrack(): Queue {
        if (tracks.isEmpty()) return this
        val max = maxIndex
        return when {
            currentIndex > 0 -> copy(currentIndex = currentIndex - 1)
            repeatMode == RepeatMode.ALL -> copy(currentIndex = max.coerceAtLeast(0))
            else -> this
        }
    }

    fun withShuffleEnabled(enabled: Boolean): Queue {
        if (tracks.isEmpty()) return copy(isShuffleEnabled = enabled, shuffledIndices = null)
        return if (enabled) {
            val currentOriginalIdx = if (isShuffleEnabled && shuffledIndices != null) {
                shuffledIndices.getOrNull(currentIndex) ?: 0
            } else {
                currentIndex.coerceIn(0, tracks.lastIndex)
            }
            val otherIndices = (tracks.indices).filterNot { it == currentOriginalIdx }.shuffled()
            val newShuffled = listOf(currentOriginalIdx) + otherIndices
            copy(isShuffleEnabled = true, shuffledIndices = newShuffled, currentIndex = 0)
        } else {
            val currentOriginalIdx = if (shuffledIndices != null) {
                shuffledIndices.getOrNull(currentIndex) ?: 0
            } else {
                currentIndex
            }
            copy(isShuffleEnabled = false, shuffledIndices = null, currentIndex = currentOriginalIdx.coerceIn(0, tracks.lastIndex))
        }
    }

    fun addTrack(track: Track): Queue {
        val newTracks = tracks + track
        val newShuffled = if (isShuffleEnabled && shuffledIndices != null) {
            shuffledIndices + (newTracks.size - 1)
        } else null
        return copy(tracks = newTracks, shuffledIndices = newShuffled)
    }

    fun addTracksNext(newTracks: List<Track>): Queue {
        val before = tracks.subList(0, currentIndex + 1)
        val after = tracks.subList(currentIndex + 1, tracks.size)
        val combined = before + newTracks + after
        return copy(tracks = combined, shuffledIndices = null, isShuffleEnabled = false)
    }

    fun replaceTracks(newTracks: List<Track>, startIndex: Int = 0): Queue =
        copy(tracks = newTracks, currentIndex = startIndex.coerceIn(0, maxOf(0, newTracks.size - 1)), shuffledIndices = null, isShuffleEnabled = false)
}

enum class RepeatMode {
    OFF, ONE, ALL;

    fun next(): RepeatMode = when (this) {
        OFF -> ONE
        ONE -> ALL
        ALL -> OFF
    }
}

/**
 * Sealed class representing the full player state.
 */
sealed class PlayerState {
    object Idle : PlayerState()
    data class Buffering(val track: Track? = null) : PlayerState()
    data class Playing(
        val track: Track,
        val positionMs: Long,
        val durationMs: Long,
        val queue: Queue,
    ) : PlayerState()
    data class Paused(
        val track: Track,
        val positionMs: Long,
        val durationMs: Long,
        val queue: Queue,
    ) : PlayerState()
    data class Error(val message: String, val track: Track? = null) : PlayerState()
}

val PlayerState.currentTrack: Track?
    get() = when (this) {
        is PlayerState.Playing -> track
        is PlayerState.Paused -> track
        is PlayerState.Buffering -> track
        is PlayerState.Error -> track
        else -> null
    }


val PlayerState.isPlaying: Boolean
    get() = this is PlayerState.Playing

val PlayerState.positionMs: Long
    get() = when (this) {
        is PlayerState.Playing -> positionMs
        is PlayerState.Paused -> positionMs
        else -> 0L
    }

val PlayerState.durationMs: Long
    get() = when (this) {
        is PlayerState.Playing -> durationMs
        is PlayerState.Paused -> durationMs
        else -> 0L
    }

val PlayerState.queue: Queue
    get() = when (this) {
        is PlayerState.Playing -> queue
        is PlayerState.Paused -> queue
        else -> Queue()
    }

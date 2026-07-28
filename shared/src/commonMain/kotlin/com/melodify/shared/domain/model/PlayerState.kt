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
    val currentTrack: Track? get() = if (tracks.isNotEmpty() && currentIndex in tracks.indices) tracks[currentIndex] else null
    val hasNext: Boolean get() = currentIndex < tracks.size - 1 || repeatMode != RepeatMode.OFF
    val hasPrevious: Boolean get() = currentIndex > 0 || repeatMode != RepeatMode.OFF
    val isEmpty: Boolean get() = tracks.isEmpty()

    fun withNextTrack(): Queue {
        if (tracks.isEmpty()) return this
        return when (repeatMode) {
            RepeatMode.ONE -> this
            RepeatMode.ALL -> copy(currentIndex = (currentIndex + 1) % tracks.size)
            RepeatMode.OFF -> if (currentIndex < tracks.size - 1) copy(currentIndex = currentIndex + 1) else this
        }
    }

    fun withPreviousTrack(): Queue {
        if (tracks.isEmpty()) return this
        return when {
            currentIndex > 0 -> copy(currentIndex = currentIndex - 1)
            repeatMode == RepeatMode.ALL -> copy(currentIndex = tracks.size - 1)
            else -> this
        }
    }

    fun withShuffleEnabled(enabled: Boolean): Queue {
        return if (enabled) {
            val indices = (tracks.indices).toMutableList().also { it.shuffle() }
            copy(isShuffleEnabled = true, shuffledIndices = indices)
        } else {
            copy(isShuffleEnabled = false, shuffledIndices = null)
        }
    }

    fun addTrack(track: Track): Queue = copy(tracks = tracks + track)
    fun addTracksNext(newTracks: List<Track>): Queue {
        val before = tracks.subList(0, currentIndex + 1)
        val after = tracks.subList(currentIndex + 1, tracks.size)
        return copy(tracks = before + newTracks + after)
    }
    fun replaceTracks(newTracks: List<Track>, startIndex: Int = 0): Queue =
        copy(tracks = newTracks, currentIndex = startIndex.coerceIn(0, maxOf(0, newTracks.size - 1)))
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

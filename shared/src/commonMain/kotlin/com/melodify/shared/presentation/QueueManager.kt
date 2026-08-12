package com.melodify.shared.presentation

import com.melodify.shared.domain.model.Queue
import com.melodify.shared.domain.model.RepeatMode
import com.melodify.shared.domain.model.Track
import com.melodify.shared.data.storage.LastPlayedStorage
import com.melodify.shared.domain.player.AudioPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QueueManager(
    private val audioPlayer: AudioPlayer,
    initialQueue: Queue = Queue()
) {
    private val _queue = MutableStateFlow(initialQueue)
    val queue: StateFlow<Queue> = _queue.asStateFlow()

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val q = _queue.value
        if (fromIndex !in q.tracks.indices || toIndex !in q.tracks.indices || fromIndex == toIndex) return
        val mutableTracks = q.tracks.toMutableList()
        val item = mutableTracks.removeAt(fromIndex)
        mutableTracks.add(toIndex, item)

        val newCurrentIndex = when {
            q.currentIndex == fromIndex -> toIndex
            fromIndex < toIndex && q.currentIndex in (fromIndex + 1)..toIndex -> q.currentIndex - 1
            toIndex < fromIndex && q.currentIndex in toIndex..<fromIndex -> q.currentIndex + 1
            else -> q.currentIndex
        }
        val updatedQueue = q.copy(tracks = mutableTracks, currentIndex = newCurrentIndex)
        _queue.value = updatedQueue
        saveLastPlayed()
    }

    fun moveQueueItemUp(index: Int) {
        val q = _queue.value
        if (index <= 0 || index !in q.tracks.indices) return
        val mutableTracks = q.tracks.toMutableList()
        val temp = mutableTracks[index]
        mutableTracks[index] = mutableTracks[index - 1]
        mutableTracks[index - 1] = temp

        val newCurrentIndex = when (q.currentIndex) {
            index -> index - 1
            index - 1 -> index
            else -> q.currentIndex
        }
        val updatedQueue = q.copy(tracks = mutableTracks, currentIndex = newCurrentIndex)
        _queue.value = updatedQueue
        saveLastPlayed()
    }

    fun moveQueueItemDown(index: Int) {
        val q = _queue.value
        if (index < 0 || index >= q.tracks.lastIndex) return
        val mutableTracks = q.tracks.toMutableList()
        val temp = mutableTracks[index]
        mutableTracks[index] = mutableTracks[index + 1]
        mutableTracks[index + 1] = temp

        val newCurrentIndex = when (q.currentIndex) {
            index -> index + 1
            index + 1 -> index
            else -> q.currentIndex
        }
        val updatedQueue = q.copy(tracks = mutableTracks, currentIndex = newCurrentIndex)
        _queue.value = updatedQueue
        saveLastPlayed()
    }

    fun playNext(currentQueue: Queue, isEndOfTrackSleep: Boolean, autoPlayEnabled: Boolean, recommendationEngine: () -> List<Track>): Queue {
        if (isEndOfTrackSleep) {
            return currentQueue
        }
        val q = currentQueue
        if (q.tracks.isEmpty()) return q
        
        // If at end of queue with no repeat, just stop
        if (q.repeatMode == RepeatMode.OFF && q.currentIndex >= q.tracks.size - 1) {
            if (autoPlayEnabled) {
                val currentTrackIds = q.tracks.map { it.id }.toSet()
                val recommendations = recommendationEngine()
                if (recommendations.isNotEmpty()) {
                    val newTracks = q.tracks + recommendations
                    return q.copy(
                        tracks = newTracks,
                        currentIndex = q.currentIndex + 1
                    )
                }
            }
            return q
        }
        return q.withNextTrack()
    }

    fun playPrevious(currentQueue: Queue): Queue {
        val q = currentQueue
        if (audioPlayer.positionMs.value > 3000L) {
            audioPlayer.seekTo(0L)
            return q
        }
        return q.withPreviousTrack()
    }

    fun skipToIndex(queue: Queue, index: Int): Queue {
        if (index in queue.tracks.indices) {
            return queue.copy(currentIndex = index)
        }
        return queue
    }

    fun addToQueue(queue: Queue, track: Track): Queue {
        return queue.addTrack(track)
    }

    fun addTracksNext(queue: Queue, tracks: List<Track>): Queue {
        return queue.addTracksNext(tracks)
    }

    fun removeFromQueue(queue: Queue, index: Int): Queue {
        if (index !in queue.tracks.indices) return queue
        val newTracks = queue.tracks.toMutableList().also { it.removeAt(index) }
        val newIndex = when {
            index < queue.currentIndex -> queue.currentIndex - 1
            index == queue.currentIndex && newTracks.isNotEmpty() ->
                queue.currentIndex.coerceAtMost(newTracks.lastIndex)
            else -> queue.currentIndex
        }
        return queue.copy(tracks = newTracks, currentIndex = newIndex)
    }

    fun toggleShuffle(queue: Queue): Queue {
        return queue.withShuffleEnabled(!queue.isShuffleEnabled)
    }

    fun cycleRepeatMode(queue: Queue): Queue {
        return queue.copy(repeatMode = queue.repeatMode.next())
    }

    private fun saveLastPlayed() {
        val q = _queue.value
        LastPlayedStorage.saveLastPlayed(q.currentTrack, q, audioPlayer.positionMs.value, audioPlayer.durationMs.value)
    }

    fun setQueue(queue: Queue) {
        _queue.value = queue
    }
}
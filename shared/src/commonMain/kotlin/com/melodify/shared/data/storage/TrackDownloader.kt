package com.melodify.shared.data.storage

import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

object TrackDownloader {
    val downloadsDir: File
        get() = AppStorage.getDownloadsDir()

    private fun targetFileFor(track: Track): File = File(downloadsDir, "${track.id}.m4a")
    private fun partialFileFor(track: Track): File = File(downloadsDir, "${track.id}.m4a.part")

    private fun targetFileFlac(track: Track): File = File(downloadsDir, "${track.id}.flac")

    fun isDownloaded(track: Track): Boolean {
        if (track.localPath != null && File(track.localPath).exists()) return true
        val targetM4a = targetFileFor(track)
        val targetFlac = targetFileFlac(track)
        return (targetM4a.exists() && targetM4a.length() > 0) || (targetFlac.exists() && targetFlac.length() > 0)
    }

    fun getDownloadedPath(track: Track): String? {
        if (track.localPath != null && File(track.localPath).exists()) return track.localPath
        val targetFlac = targetFileFlac(track)
        if (targetFlac.exists() && targetFlac.length() > 0) return targetFlac.absolutePath
        val targetM4a = targetFileFor(track)
        return if (targetM4a.exists() && targetM4a.length() > 0) targetM4a.absolutePath else null
    }

    suspend fun downloadTrack(
        track: Track,
        musicRepository: MusicRepository,
        onProgress: (Float) -> Unit = {}
    ): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = targetFileFor(track)
            if (targetFile.exists() && targetFile.length() > 0) {
                return@runCatching track.copy(localPath = targetFile.absolutePath)
            }

            var activeTrack = track
            val videoId = track.youtubeVideoId ?: track.id
            val streamUrl = try {
                musicRepository.getStreamUrl(videoId).getOrThrow()
            } catch (e: Exception) {
                val matched = musicRepository.matchSpotifyTrack(track.title, track.artistNames, track.durationMs).getOrThrow()
                activeTrack = track.copy(
                    youtubeVideoId = matched.youtubeVideoId ?: matched.id,
                    thumbnailUrl = track.thumbnailUrl ?: matched.thumbnailUrl
                )
                musicRepository.getStreamUrl(activeTrack.youtubeVideoId ?: activeTrack.id).getOrThrow()
            }
            
            return@runCatching downloadFromUrl(activeTrack, streamUrl, targetFile, onProgress)
        }
    }

    suspend fun downloadFlacFromUrl(
        track: Track,
        url: String,
        onProgress: (Float) -> Unit = {}
    ): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = targetFileFlac(track)
            if (targetFile.exists() && targetFile.length() > 0) {
                onProgress(1.0f)
                return@runCatching track.copy(localPath = targetFile.absolutePath)
            }
            return@runCatching downloadFromUrl(track, url, targetFile, onProgress)
        }
    }

    private suspend fun downloadFromUrl(
        track: Track,
        streamUrl: String,
        targetFile: File,
        onProgress: (Float) -> Unit
    ): Track = withContext(Dispatchers.IO) {
        val partialFile = File(downloadsDir, "${targetFile.name}.part")
        
        val conn = URL(streamUrl).openConnection() as HttpURLConnection
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connectTimeout = 15_000
            conn.readTimeout = 30_000

            // Resume from existing partial file if the server supports ranges
            val existingBytes = if (partialFile.exists()) partialFile.length() else 0L
            if (existingBytes > 0) {
                conn.setRequestProperty("Range", "bytes=$existingBytes-")
            }
            conn.connect()

            val responseCode = conn.responseCode
            val didResume = existingBytes > 0 && responseCode == HttpURLConnection.HTTP_PARTIAL
            val startOffset = if (didResume) existingBytes else 0L
            val totalBytes = when (responseCode) {
                HttpURLConnection.HTTP_OK -> conn.contentLengthLong
                HttpURLConnection.HTTP_PARTIAL -> if (conn.contentLengthLong >= 0) conn.contentLengthLong + startOffset else -1L
                else -> throw IOException("Server returned HTTP $responseCode for ${track.title}")
            }

            // If the server ignored the Range header, restart from scratch
            if (existingBytes > 0 && !didResume) {
                partialFile.delete()
            }

            val inputStream = conn.getInputStream()
            val outputStream = FileOutputStream(partialFile, didResume)
            inputStream.use { input ->
                outputStream.use { output ->
                    val buffer = ByteArray(16384)
                    var downloadedBytes = startOffset
                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                    output.flush()
                }
            }

            // Atomic-ish publish: rename .part to final name only after a complete write
            if (!partialFile.renameTo(targetFile)) {
                partialFile.copyTo(targetFile, overwrite = true)
                partialFile.delete()
            }

            if (!targetFile.exists() || targetFile.length() == 0L) {
                partialFile.delete()
                throw IOException("Download produced an empty file for ${track.title}")
            }

            track.copy(localPath = targetFile.absolutePath)
        }

    suspend fun downloadPlaylistParallel(
        tracks: List<Track>,
        musicRepository: MusicRepository,
        maxConcurrency: Int = 4,
        onProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): List<Result<Track>> = withContext(Dispatchers.IO) {
        val semaphore = Semaphore(maxConcurrency)
        val completedCount = AtomicInteger(0)
        val total = tracks.size

        tracks.map { track ->
            async {
                semaphore.withPermit {
                    val result = downloadTrack(track, musicRepository)
                    val done = completedCount.incrementAndGet()
                    onProgress(done, total)
                    result
                }
            }
        }.awaitAll()
    }

    fun deleteDownloadedTrack(track: Track): Boolean {
        try {
            val file = targetFileFor(track)
            if (file.exists()) file.delete()
            val partial = partialFileFor(track)
            if (partial.exists()) partial.delete()
            return !file.exists() && !partial.exists()
        } catch (ignored: Exception) {}
        return false
    }
}

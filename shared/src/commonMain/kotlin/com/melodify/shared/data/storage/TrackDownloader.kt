package com.melodify.shared.data.storage

import com.melodify.shared.data.MusicRepository
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

object TrackDownloader {
    val downloadsDir: File
        get() {
            val userHome = System.getProperty("user.home") ?: "."
            val dir = File(userHome, ".melodify/downloads")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun isDownloaded(track: Track): Boolean {
        if (track.localPath != null && File(track.localPath).exists()) return true
        val targetFile = File(downloadsDir, "${track.id}.m4a")
        return targetFile.exists() && targetFile.length() > 0
    }

    fun getDownloadedPath(track: Track): String? {
        if (track.localPath != null && File(track.localPath).exists()) return track.localPath
        val targetFile = File(downloadsDir, "${track.id}.m4a")
        return if (targetFile.exists()) targetFile.absolutePath else null
    }

    suspend fun downloadTrack(
        track: Track,
        musicRepository: MusicRepository,
        onProgress: (Float) -> Unit = {}
    ): Result<Track> = withContext(Dispatchers.IO) {
        runCatching {
            val targetFile = File(downloadsDir, "${track.id}.m4a")
            if (targetFile.exists() && targetFile.length() > 0) {
                return@runCatching track.copy(localPath = targetFile.absolutePath)
            }

            val videoId = track.youtubeVideoId ?: track.id
            val streamUrl = musicRepository.getStreamUrl(videoId).getOrThrow()

            val conn = URL(streamUrl).openConnection()
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.connect()

            val totalBytes = conn.contentLengthLong
            var downloadedBytes = 0L

            conn.getInputStream().use { input ->
                targetFile.outputStream().use { output ->
                    val buffer = ByteArray(16384)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        downloadedBytes += read
                        if (totalBytes > 0) {
                            onProgress(downloadedBytes.toFloat() / totalBytes.toFloat())
                        }
                    }
                }
            }

            track.copy(localPath = targetFile.absolutePath)
        }
    }

    fun deleteDownloadedTrack(track: Track): Boolean {
        try {
            val file = File(downloadsDir, "${track.id}.m4a")
            if (file.exists()) return file.delete()
        } catch (ignored: Exception) {}
        return false
    }
}

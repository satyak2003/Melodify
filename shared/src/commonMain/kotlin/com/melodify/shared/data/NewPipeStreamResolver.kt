package com.melodify.shared.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.stream.AudioStream
import org.schabi.newpipe.extractor.stream.DeliveryMethod
import java.net.HttpURLConnection
import java.net.URL

/**
 * Stream resolver using NewPipe Extractor.
 * This handles YouTube's signature cipher decryption natively in JVM,
 * so it works on both Android and Desktop without needing yt-dlp.
 */
object NewPipeStreamResolver {
    private var initialized = false

    private fun ensureInitialized() {
        if (!initialized) {
            NewPipe.init(KtorDownloader)
            initialized = true
        }
    }

    /**
     * Resolves a working stream URL for the given YouTube video ID.
     * Returns null if resolution fails.
     */
    suspend fun getStreamUrl(videoId: String, preferM4a: Boolean = true): String? {
        return withContext(Dispatchers.IO) {
            try {
                ensureInitialized()
                val url = "https://music.youtube.com/watch?v=$videoId"
                val extractor = ServiceList.YouTube.getStreamExtractor(url)
                extractor.fetchPage()

                val audioStreams: List<AudioStream> = extractor.audioStreams
                    ?.filter { it.deliveryMethod == DeliveryMethod.PROGRESSIVE_HTTP }
                    ?: emptyList()

                if (audioStreams.isEmpty()) {
                    println("NewPipe: No progressive audio streams for $videoId")
                    return@withContext null
                }

                // Prefer m4a/AAC for maximum player compatibility
                val best = if (preferM4a) {
                    audioStreams
                        .filter { it.format?.mimeType?.contains("mp4") == true || it.format?.suffix == "m4a" }
                        .maxByOrNull { it.averageBitrate }
                        ?: audioStreams.maxByOrNull { it.averageBitrate }
                } else {
                    audioStreams.maxByOrNull { it.averageBitrate }
                }

                var streamUrl = best?.content
                if (streamUrl != null && best != null) {
                    println("NewPipe: Resolved stream for $videoId -> itag=${best.itag} format=${best.format?.mimeType} bitrate=${best.averageBitrate}")
                    // Append &file=audio.m4a for mp4/AAC streams so JavaFX Media recognizes the format
                    val isMp4 = best.format?.mimeType?.contains("mp4") == true || best.format?.suffix == "m4a"
                    if (isMp4 && !streamUrl.contains("&file=")) {
                        streamUrl = "$streamUrl&file=audio.m4a"
                    }
                }
                streamUrl
            } catch (e: Exception) {
                println("NewPipe: Failed to resolve stream for $videoId: ${e.message}")
                null
            }
        }
    }
}

/**
 * Simple HTTP downloader for NewPipe Extractor using java.net.HttpURLConnection.
 * NewPipe Extractor needs a Downloader implementation to fetch web pages.
 */
private object KtorDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = request.httpMethod()
        connection.connectTimeout = 10_000
        connection.readTimeout = 10_000
        connection.instanceFollowRedirects = true

        // Set headers
        request.headers().forEach { (key, values) ->
            values.forEach { value ->
                connection.addRequestProperty(key, value)
            }
        }
        // Default User-Agent
        if (connection.getRequestProperty("User-Agent") == null) {
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        }

        // Write body if present
        val dataToSend = request.dataToSend()
        if (dataToSend != null && dataToSend.isNotEmpty()) {
            connection.doOutput = true
            connection.outputStream.use { it.write(dataToSend) }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage ?: ""

        val responseHeaders = mutableMapOf<String, List<String>>()
        connection.headerFields?.forEach { (key, values) ->
            if (key != null) {
                responseHeaders[key] = values
            }
        }

        val responseBody = try {
            val inputStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream ?: connection.inputStream
            }
            inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            ""
        }

        connection.disconnect()

        return Response(
            responseCode,
            responseMessage,
            responseHeaders,
            responseBody,
            request.url()
        )
    }
}

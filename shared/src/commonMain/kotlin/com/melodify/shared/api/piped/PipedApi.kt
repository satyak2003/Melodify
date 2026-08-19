package com.melodify.shared.api.piped

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class PipedStreamResponse(
    val audioStreams: List<PipedAudioStream>? = null
)

@Serializable
data class PipedAudioStream(
    val url: String,
    val format: String,
    val quality: String,
    val bitrate: Int,
    val codec: String? = null
)

object PipedApi {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    // Community instance recommended for Music
    private const val BASE_URL = "https://pipedapi.kavin.rocks"

    suspend fun getStreamUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        // Piped strictly requires 11-character YouTube video IDs
        if (videoId.length != 11) {
            return@withContext null
        }
        
        try {
            val response = client.get("$BASE_URL/streams/$videoId") {
                header("User-Agent", "Mozilla/5.0")
            }
            
            val streamResponse = json.decodeFromString<PipedStreamResponse>(response.bodyAsText())
            val streams = streamResponse.audioStreams
            
            if (streams.isNullOrEmpty()) {
                println("Piped API: No audio streams found for $videoId")
                return@withContext null
            }
            
            // Try to find the highest bitrate m4a/aac stream
            val bestStream = streams.filter { it.format == "M4A" || it.codec == "mp4a.40.2" }
                .maxByOrNull { it.bitrate } ?: streams.maxByOrNull { it.bitrate }
                
            if (bestStream != null) {
                println("Piped API: Resolved stream for $videoId -> bitrate=${bestStream.bitrate} codec=${bestStream.codec}")
                return@withContext bestStream.url
            }
            null
        } catch (e: Exception) {
            println("Piped API Error for $videoId: ${e.message}")
            null
        }
    }
}

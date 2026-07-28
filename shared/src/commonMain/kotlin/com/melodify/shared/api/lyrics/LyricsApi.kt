package com.melodify.shared.api.lyrics

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import kotlinx.serialization.Serializable

@Serializable
data class LrcLibResponse(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String?,
    val duration: Int,
    val plainLyrics: String?,
    val syncedLyrics: String?
)

data class Lyrics(
    val plainLyrics: String?,
    val syncedLyrics: List<LyricLine>?
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

class LyricsApi(private val httpClient: HttpClient) {
    
    suspend fun getLyrics(title: String, artist: String, album: String? = null, durationSeconds: Int? = null): Lyrics? {
        return try {
            val url = URLBuilder("https://lrclib.net/api/get").apply {
                parameters.append("track_name", title)
                parameters.append("artist_name", artist)
                if (album != null) parameters.append("album_name", album)
                if (durationSeconds != null) parameters.append("duration", durationSeconds.toString())
            }.buildString()
            
            val response: LrcLibResponse = httpClient.get(url).body()
            
            val synced = response.syncedLyrics?.let { parseSyncedLyrics(it) }
            Lyrics(plainLyrics = response.plainLyrics, syncedLyrics = synced)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun parseSyncedLyrics(lrcText: String): List<LyricLine> {
        val regex = Regex("\\[(\\d+):(\\d+)\\.(\\d+)]\\s*(.*)")
        return lrcText.lines().mapNotNull { line ->
            val match = regex.find(line) ?: return@mapNotNull null
            val (min, sec, hundredths, text) = match.destructured
            val timestampMs = (min.toLong() * 60 * 1000) + (sec.toLong() * 1000) + (hundredths.toLong() * 10)
            LyricLine(timestampMs, text)
        }
    }
}

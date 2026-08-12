package com.melodify.shared.data

import com.melodify.shared.api.innertube.InnerTubeApi
import com.melodify.shared.api.innertube.InnerTubeParser
import com.melodify.shared.domain.model.SearchResult
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.math.abs
import com.melodify.shared.domain.model.YouTubePlaylist
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import com.melodify.shared.data.storage.YouTubeAuthManager

class MusicRepository(
    private val innerTubeApi: InnerTubeApi,
    private val innerTubeParser: InnerTubeParser
) {

    suspend fun search(query: String): Result<SearchResult> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.search(query)
            innerTubeParser.parseSearchResults(response)
        }
    }

    suspend fun getStreamUrl(videoId: String): Result<String> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getPlayerInfo(videoId)
            innerTubeParser.parseBestStreamUrl(response) 
                ?: throw Exception("No suitable streaming URL found for videoId: $videoId")
        }
    }

    suspend fun getTrackDetails(videoId: String): Result<Track> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getPlayerInfo(videoId)
            innerTubeParser.parseVideoDetails(response)
                ?: throw Exception("Could not parse video details for videoId: $videoId")
        }
    }

    suspend fun getHomeFeed(): Result<List<Track>> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getHomeFeed()
            innerTubeParser.parseBrowseResults(response)
        }
    }

    suspend fun getYouTubePlaylistTracks(playlistId: String): Result<List<Track>> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getYouTubePlaylist(playlistId)
            innerTubeParser.parseBrowseResults(response)
        }
    }

    suspend fun getRelatedTracks(videoId: String, playlistId: String? = null): Result<List<Track>> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getRelatedSongs(videoId, playlistId)
            innerTubeParser.parseNextResults(response)
        }
    }
    
    suspend fun fetchUserPlaylists(accessToken: String? = null): Result<List<YouTubePlaylist>> = runCatching {
        withContext(Dispatchers.IO) {
            // First try InnerTube API if YouTube cookies are available (works for YouTube Music)
            if (YouTubeAuthManager.getCookieHeader() != null) {
                try {
                    return@withContext innerTubeApi.getUserPlaylists()
                } catch (e: Exception) {
                    println("InnerTube getUserPlaylists failed, falling back to YouTube Data API: ${e.message}")
                }
            }
            
            // Fallback to YouTube Data API v3 if OAuth token provided
            if (accessToken != null && accessToken.isNotBlank()) {
                val client = HttpClient()
                val playlists = mutableListOf<YouTubePlaylist>()
                var nextPageToken: String? = null
                
                do {
                    val url = buildString {
                        append("https://www.googleapis.com/youtube/v3/playlists?mine=true&part=snippet&maxResults=50")
                        if (nextPageToken != null) append("&pageToken=$nextPageToken")
                    }
                    
                    val response = client.get(url) {
                        header("Authorization", "Bearer $accessToken")
                        header("Accept", "application/json")
                    }
                    val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText()).jsonObject
                    val items = json["items"]?.jsonArray ?: emptyList()
                    
                    val pagePlaylists = items.mapNotNull { item ->
                        val id = item.jsonObject["id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                        val snippet = item.jsonObject["snippet"]?.jsonObject
                        val title = snippet?.get("title")?.jsonPrimitive?.content ?: ""
                        val thumbnailUrl = snippet?.get("thumbnails")?.jsonObject?.get("medium")?.jsonObject?.get("url")?.jsonPrimitive?.content
                            ?: snippet?.get("thumbnails")?.jsonObject?.get("default")?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                        YouTubePlaylist(id, title, thumbnailUrl)
                    }
                    playlists.addAll(pagePlaylists)
                    nextPageToken = json["nextPageToken"]?.jsonPrimitive?.content
                } while (nextPageToken != null)
                
                client.close()
                playlists
            } else {
                throw Exception("No authentication available for fetching YouTube playlists. Please sign in with Google or provide YouTube cookies.")
            }
        }
    }

    suspend fun matchSpotifyTrack(title: String, artist: String, durationMs: Long): Result<Track> = runCatching {
        withContext(Dispatchers.IO) {
            val query = "$title $artist".trim()
            val searchResult = innerTubeApi.search(query)
            val parsedResult = innerTubeParser.parseSearchResults(searchResult)
            val durationSeconds = (durationMs / 1000).toInt()

            if (parsedResult.tracks.isNotEmpty()) {
                val bestMatch = if (durationSeconds > 0) {
                    parsedResult.tracks.minByOrNull { track -> abs(track.durationSeconds - durationSeconds) }
                } else null
                return@withContext bestMatch ?: parsedResult.tracks.first()
            }

            // Fallback search with title only if title + artist returned 0 results
            val fallbackResult = innerTubeApi.search(title)
            val fallbackParsed = innerTubeParser.parseSearchResults(fallbackResult)
            fallbackParsed.tracks.firstOrNull()
                ?: throw Exception("No matching track found for: $query")
        }
    }
}

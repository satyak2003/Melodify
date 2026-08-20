package com.melodify.shared.data

import com.melodify.shared.api.deezer.DeezerApi
import com.melodify.shared.api.innertube.InnerTubeApi
import com.melodify.shared.api.innertube.InnerTubeParser
import com.melodify.shared.domain.model.SearchResult
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.Artist
import com.melodify.shared.domain.model.Album
import com.melodify.shared.domain.model.TrackSource
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
    private val innerTubeParser: InnerTubeParser,
    private val deezerApi: DeezerApi
) {

    suspend fun search(query: String): Result<SearchResult> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.search(query)
            val parsedResult = innerTubeParser.parseSearchResults(response)
            
            // Enhance tracks with Deezer metadata
            val enhancedTracks = parsedResult.tracks.map { track ->
                enhanceTrack(track)
            }
            
            parsedResult.copy(tracks = enhancedTracks)
        }
    }

    suspend fun getStreamUrl(videoId: String, fallbackTitle: String? = null, fallbackArtist: String? = null): Result<String> = runCatching {
        // 1. Try JioSaavn First (Most stable, 320kbps MP4 direct streams)
        if (fallbackTitle != null) {
            val artist = fallbackArtist ?: ""
            try {
                val saavnUrl = com.melodify.shared.api.jiosaavn.JioSaavnApi.getStreamUrl(fallbackTitle, artist)
                if (saavnUrl != null) return@runCatching saavnUrl
            } catch (e: Exception) {
                println("JioSaavn fetch failed: ${e.message}")
            }
        }

        // Only attempt YouTube stream resolution if the ID is a valid YouTube Video ID (11 chars)
        if (videoId.length == 11) {
            // 2. Try Piped API (Community hosted YouTube wrapper, avoids cipher decryption locally)
            try {
                val pipedUrl = com.melodify.shared.api.piped.PipedApi.getStreamUrl(videoId)
                if (pipedUrl != null) return@runCatching pipedUrl
            } catch (e: Exception) {
                println("Piped API failed: ${e.message}")
            }

            // 3. Try NewPipe Extractor (Works on Android + Desktop, handles cipher cracking natively)
            try {
                val newPipeUrl = NewPipeStreamResolver.getStreamUrl(videoId, preferM4a = true)
                if (newPipeUrl != null) return@runCatching newPipeUrl
            } catch (e: Exception) {
                println("NewPipe failed for $videoId: ${e.message}")
            }

            // 4. Try InnerTube API directly (may work for some videos)
            try {
                val response = innerTubeApi.getPlayerInfo(videoId)
                val url = innerTubeParser.parseBestStreamUrl(response)
                if (url != null) return@runCatching url
            } catch (e: Exception) {
                println("InnerTubeApi failed for $videoId: ${e.message}")
            }

            // 5. Try platform-specific stream resolver as fallback (yt-dlp on desktop)
            val platformUrl = platformResolveStreamUrl(videoId)
            if (platformUrl != null) return@runCatching platformUrl
        } else {
            println("Skipping YouTube stream resolution for non-YouTube ID: $videoId")
        }
        
        // 4. Fallback: search for an alternative video ID and try NewPipe again
        if (fallbackTitle != null) {
            val artist = fallbackArtist ?: ""
            val query = "$fallbackTitle $artist lyrics".trim()
            try {
                val searchResult = innerTubeApi.searchVideo(query)
                val parsedResult = innerTubeParser.parseSearchResults(searchResult)
                val standardVideoId = parsedResult.tracks.firstOrNull()?.id
                
                if (standardVideoId != null && standardVideoId != videoId) {
                    // Try Piped API for the fallback video
                    val pipedFallback = com.melodify.shared.api.piped.PipedApi.getStreamUrl(standardVideoId)
                    if (pipedFallback != null) return@runCatching pipedFallback

                    // Try NewPipe for the fallback video
                    val newPipeFallback = NewPipeStreamResolver.getStreamUrl(standardVideoId, preferM4a = true)
                    if (newPipeFallback != null) return@runCatching newPipeFallback

                    // Try platform resolver for the fallback video
                    val platformFallback = platformResolveStreamUrl(standardVideoId)
                    if (platformFallback != null) return@runCatching platformFallback
                }
            } catch (e: Exception) {
                println("Fallback search failed: ${e.message}")
            }
        }
        
        throw Exception("No suitable streaming URL found for videoId: $videoId")
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
            innerTubeParser.parseBrowseResults(response).tracks
        }
    }

    suspend fun getYouTubePlaylistTracks(playlistId: String, accessToken: String? = null): Result<List<Track>> = runCatching {
        withContext(Dispatchers.IO) {
            val allTracks = mutableListOf<Track>()
            
            // If we have an OAuth token, use YouTube Data API v3
            if (accessToken != null && accessToken.isNotBlank()) {
                val client = HttpClient()
                var nextPageToken: String? = null
                
                do {
                    val url = buildString {
                        append("https://www.googleapis.com/youtube/v3/playlistItems?part=snippet,contentDetails&maxResults=50&playlistId=$playlistId")
                        if (nextPageToken != null) append("&pageToken=$nextPageToken")
                    }
                    val response = client.get(url) {
                        header("Authorization", "Bearer $accessToken")
                        header("Accept", "application/json")
                    }
                    if (response.status.value in 200..299) {
                        try {
                            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText()).jsonObject
                            val items = json["items"]?.jsonArray ?: emptyList()
                            
                            val pageTracks = items.mapNotNull { item ->
                                val snippet = item.jsonObject["snippet"]?.jsonObject
                                val contentDetails = item.jsonObject["contentDetails"]?.jsonObject
                                val videoId = contentDetails?.get("videoId")?.jsonPrimitive?.content
                                val title = snippet?.get("title")?.jsonPrimitive?.content ?: ""
                                val author = snippet?.get("videoOwnerChannelTitle")?.jsonPrimitive?.content ?: "Unknown Artist"
                                val thumbnailUrl = snippet?.get("thumbnails")?.jsonObject?.get("high")?.jsonObject?.get("url")?.jsonPrimitive?.content
                                    ?: snippet?.get("thumbnails")?.jsonObject?.get("default")?.jsonObject?.get("url")?.jsonPrimitive?.content ?: ""
                                    
                                if (videoId != null && title.isNotBlank() && title != "Private video" && title != "Deleted video") {
                                    Track(
                                        id = videoId,
                                        title = title,
                                        artists = listOf(Artist(id = author, name = author, thumbnailUrl = null)),
                                        album = null,
                                        thumbnailUrl = thumbnailUrl,
                                        durationMs = 200000L, // Placeholder duration
                                        youtubeVideoId = videoId,
                                        source = TrackSource.YOUTUBE
                                    )
                                } else null
                            }
                            allTracks.addAll(pageTracks)
                            nextPageToken = json["nextPageToken"]?.jsonPrimitive?.content
                        } catch (e: Exception) {
                            println("JSON Parsing error in playlistItems: ${e.message}")
                            break // Stop pagination on error
                        }
                    } else {
                        println("YouTube API Error in playlistItems: ${response.status.value} - ${response.bodyAsText()}")
                        break // Fallback or stop if error
                    }
                } while (nextPageToken != null)
                
                client.close()
                // If we successfully authenticated, return the tracks (even if empty) to avoid InnerTube fallback crashing on private playlists
                return@withContext allTracks
            }
            
            // Fallback to InnerTube API (capped at 100 anonymously)
            var response = innerTubeApi.getYouTubePlaylist(playlistId)
            var parsed = innerTubeParser.parseBrowseResults(response)
            allTracks.addAll(parsed.tracks)
            
            // Fetch all pages, max out at ~1000 tracks to avoid infinite loops or memory issues
            while (parsed.continuationToken != null && allTracks.size < 1000) {
                try {
                    response = innerTubeApi.getYouTubePlaylistContinuation(parsed.continuationToken!!)
                    parsed = innerTubeParser.parseBrowseResults(response)
                    if (parsed.tracks.isEmpty()) break
                    allTracks.addAll(parsed.tracks)
                } catch (e: Exception) {
                    println("Failed to fetch continuation: ${e.message}")
                    break
                }
            }
            
            // Enhance with Deezer best-effort
            allTracks.map { enhanceTrack(it) }
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
                    if (response.status.value !in 200..299) {
                        throw Exception("YouTube API Error: ${response.status.value} - ${response.bodyAsText()}")
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
            val searchResult = innerTubeApi.searchVideo(query)
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

    suspend fun enhanceTrack(track: Track): Track {
        try {
            val query = "${track.title} ${track.artists.firstOrNull()?.name ?: ""}".trim()
            val deezerResult = deezerApi.searchTrack(query, limit = 1)
            val bestMatch = deezerResult?.data?.firstOrNull()
            
            if (bestMatch != null) {
                return track.copy(
                    title = bestMatch.title,
                    artists = listOf(Artist(id = bestMatch.artist.id.toString(), name = bestMatch.artist.name, thumbnailUrl = bestMatch.artist.picture_xl)),
                    album = Album(id = bestMatch.album.id.toString(), title = bestMatch.album.title, thumbnailUrl = bestMatch.album.cover_xl),
                    thumbnailUrl = bestMatch.album.cover_xl ?: track.thumbnailUrl,
                    durationMs = bestMatch.duration * 1000L
                )
            } else {
                // Fallback to InnerTube
                val fallbackResult = innerTubeApi.search(query)
                val fallbackParsed = innerTubeParser.parseSearchResults(fallbackResult)
                val ytMatch = fallbackParsed.tracks.firstOrNull()
                if (ytMatch != null) {
                    return track.copy(
                        title = ytMatch.title,
                        artists = ytMatch.artists,
                        album = ytMatch.album,
                        thumbnailUrl = ytMatch.thumbnailUrl ?: track.thumbnailUrl,
                        durationMs = ytMatch.durationMs
                    )
                }
            }
        } catch (e: Exception) {
            println("Deezer/InnerTube enhancement failed for ${track.title}: ${e.message}")
        }
        return track
    }
}

package com.melodify.shared.data

import com.melodify.shared.api.innertube.InnerTubeApi
import com.melodify.shared.api.innertube.InnerTubeParser
import com.melodify.shared.domain.model.SearchResult
import com.melodify.shared.domain.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.math.abs

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

    suspend fun getRelatedTracks(videoId: String, playlistId: String? = null): Result<List<Track>> = runCatching {
        withContext(Dispatchers.IO) {
            val response = innerTubeApi.getRelatedSongs(videoId, playlistId)
            innerTubeParser.parseNextResults(response)
        }
    }

    suspend fun matchSpotifyTrack(title: String, artist: String, durationMs: Long): Result<Track> = runCatching {
        withContext(Dispatchers.IO) {
            val query = "$title $artist"
            val searchResult = innerTubeApi.search(query)
            val parsedResult = innerTubeParser.parseSearchResults(searchResult)
            
            val durationSeconds = (durationMs / 1000).toInt()
            
            // Try to find the closest match by duration
            val bestMatch = parsedResult.tracks.minByOrNull { track ->
                abs(track.durationSeconds - durationSeconds)
            }
            
            bestMatch ?: throw Exception("No matching track found for: $query")
        }
    }
}

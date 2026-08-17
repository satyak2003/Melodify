package com.melodify.shared.api.spotify

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SpotifyApi(private val httpClient: HttpClient) {

    suspend fun getPlaylist(playlistId: String): Pair<SpotifyPlaylist, List<SpotifyTrack>> {
        val response = httpClient.get("https://open.spotify.com/embed/playlist/$playlistId")
        if (!response.status.isSuccess()) {
            throw Exception("Failed to fetch playlist embed page")
        }
        val html = response.bodyAsText()

        val startIndex = html.indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")
        if (startIndex == -1) throw Exception("__NEXT_DATA__ JSON not found in embed HTML")
        
        val jsonStart = html.indexOf(">", startIndex) + 1
        val jsonEnd = html.indexOf("</script>", jsonStart)
        val jsonStr = html.substring(jsonStart, jsonEnd)

        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.parseToJsonElement(jsonStr).jsonObject

        val tracksList = mutableListOf<SpotifyTrack>()
        var playlistName = "Spotify Playlist"
        var playlistDesc = ""
        var playlistImageUrl = ""

        try {
            val props = parsed["props"]?.jsonObject
            val pageProps = props?.get("pageProps")?.jsonObject
            val state = pageProps?.get("state")?.jsonObject
            val data = state?.get("data")?.jsonObject
            val entity = data?.get("entity")?.jsonObject
            
            // Extract Playlist Info
            playlistName = entity?.get("name")?.toString()?.replace("\"", "") ?: "Spotify Playlist"
            playlistDesc = entity?.get("description")?.toString()?.replace("\"", "") ?: ""
            playlistImageUrl = entity?.get("coverArt")?.jsonObject?.get("sources")?.jsonArray?.firstOrNull()?.jsonObject?.get("url")?.toString()?.replace("\"", "") ?: ""

            val trackList = entity?.get("trackList")?.jsonArray

            if (trackList != null) {
                for (item in trackList) {
                    val itemObj = item.jsonObject
                    // The old 'id' field is gone. Extract from 'uri' e.g. spotify:track:3ouNEk0tv5TTi8VWMe1xbX
                    var id = itemObj["id"]?.toString()?.replace("\"", "")
                    if (id == null || id == "null") {
                        val uri = itemObj["uri"]?.toString()?.replace("\"", "")
                        if (uri != null && uri.contains("spotify:track:")) {
                            id = uri.substringAfterLast(":")
                        }
                    }
                    val title = itemObj["title"]?.toString()?.replace("\"", "")
                    val subtitle = itemObj["subtitle"]?.toString()?.replace("\"", "")
                    val duration = itemObj["duration"]?.toString()?.toLongOrNull()

                    if (title != null && id != null) {
                        tracksList.add(
                            SpotifyTrack(
                                id = id,
                                name = title,
                                artists = listOf(SpotifyArtist(name = subtitle)),
                                durationMs = duration
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw Exception("Error parsing Spotify embed JSON: ${e.message}")
        }
        
        val playlist = SpotifyPlaylist(
            id = playlistId,
            name = playlistName,
            description = playlistDesc,
            images = if (playlistImageUrl.isNotBlank()) listOf(SpotifyImage(url = playlistImageUrl)) else emptyList()
        )

        return Pair(playlist, tracksList)
    }
}
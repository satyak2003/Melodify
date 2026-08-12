package com.melodify.shared.api.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class SpotifyApi(private val httpClient: HttpClient) {
    var accessToken: String? = null

    fun hasValidToken(): Boolean = accessToken != null

    private var refreshTokenCallback: ((String) -> Unit)? = null
    private var tokenRefreshInProgress = false

    fun setRefreshTokenCallback(callback: (String) -> Unit) {
        refreshTokenCallback = callback
    }

    private suspend inline fun <reified T> getWithAuth(url: String): T {
        val token = accessToken ?: throw IllegalStateException("No access token available")
        var attempts = 0
        while (true) {
            try {
                val response = httpClient.get(url) {
                    header(HttpHeaders.Authorization, "Bearer $token")
                }
                return response.body()
            } catch (e: Exception) {
                val statusCode = when {
                    e.message?.contains("401") == true -> HttpStatusCode.Unauthorized
                    e.message?.contains("403") == true -> HttpStatusCode.Forbidden
                    e.message?.contains("429") == true -> HttpStatusCode.TooManyRequests
                    else -> null
                }

                if (statusCode == HttpStatusCode.Unauthorized || statusCode == HttpStatusCode.Forbidden) {
                    if (!tokenRefreshInProgress) {
                        tryRefreshToken()
                    }
                    throw e
                }

                if (statusCode == HttpStatusCode.TooManyRequests) {
                    attempts++
                    if (attempts > 5) throw e
                    delay(2000L * attempts)
                } else {
                    throw e
                }
            }
        }
    }

    private fun tryRefreshToken() {
        if (tokenRefreshInProgress) return
        val callback = refreshTokenCallback
        if (callback != null) {
            tokenRefreshInProgress = true
            callback("")
            tokenRefreshInProgress = false
        }
    }

    suspend fun getUserProfile(): SpotifyUser {
        return getWithAuth("https://api.spotify.com/v1/me")
    }

    suspend fun getUserPlaylists(limit: Int = 50): List<SpotifyPlaylist> {
        val response: SpotifyPlaylistsResponse = getWithAuth("https://api.spotify.com/v1/me/playlists?limit=$limit")
        return response.items.filterNotNull()
    }

    suspend fun getPlaylist(playlistId: String): SpotifyPlaylist {
        return getWithAuth("https://api.spotify.com/v1/playlists/$playlistId")
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50, offset: Int = 0): SpotifyPlaylistTracksResponse {
        return getWithAuth("https://api.spotify.com/v1/playlists/$playlistId/items?limit=$limit&offset=$offset")
    }

    suspend fun getAllPlaylistTracks(playlistId: String): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 100 // Spotify API max limit per request
        while (true) {
            val response = getPlaylistTracks(playlistId, limit, offset)
            tracks.addAll(response.items.mapNotNull { it?.track })
            if (response.next == null) break
            offset += limit
            delay(150L) // Throttle requests to avoid rate limits
        }
        return tracks
    }

    suspend fun getSavedTracks(limit: Int = 50, offset: Int = 0): List<SpotifyTrack> {
        val response: Paging<SpotifyPlaylistItem> = getWithAuth("https://api.spotify.com/v1/me/tracks?limit=$limit&offset=$offset")
        return response.items.mapNotNull { it.track }
    }

    suspend fun exchangeCodeForToken(code: String, codeVerifier: String, redirectUri: String, clientId: String): SpotifyToken {
        val response = httpClient.submitForm(
            url = "https://accounts.spotify.com/api/token",
            formParameters = Parameters.build {
                append("client_id", clientId)
                append("grant_type", "authorization_code")
                append("code", code)
                append("redirect_uri", redirectUri)
                append("code_verifier", codeVerifier)
            }
        )
        return response.body()
    }

    suspend fun refreshToken(refreshToken: String, clientId: String): SpotifyToken {
        val response = httpClient.submitForm(
            url = "https://accounts.spotify.com/api/token",
            formParameters = Parameters.build {
                append("client_id", clientId)
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
            }
        )
        return response.body()
    }

    suspend fun getPlaylistTracksFromEmbed(playlistId: String): List<SpotifyTrack> {
        val response = httpClient.get("https://open.spotify.com/embed/playlist/$playlistId")
        val html = response.bodyAsText()

        val startIndex = html.indexOf("<script id=\"__NEXT_DATA__\" type=\"application/json\">")
        if (startIndex == -1) return emptyList()
        val jsonStart = html.indexOf(">", startIndex) + 1
        val jsonEnd = html.indexOf("</script>", jsonStart)
        val jsonStr = html.substring(jsonStart, jsonEnd)

        val json = Json { ignoreUnknownKeys = true }
        val parsed = json.parseToJsonElement(jsonStr).jsonObject

        val tracksList = mutableListOf<SpotifyTrack>()
        try {
            val props = parsed["props"]?.jsonObject
            val pageProps = props?.get("pageProps")?.jsonObject
            val state = pageProps?.get("state")?.jsonObject
            val data = state?.get("data")?.jsonObject
            val entity = data?.get("entity")?.jsonObject
            val trackList = entity?.get("trackList")?.jsonArray

            if (trackList != null) {
                for (item in trackList) {
                    val itemObj = item.jsonObject
                    val id = itemObj["id"]?.toString()?.replace("\"", "")
                    val title = itemObj["title"]?.toString()?.replace("\"", "")
                    val subtitle = itemObj["subtitle"]?.toString()?.replace("\"", "")
                    val duration = itemObj["duration"]?.toString()?.toLongOrNull()

                    if (title != null) {
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
        }
        return tracksList
    }
}
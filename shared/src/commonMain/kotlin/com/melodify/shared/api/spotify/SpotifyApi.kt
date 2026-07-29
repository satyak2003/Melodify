package com.melodify.shared.api.spotify

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters

class SpotifyApi(private val httpClient: HttpClient) {
    var accessToken: String? = null

    private suspend inline fun <reified T> get(url: String): T {
        val token = accessToken ?: throw IllegalStateException("No access token available")
        val response = httpClient.get(url) {
            header(HttpHeaders.Authorization, "Bearer $token")
        }
        return response.body()
    }

    suspend fun getUserProfile(): SpotifyUser {
        return get("https://api.spotify.com/v1/me")
    }

    suspend fun getUserPlaylists(limit: Int = 50): List<SpotifyPlaylist> {
        val response: SpotifyPlaylistsResponse = get("https://api.spotify.com/v1/me/playlists?limit=$limit")
        return response.items
    }

    suspend fun getPlaylist(playlistId: String): SpotifyPlaylist {
        return get("https://api.spotify.com/v1/playlists/$playlistId")
    }

    suspend fun getPlaylistTracks(playlistId: String, limit: Int = 50, offset: Int = 0): SpotifyPlaylistTracksResponse {
        return get("https://api.spotify.com/v1/playlists/$playlistId/tracks?limit=$limit&offset=$offset")
    }


    suspend fun getAllPlaylistTracks(playlistId: String): List<SpotifyTrack> {
        val tracks = mutableListOf<SpotifyTrack>()
        var offset = 0
        val limit = 50
        while (true) {
            val response = getPlaylistTracks(playlistId, limit, offset)
            tracks.addAll(response.items.mapNotNull { it.track })
            if (response.next == null) break
            offset += limit
        }
        return tracks
    }

    suspend fun getSavedTracks(limit: Int = 50, offset: Int = 0): List<SpotifyTrack> {
        val response: Paging<SpotifyPlaylistItem> = get("https://api.spotify.com/v1/me/tracks?limit=$limit&offset=$offset")
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
}

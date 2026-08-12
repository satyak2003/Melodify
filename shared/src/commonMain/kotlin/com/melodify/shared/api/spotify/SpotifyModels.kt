package com.melodify.shared.api.spotify

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Paging<T>(
    val items: List<T>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: String? = null
)

@Serializable
data class SpotifyPlaylistsResponse(val items: List<SpotifyPlaylist?>)

@Serializable
data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    @SerialName("tracks") val tracksInfo: SpotifyTracksInfo? = null,
    @SerialName("snapshot_id") val snapshotId: String = ""
)

@Serializable
data class SpotifyTracksInfo(
    val total: Int = 0,
    val items: List<SpotifyPlaylistItem?> = emptyList()
)

@Serializable
data class SpotifyImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null
)

@Serializable
data class SpotifyPlaylistTracksResponse(
    val items: List<SpotifyPlaylistItem?> = emptyList(),
    val total: Int = 0,
    val next: String? = null
)

@Serializable
data class SpotifyPlaylistItem(
    // New /items endpoint uses "item" key; old /tracks used "track"
    @SerialName("item") val item: SpotifyTrack? = null,
    @SerialName("track") val trackLegacy: SpotifyTrack? = null,
    @SerialName("is_local") val isLocal: Boolean = false
) {
    // Use whichever key is present
    val track: SpotifyTrack? get() = item ?: trackLegacy
}

@Serializable
data class SpotifyTrack(
    val id: String? = null,
    val name: String? = null,
    val artists: List<SpotifyArtist?>? = emptyList(),
    val album: SpotifyAlbum? = null,
    @SerialName("duration_ms") val durationMs: Long? = 0,
    val explicit: Boolean? = false,
    val popularity: Int? = 0
)

@Serializable
data class SpotifyArtist(
    val id: String? = null, 
    val name: String? = null
)

@Serializable
data class SpotifyAlbum(
    val id: String? = null,
    val name: String? = null,
    val images: List<SpotifyImage?>? = emptyList(),
    @SerialName("release_date") val releaseDate: String? = null
)

@Serializable
data class SpotifyToken(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresIn: Int,
    @SerialName("refresh_token") val refreshToken: String? = null,
    @SerialName("scope") val scope: String? = null
)

@Serializable
data class SpotifyUser(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
    val email: String? = null,
    val images: List<SpotifyImage?> = emptyList()
)

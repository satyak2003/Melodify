package com.melodify.shared.domain.model

import kotlinx.serialization.Serializable

/**
 * Represents a single music track.
 * Can originate from YouTube Music search results, Spotify metadata, or local storage.
 */
@Serializable
data class Track(
    val id: String,
    val title: String,
    val artists: List<Artist>,
    val album: Album?,
    val thumbnailUrl: String?,
    val durationMs: Long,
    val durationSeconds: Int = (durationMs / 1000).toInt(),
    val source: TrackSource = TrackSource.YOUTUBE,
    val localPath: String? = null, // Set when track is a local file
    val spotifyId: String? = null, // Set when imported from Spotify
    val youtubeVideoId: String? = null, // Set when matched to YouTube
    val isExplicit: Boolean = false,
    val isLiked: Boolean = false,
) {
    val artistNames: String get() = artists.joinToString(", ") { it.name }
    /** Single artist string alias used by some APIs */
    val artist: String get() = artists.firstOrNull()?.name ?: ""
    val isLocal: Boolean get() = localPath != null
    val isFlac: Boolean get() = localPath?.endsWith(".flac", ignoreCase = true) == true
}

@Serializable
enum class TrackSource {
    YOUTUBE,   // Streamed from YouTube Music
    SPOTIFY,   // Metadata from Spotify, audio from YouTube
    LOCAL      // Local file on device/desktop
}



@Serializable
data class Artist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val browseId: String? = null, // YouTube Music browse ID
)

@Serializable
data class Album(
    val id: String,
    val title: String,
    val thumbnailUrl: String? = null,
    val year: Int? = null,
    val browseId: String? = null,
)

@Serializable
data class Playlist(
    val id: String,
    val title: String,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val trackCount: Int = 0,
    val tracks: List<Track> = emptyList(),
    val source: PlaylistSource = PlaylistSource.LOCAL,
    val spotifyId: String? = null,
)

@Serializable
enum class PlaylistSource {
    LOCAL,
    SPOTIFY,
    YOUTUBE,
    JELLYFIN
}

@Serializable
data class Lyrics(
    val trackId: String,
    val plainText: String?,
    val syncedLines: List<LyricLine>?,
    val isSynced: Boolean = syncedLines != null,
)

@Serializable
data class LyricLine(
    val timestampMs: Long,
    val text: String,
)

@Serializable
data class SearchResult(
    val tracks: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

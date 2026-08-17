package com.melodify.shared.api.deezer

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeezerSearchResponse(
    val data: List<DeezerTrack> = emptyList(),
    val total: Int = 0
)

@Serializable
data class DeezerTrack(
    val id: Long,
    val title: String,
    val duration: Int = 0,
    val explicit_lyrics: Boolean = false,
    val artist: DeezerArtist,
    val album: DeezerAlbum
)

@Serializable
data class DeezerArtist(
    val id: Long,
    val name: String,
    val picture_xl: String? = null
)

@Serializable
data class DeezerAlbum(
    val id: Long,
    val title: String,
    val cover_xl: String? = null
)

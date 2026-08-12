package com.melodify.shared.api.innertube

import com.melodify.shared.domain.model.YouTubePlaylist
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.parameter
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

import com.melodify.shared.data.storage.YouTubeAuthManager
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

class InnerTubeApi(private val httpClient: HttpClient) {
    private var visitorData: String? = null

    private fun HttpRequestBuilder.applyAuth() {
        val cookie = YouTubeAuthManager.getCookieHeader()
        if (cookie != null) {
            header("Cookie", cookie)
            val auth = YouTubeAuthManager.getAuthHeader()
            if (auth != null) {
                header("Authorization", auth)
            }
        }
    }

    suspend fun getVisitorData(): String {
        visitorData?.let { return it }
        try {
            val response = httpClient.post("${InnerTubeConstants.BASE_URL}music/get_search_suggestions") {
                parameter("key", InnerTubeConstants.API_KEY)
                contentType(ContentType.Application.Json)
                applyAuth()
                setBody(InnerTubeRequest(context = buildContext(isAndroid = false)))
            }.body<JsonObject>()
            val vData = response["responseContext"]?.jsonObject?.get("visitorData")?.jsonPrimitive?.content
            if (!vData.isNullOrBlank()) {
                visitorData = vData
                return vData
            }
        } catch (e: Exception) {
            // Ignore error and use fallback
        }
        val fallback = "CgtMZXRQc19VdmFZWSjH6aLTBjIKCgJJThIEGgAgGA%3D%3D"
        visitorData = fallback
        return fallback
    }

    suspend fun search(query: String): SearchResponse {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            query = query
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}search") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getPlayerInfo(videoId: String): PlayerResponse {
        val vData = getVisitorData()

        // 1. Try WEB_REMIX client first for pure music streams
        try {
            val webRequest = InnerTubeRequest(
                context = buildContext(isAndroid = false, visitorData = vData),
                videoId = videoId
            )
            val response = httpClient.post("${InnerTubeConstants.BASE_URL}player") {
                parameter("key", InnerTubeConstants.API_KEY)
                contentType(ContentType.Application.Json)
                applyAuth()
                setBody(webRequest)
            }.body<PlayerResponse>()

            val hasDirectUrl = response.streamingData?.adaptiveFormats?.any { it.url != null } == true ||
                    response.streamingData?.formats?.any { it.url != null } == true
            if (hasDirectUrl) return response
        } catch (e: Exception) {
            // Fallback to Android client
        }

        // 2. Fallback to ANDROID_VR client
        val androidRequest = InnerTubeRequest(
            context = buildContext(isAndroid = true, visitorData = vData),
            videoId = videoId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}player") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(androidRequest)
        }.body()
    }

    suspend fun getHomeFeed(): BrowseResponse {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            browseId = "FEmusic_home"
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}browse") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getArtistPage(browseId: String): BrowseResponse {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            browseId = browseId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}browse") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getAlbumPage(browseId: String): BrowseResponse {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            browseId = browseId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}browse") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getYouTubePlaylist(playlistId: String): BrowseResponse {
        val targetBrowseId = if (playlistId.startsWith("VL")) playlistId else "VL$playlistId"
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            browseId = targetBrowseId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}browse") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getRelatedSongs(videoId: String, playlistId: String?): NextResponse {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            videoId = videoId,
            playlistId = playlistId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}next") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body()
    }

    suspend fun getUserPlaylists(): List<YouTubePlaylist> {
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = false),
            browseId = "FEmusic_library_playlists"
        )
        val response = httpClient.post("${InnerTubeConstants.BASE_URL}browse") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            applyAuth()
            setBody(request)
        }.body<BrowseResponse>()

        val playlists = mutableListOf<YouTubePlaylist>()
        val sectionListContents = response.contents?.singleColumnBrowseResultsRenderer?.tabs?.firstOrNull()?.tabRenderer?.content?.sectionListRenderer?.contents

        sectionListContents?.forEach { sectionContent ->
            sectionContent.itemSectionRenderer?.contents?.forEach { itemSectionContent ->
                itemSectionContent.musicTwoRowItemRenderer?.let { renderer ->
                    val playlistId = renderer.navigationEndpoint?.browseEndpoint?.browseId
                    val title = renderer.title?.runs?.firstOrNull()?.text
                    val subtitle = renderer.subtitle?.runs?.firstOrNull()?.text
                    val thumbnailUrl = renderer.thumbnail?.musicThumbnailRenderer?.thumbnail?.thumbnails?.maxByOrNull { it.width ?: 0 }?.url

                    if (playlistId != null && title != null) {
                        playlists.add(YouTubePlaylist(playlistId, title, thumbnailUrl ?: ""))
                    }
                }
            }
        }
        return playlists
    }

    private fun buildContext(isAndroid: Boolean = false, visitorData: String? = null): InnerTubeContext {
        val clientData = if (isAndroid) InnerTubeConstants.ANDROID_VR_CLIENT else InnerTubeConstants.WEB_REMIX_CLIENT
        return InnerTubeContext(
            client = InnerTubeClient(
                clientName = clientData["clientName"] as String,
                clientVersion = clientData["clientVersion"] as String,
                gl = clientData["gl"] as String,
                hl = clientData["hl"] as String,
                visitorData = visitorData,
                androidSdkVersion = clientData["androidSdkVersion"] as? Int
            )
        )
    }
}



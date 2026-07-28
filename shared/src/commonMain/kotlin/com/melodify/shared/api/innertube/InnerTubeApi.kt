package com.melodify.shared.api.innertube

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


class InnerTubeApi(private val httpClient: HttpClient) {
    private var visitorData: String? = null

    suspend fun getVisitorData(): String {
        visitorData?.let { return it }
        try {
            val response = httpClient.post("${InnerTubeConstants.BASE_URL}music/get_search_suggestions") {
                parameter("key", InnerTubeConstants.API_KEY)
                contentType(ContentType.Application.Json)
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
            setBody(request)
        }.body()
    }

    suspend fun getPlayerInfo(videoId: String): PlayerResponse {
        val vData = getVisitorData()
        val request = InnerTubeRequest(
            context = buildContext(isAndroid = true, visitorData = vData),
            videoId = videoId
        )
        return httpClient.post("${InnerTubeConstants.BASE_URL}player") {
            parameter("key", InnerTubeConstants.API_KEY)
            contentType(ContentType.Application.Json)
            setBody(request)
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
            setBody(request)
        }.body()
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


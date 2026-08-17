package com.melodify.shared.data.network.jellyfin

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

class JellyfinClient(private val httpClient: HttpClient) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun authenticate(url: String, user: String, pass: String): JellyfinAuthResult {
        val authString = "MediaBrowser Client=\"Melodify\", Device=\"KMP\", DeviceId=\"melodify_app\", Version=\"1.0.0\""
        
        val response = try {
            httpClient.post("$url/Users/AuthenticateByName") {
                header("X-Emby-Authorization", authString)
                contentType(ContentType.Application.Json)
                setBody("{\"Username\":\"$user\",\"Pw\":\"$pass\"}")
            }.body<String>()
        } catch (e: Exception) {
            throw Exception("Authentication failed: ${e.message}")
        }

        val obj = try {
            json.parseToJsonElement(response).jsonObject
        } catch (e: Exception) {
            throw Exception(if (response.startsWith("Error")) response else "Invalid JSON from Jellyfin: $response")
        }
        val token = obj["AccessToken"]?.jsonPrimitive?.content ?: throw Exception("No AccessToken in Jellyfin response")
        val userId = obj["User"]?.jsonObject?.get("Id")?.jsonPrimitive?.content ?: throw Exception("No User ID in Jellyfin response")
        
        return JellyfinAuthResult(token, userId)
    }

    suspend fun getLibrary(url: String, userId: String, token: String): List<JellyfinTrack> {
        val response = httpClient.get("$url/Users/$userId/Items?IncludeItemTypes=Audio&Recursive=true&Fields=Path,MediaSources") {
            header("X-Emby-Token", token)
        }.body<String>()

        val obj = json.parseToJsonElement(response).jsonObject
        val items = obj["Items"]?.jsonArray ?: return emptyList()

        return items.mapNotNull { item ->
            try {
                val id = item.jsonObject["Id"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val name = item.jsonObject["Name"]?.jsonPrimitive?.content ?: "Unknown"
                val album = item.jsonObject["Album"]?.jsonPrimitive?.content ?: "Unknown Album"
                val artist = item.jsonObject["Artists"]?.jsonArray?.firstOrNull()?.jsonPrimitive?.content ?: "Unknown Artist"
                val runTimeTicks = item.jsonObject["RunTimeTicks"]?.jsonPrimitive?.longOrNull ?: 0L
                val durationMs = runTimeTicks / 10000
                
                JellyfinTrack(
                    id = id,
                    title = name,
                    artist = artist,
                    album = album,
                    durationMs = durationMs
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getStreamUrl(url: String, itemId: String, token: String): String {
        return "$url/Audio/$itemId/universal?api_key=$token&Container=flac,mp3,m4a"
    }
}

data class JellyfinAuthResult(val token: String, val userId: String)

data class JellyfinTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

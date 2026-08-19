package com.melodify.shared.api.jiosaavn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object JioSaavnApi {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun getStreamUrl(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$title $artist".trim().replace(" ", "%20")
            val searchUrl = "https://www.jiosaavn.com/api.php?__call=autocomplete.get&_format=json&_marker=0&cc=in&includeMetaTags=1&query=$query"
            
            val searchResponse = client.get(searchUrl) {
                header("User-Agent", "Mozilla/5.0")
            }
            
            val searchJson = json.parseToJsonElement(searchResponse.bodyAsText()).jsonObject
            val songs = searchJson["songs"]?.jsonObject?.get("data")?.jsonArray
            
            if (songs.isNullOrEmpty()) {
                println("JioSaavn: No results found for '$title $artist'")
                return@withContext null
            }
            
            val firstSong = songs.first().jsonObject
            val songId = firstSong["id"]?.jsonPrimitive?.content ?: return@withContext null
            
            println("JioSaavn: Found match for '$title' -> ID: $songId")
            
            val detailsUrl = "https://www.jiosaavn.com/api.php?__call=song.getDetails&cc=in&_marker=0%3F_marker%3D0&_format=json&pids=$songId"
            val detailsResponse = client.get(detailsUrl) {
                header("User-Agent", "Mozilla/5.0")
            }
            
            val detailsJson = json.parseToJsonElement(detailsResponse.bodyAsText()).jsonObject
            val songDetails = detailsJson[songId]?.jsonObject
            val mediaUrl = songDetails?.get("media_preview_url")?.jsonPrimitive?.content
            
            if (mediaUrl != null) {
                // The preview URL is actually the full song at 96kbps. 
                // Verify the CDN isn't rate-limiting or blocking us (e.g. 403/429)
                val checkResponse = client.get(mediaUrl) {
                    header("User-Agent", "Mozilla/5.0")
                    header("Range", "bytes=0-100")
                }
                
                if (checkResponse.status.value in 200..299) {
                    println("JioSaavn: Stream is playable, returning URL for ID: $songId")
                    return@withContext mediaUrl
                } else {
                    println("JioSaavn: CDN blocked the stream (Status ${checkResponse.status.value}), falling back to YouTube")
                    return@withContext null
                }
            }
            null
        } catch (e: Exception) {
            println("JioSaavn Error: ${e.message}")
            null
        }
    }
}

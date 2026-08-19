package com.melodify.shared.api.jiosaavn

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json

// --- Data Models matching sumitkolhe/jiosaavn-api ---
@Serializable
data class JioSaavnSearchResponse(val data: JioSaavnSearchData? = null)

@Serializable
data class JioSaavnSearchData(val results: List<JioSaavnTrack>? = null)

@Serializable
data class JioSaavnTrack(
    val id: String,
    val name: String,
    @SerialName("downloadUrl") val downloadUrls: List<DownloadUrl>? = null
)

@Serializable
data class DownloadUrl(val quality: String, val url: String)

object JioSaavnApi {
    private val client = HttpClient()
    private val json = Json { ignoreUnknownKeys = true }
    
    // NOTE: You must deploy your own instance of sumitkolhe/jiosaavn-api on Vercel 
    // and replace this URL with your deployment URL to avoid rate limits!
    // Example: "https://your-deployed-api.vercel.app/api"
    private const val BASE_URL = "https://saavn.dev/api"

    suspend fun getStreamUrl(title: String, artist: String): String? = withContext(Dispatchers.IO) {
        try {
            val query = "$title $artist".trim()
            val response = client.get("$BASE_URL/search/songs") {
                parameter("query", query)
            }
            
            val searchResponse = json.decodeFromString<JioSaavnSearchResponse>(response.bodyAsText())
            val results = searchResponse.data?.results
            
            if (results.isNullOrEmpty()) {
                println("JioSaavn API: No results found for '$query'")
                return@withContext null
            }
            
            val topTrack = results.first()
            val downloadUrls = topTrack.downloadUrls
            
            if (downloadUrls.isNullOrEmpty()) {
                println("JioSaavn API: No download URLs available for '${topTrack.name}'")
                return@withContext null
            }
            
            // Try to find the 320kbps link, fallback to whatever is highest/last
            val bestUrl = downloadUrls.find { it.quality == "320kbps" }?.url 
                ?: downloadUrls.last().url
                
            println("JioSaavn API: Found 320kbps stream for '${topTrack.name}' -> ID: ${topTrack.id}")
            return@withContext bestUrl
            
        } catch (e: Exception) {
            println("JioSaavn API Error: ${e.message}")
            null
        }
    }
}

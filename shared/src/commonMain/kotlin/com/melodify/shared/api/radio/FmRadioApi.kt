package com.melodify.shared.api.radio

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class FmStation(
    val id: String,
    val name: String,
    val streamUrl: String,
    val favicon: String,
    val tags: String
)

class FmRadioApi(private val client: HttpClient) {
    // using de1 as default, api.radio-browser.info handles dns round robin usually, but de1 is very stable
    private val baseUrl = "https://de1.api.radio-browser.info/json"

    suspend fun getTopStations(limit: Int = 10, countryCode: String? = null): List<FmStation> = runCatching {
        val response = if (countryCode != null) {
            client.get("$baseUrl/stations/search?countrycodeexact=$countryCode&limit=$limit&order=clickcount&reverse=true")
        } else {
            client.get("$baseUrl/stations/topclick/$limit")
        }
        val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText())
        val stationsArray = json.jsonArray
        
        stationsArray.mapNotNull { element ->
            val obj = element.jsonObject
            val name = obj["name"]?.jsonPrimitive?.content?.trim() ?: return@mapNotNull null
            val url = obj["url_resolved"]?.jsonPrimitive?.content ?: return@mapNotNull null
            if (url.isEmpty()) return@mapNotNull null
            
            FmStation(
                id = obj["stationuuid"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                name = if (name.isEmpty()) "Unknown Station" else name,
                streamUrl = url,
                favicon = obj["favicon"]?.jsonPrimitive?.content ?: "",
                tags = obj["tags"]?.jsonPrimitive?.content ?: ""
            )
        }.filter { it.name.isNotBlank() }
    }.getOrDefault(emptyList())
}

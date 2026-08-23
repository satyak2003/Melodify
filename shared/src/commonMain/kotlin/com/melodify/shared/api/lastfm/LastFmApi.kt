package com.melodify.shared.api.lastfm

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class LastFmApi(private val client: HttpClient) {
    private val baseUrl = "https://ws.audioscrobbler.com/2.0/"
    private val apiKey = "c71c61e70049b9bb6414f56b0024a559" // TODO: Replace with secure storage or build config

    suspend fun getSimilarTracks(artist: String, track: String, limit: Int = 10): List<String> = runCatching {
        val response = client.get(baseUrl) {
            parameter("method", "track.getsimilar")
            parameter("artist", artist)
            parameter("track", track)
            parameter("api_key", apiKey)
            parameter("format", "json")
            parameter("limit", limit)
        }
        
        val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText())
        val similarTracks = json.jsonObject["similartracks"]?.jsonObject?.get("track")?.jsonArray ?: return emptyList()
        
        similarTracks.mapNotNull { 
            val trackName = it.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val artistName = it.jsonObject["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: return@mapNotNull null
            "$trackName $artistName"
        }
    }.getOrDefault(emptyList())

    suspend fun getMoodTracks(mood: String, limit: Int = 10): List<String> = runCatching {
        val response = client.get(baseUrl) {
            parameter("method", "tag.gettoptracks")
            parameter("tag", mood)
            parameter("api_key", apiKey)
            parameter("format", "json")
            parameter("limit", limit)
        }
        
        val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(response.bodyAsText())
        val topTracks = json.jsonObject["tracks"]?.jsonObject?.get("track")?.jsonArray ?: return emptyList()
        
        topTracks.mapNotNull { 
            val trackName = it.jsonObject["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
            val artistName = it.jsonObject["artist"]?.jsonObject?.get("name")?.jsonPrimitive?.content ?: return@mapNotNull null
            "$trackName $artistName"
        }
    }.getOrDefault(emptyList())
}

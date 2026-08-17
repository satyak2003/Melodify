package com.melodify.shared.api.deezer

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess

class DeezerApi(private val httpClient: HttpClient) {

    suspend fun searchTrack(query: String, limit: Int = 1): DeezerSearchResponse? {
        return try {
            val response = httpClient.get("https://api.deezer.com/search/track") {
                parameter("q", query)
                parameter("limit", limit)
            }
            if (response.status.isSuccess()) {
                response.body()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

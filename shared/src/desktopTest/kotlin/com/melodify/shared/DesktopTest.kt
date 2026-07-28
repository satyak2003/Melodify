package com.melodify.shared

import kotlinx.coroutines.runBlocking
import com.melodify.shared.api.innertube.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.junit.Test

class DesktopTest {
    @Test
    fun testInnerTube() = runBlocking {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false })
            }
        }
        val api = InnerTubeApi(client)
        try {
            val result = api.search("hello adele")
            val parsed = InnerTubeParser.parseSearchResults(result)
            println("Tracks found: ${parsed.tracks.size}")
            if (parsed.tracks.isEmpty()) {
                println("No tracks were parsed!")
            }
        } catch(e:Exception) {
            println("Error: ${e.message}")
            e.printStackTrace()
        }
    }
}

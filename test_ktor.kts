@file:DependsOn("io.ktor:ktor-client-core:2.3.11")
@file:DependsOn("io.ktor:ktor-client-cio:2.3.11")
@file:DependsOn("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.InputStreamReader

runBlocking {
    val url = "https://pipedapi.kavin.rocks/streams/LpNVf8sczqU"
    val client = HttpClient(CIO)
    try {
        val response = client.get(url) {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            header(HttpHeaders.Accept, "application/json")
        }
        println("Ktor Status: ${response.status}")
        println("Ktor Response: ${response.bodyAsText().take(200)}")
    } catch (e: Exception) {
        println("Ktor Error: ${e.message}")
    } finally {
        client.close()
    }
}

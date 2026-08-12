package com.melodify.shared.data.storage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.*

object SupabaseApi {
    private val client = HttpClient()
    
    private const val SUPABASE_URL = "https://evbrwmyyccjanwnhvxqw.supabase.co"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV2YnJ3bXl5Y2NqYW53bmh2eHF3Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODUzMjMwNDksImV4cCI6MjEwMDg5OTA0OX0.cOwSqXXNSxroD-l-ypqsNTZrZpCzYL2Hx9cqkhSCdD0"
    
    // We don't need Google Sign In token for Supabase if we rely on ANON key
    suspend fun signInWithGoogleIdToken(googleIdToken: String): Boolean {
        return true
    }

    suspend fun writeSession(sessionCode: String, data: String) {
        try {
            client.post("$SUPABASE_URL/rest/v1/sessions") {
                header("apikey", ANON_KEY)
                header("Authorization", "Bearer $ANON_KEY")
                header("Prefer", "resolution=merge-duplicates")
                contentType(ContentType.Application.Json)
                val body = buildJsonObject {
                    put("session_code", sessionCode)
                    put("data", data)
                }
                setBody(body.toString())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getSession(sessionCode: String): String? {
        try {
            val response = client.get("$SUPABASE_URL/rest/v1/sessions?session_code=eq.$sessionCode") {
                header("apikey", ANON_KEY)
                header("Authorization", "Bearer $ANON_KEY")
                header("Accept", "application/json")
            }
            if (response.status.isSuccess()) {
                val responseText = response.bodyAsText()
                val jsonArray = Json { ignoreUnknownKeys = true }.parseToJsonElement(responseText).jsonArray
                if (jsonArray.isNotEmpty()) {
                    return jsonArray[0].jsonObject["data"]?.jsonPrimitive?.content
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    
    suspend fun deleteSession(sessionCode: String) {
        try {
            client.delete("$SUPABASE_URL/rest/v1/sessions?session_code=eq.$sessionCode") {
                header("apikey", ANON_KEY)
                header("Authorization", "Bearer $ANON_KEY")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

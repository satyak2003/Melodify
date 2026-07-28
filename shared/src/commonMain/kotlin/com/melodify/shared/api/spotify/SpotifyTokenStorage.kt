package com.melodify.shared.api.spotify

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.io.File

@Serializable
data class SpotifySession(
    val accessToken: String? = null,
    val refreshToken: String? = null
)

object SpotifyTokenStorage {
    private val verifierFile: File
        get() = File(baseDir, "spotify_verifier.txt")

    private val sessionFile: File
        get() = File(baseDir, "spotify_session.json")

    private val baseDir: File
        get() {
            val userHome = System.getProperty("user.home")
            val dir = if (userHome != null && userHome != "/" && userHome.isNotBlank()) {
                val f = File(userHome, ".melodify")
                if (f.exists() || f.mkdirs()) f else File(System.getProperty("java.io.tmpdir") ?: ".", ".melodify")
            } else {
                File(System.getProperty("java.io.tmpdir") ?: ".", ".melodify")
            }
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val redirectUriFile: File
        get() = File(baseDir, "spotify_redirect_uri.txt")

    fun saveCodeVerifier(verifier: String) {
        try { verifierFile.writeText(verifier) } catch (e: Exception) {}
    }

    fun loadCodeVerifier(): String? {
        try {
            if (!verifierFile.exists()) return null
            return verifierFile.readText()
        } catch (e: Exception) { return null }
    }

    fun saveRedirectUri(uri: String) {
        try { redirectUriFile.writeText(uri) } catch (e: Exception) {}
    }

    fun loadRedirectUri(): String? {
        try {
            if (!redirectUriFile.exists()) return null
            return redirectUriFile.readText()
        } catch (e: Exception) { return null }
    }



    fun saveToken(accessToken: String, refreshToken: String? = null) {
        try {
            val json = buildJsonObject {
                put("accessToken", accessToken)
                if (!refreshToken.isNullOrBlank()) {
                    put("refreshToken", refreshToken)
                }
            }.toString()
            sessionFile.writeText(json)
        } catch (e: Exception) {
            println("Failed to save Spotify token: ${e.message}")
        }
    }

    fun loadToken(): SpotifySession {
        try {
            if (!sessionFile.exists()) return SpotifySession()
            val json = Json.parseToJsonElement(sessionFile.readText()).jsonObject
            val access = json["accessToken"]?.jsonPrimitive?.content
            val refresh = json["refreshToken"]?.jsonPrimitive?.content
            return SpotifySession(access, refresh)
        } catch (e: Exception) {
            return SpotifySession()
        }
    }

    fun clearToken() {
        try {
            if (sessionFile.exists()) {
                sessionFile.delete()
            }
        } catch (e: Exception) {
            println("Failed to clear Spotify token: ${e.message}")
        }
    }
}

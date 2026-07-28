package com.melodify.shared.api.spotify

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

object SpotifyAuthHelper {
    const val CLIENT_ID = "3a52ed8ee5544ed8983d75d4bb229f90"
    const val REDIRECT_URI = "http://127.0.0.1:8080/callback"
    const val ANDROID_REDIRECT_URI = "melodify://callback"
    const val SCOPES = "playlist-read-private playlist-read-collaborative user-library-read user-read-private user-read-email"
    
    private val charPool: List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('-', '.', '_', '~')

    fun generateCodeVerifier(): String {
        val length = Random.nextInt(43, 129)
        return (1..length)
            .map { Random.nextInt(0, charPool.size) }
            .map(charPool::get)
            .joinToString("")
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.encodeToByteArray()
        val hashed = sha256(bytes)
        return Base64.UrlSafe.encode(hashed).trimEnd('=')
    }

    fun buildAuthUrl(codeVerifier: String, redirectUri: String = REDIRECT_URI): String {
        val challenge = generateCodeChallenge(codeVerifier)
        val encodedRedirectUri = redirectUri.replace(":", "%3A").replace("/", "%2F")
        return "https://accounts.spotify.com/authorize?" +
                "client_id=$CLIENT_ID" +
                "&response_type=code" +
                "&redirect_uri=$encodedRedirectUri" +
                "&code_challenge_method=S256" +
                "&code_challenge=$challenge" +
                "&scope=${SCOPES.replace(" ", "%20")}"
    }

    fun parseAuthCode(redirectUrl: String): String? {
        if (redirectUrl.contains("code=")) {
            val codeSection = redirectUrl.substringAfter("code=")
            return codeSection.substringBefore("&")
        }
        return null
    }
}

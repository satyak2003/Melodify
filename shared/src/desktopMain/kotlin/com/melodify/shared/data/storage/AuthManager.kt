package com.melodify.shared.data.storage

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.ServerSocket
import java.awt.Desktop
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import kotlinx.serialization.json.*

actual object AuthManager {
    private val _isGoogleLoggedIn = MutableStateFlow(false)
    actual val isGoogleLoggedIn = _isGoogleLoggedIn.asStateFlow()
    
    private val _userProfileUrl = MutableStateFlow<String?>(null)
    actual val userProfileUrl = _userProfileUrl.asStateFlow()

    private val _userName = MutableStateFlow<String?>(null)
    actual val userName = _userName.asStateFlow()
    
    // Desktop App Client ID
    private const val CLIENT_ID = "YOUR_GOOGLE_CLIENT_ID_HERE"
    private const val CLIENT_SECRET = "YOUR_GOOGLE_CLIENT_SECRET_HERE"
    private const val REDIRECT_URI = "http://127.0.0.1:8080/callback"

    actual suspend fun loginWithGoogle(): GoogleTokens? = withContext(Dispatchers.IO) {
        try {
            val secureRandom = SecureRandom()
            val codeVerifierBytes = ByteArray(32)
            secureRandom.nextBytes(codeVerifierBytes)
            val codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(codeVerifierBytes)

            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(codeVerifier.toByteArray(Charsets.US_ASCII))
            val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash)

            val authUrl = "https://accounts.google.com/o/oauth2/v2/auth?" +
                    "client_id=$CLIENT_ID&" +
                    "redirect_uri=$REDIRECT_URI&" +
                    "response_type=code&" +
                    "scope=email%20profile%20https://www.googleapis.com/auth/youtube.readonly&" +
                    "code_challenge=$codeChallenge&" +
                    "code_challenge_method=S256"
            
            val serverSocket = ServerSocket(8080).apply {
                soTimeout = 60000 // 60 seconds timeout
            }
            
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(authUrl))
            } else {
                serverSocket.close()
                return@withContext null
            }
            
            val socket = serverSocket.accept()
            val reader = socket.getInputStream().bufferedReader()
            val line = reader.readLine() ?: ""
            
            val responseHtml = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<html><body><h2>Login successful!</h2><p>You can close this tab and return to Melodify.</p><script>window.close();</script></body></html>"
            socket.getOutputStream().write(responseHtml.toByteArray())
            socket.close()
            serverSocket.close()
            
            if (!line.contains("GET /callback") || !line.contains("code=")) return@withContext null
            
            val code = line.substringAfter("code=").substringBefore("&").substringBefore(" ")
            if (code.isBlank() || code.contains("error")) return@withContext null

            // Exchange code for token
            val encodedCode = java.net.URLEncoder.encode(code, "UTF-8")
            val client = HttpClient()
            val response = client.post("https://oauth2.googleapis.com/token") {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody("client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET&grant_type=authorization_code&redirect_uri=$REDIRECT_URI&code=$encodedCode&code_verifier=$codeVerifier")
            }
            client.close()
            
            val responseBody = response.bodyAsText()
            val json = Json { ignoreUnknownKeys = true }.parseToJsonElement(responseBody).jsonObject
            val accessToken = json["access_token"]?.jsonPrimitive?.content ?: ""
            val idToken = json["id_token"]?.jsonPrimitive?.content ?: ""
            
            if (accessToken.isNotEmpty() || idToken.isNotEmpty()) {
                _isGoogleLoggedIn.value = true
                return@withContext GoogleTokens(accessToken, idToken)
            }
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun logout() {
        _isGoogleLoggedIn.value = false
    }
}

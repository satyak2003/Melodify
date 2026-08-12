package com.melodify.shared.data.storage

data class GoogleTokens(val accessToken: String, val idToken: String)

expect object AuthManager {
    suspend fun loginWithGoogle(): GoogleTokens?
    suspend fun logout()
    val isGoogleLoggedIn: kotlinx.coroutines.flow.StateFlow<Boolean>
    val userProfileUrl: kotlinx.coroutines.flow.StateFlow<String?>
    val userName: kotlinx.coroutines.flow.StateFlow<String?>
}

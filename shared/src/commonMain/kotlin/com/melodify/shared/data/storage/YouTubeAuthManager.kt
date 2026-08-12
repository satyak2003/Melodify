package com.melodify.shared.data.storage

import com.melodify.shared.crypto.sha1Hex
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.melodify.shared.utils.getCurrentTimeSeconds
import java.io.File

object YouTubeAuthManager {
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userAccountName = MutableStateFlow<String?>("Guest User")
    val userAccountName: StateFlow<String?> = _userAccountName.asStateFlow()

    private var cookies: String? = null
    
    private val file: File
        get() = File(AppStorage.getStorageDir(), "yt_cookies.txt")

    init {
        try {
            if (file.exists()) {
                val saved = file.readText()
                if (saved.isNotBlank()) {
                    cookies = saved
                    _isLoggedIn.value = true
                    _userAccountName.value = "YouTube Premium User"
                }
            }
        } catch (e: Exception) {
            println("Failed to load YT cookies: ${e.message}")
        }
    }

    fun loginWithCookies(cookieHeader: String, accountName: String = "YouTube Premium User") {
        if (cookieHeader.isNotBlank()) {
            cookies = cookieHeader.trim()
            _isLoggedIn.value = true
            _userAccountName.value = accountName
            try {
                file.writeText(cookies!!)
            } catch (e: Exception) {
                println("Failed to save YT cookies: ${e.message}")
            }
        }
    }

    fun logout() {
        cookies = null
        _isLoggedIn.value = false
        _userAccountName.value = "Guest User"
        try {
            if (file.exists()) file.delete()
        } catch (ignored: Exception) {}
    }

    fun getCookieHeader(): String? = cookies

    fun getAuthHeader(): String? {
        val cookieString = cookies ?: return null
        // Extract SAPISID
        val sapisidMatch = Regex("SAPISID=([^;]+)").find(cookieString)
        val sapisid = sapisidMatch?.groupValues?.get(1) ?: return null

        val timestamp = getCurrentTimeSeconds().toString()
        val origin = "https://www.youtube.com"
        val hashStr = "$timestamp $sapisid $origin"
        val hash = sha1Hex(hashStr).lowercase()
        return "SAPISIDHASH ${timestamp}_$hash"
    }
}


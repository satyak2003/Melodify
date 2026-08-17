package com.melodify.shared.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object JellyfinSettings {
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _apiToken = MutableStateFlow("")
    val apiToken: StateFlow<String> = _apiToken.asStateFlow()
    
    private val _userId = MutableStateFlow("")
    val userId: StateFlow<String> = _userId.asStateFlow()

    private val settingsFile: File by lazy {
        File(AppStorage.getStorageDir(), "jellyfin_settings.txt")
    }

    init {
        loadSettings()
    }

    private fun loadSettings() {
        if (settingsFile.exists()) {
            try {
                val lines = settingsFile.readLines()
                if (lines.size >= 5) {
                    _serverUrl.value = lines[0]
                    _username.value = lines[1]
                    _password.value = lines[2]
                    _apiToken.value = lines[3]
                    _userId.value = lines[4]
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveSettings(url: String, user: String, pass: String, token: String = _apiToken.value, id: String = _userId.value) {
        _serverUrl.value = url
        _username.value = user
        _password.value = pass
        _apiToken.value = token
        _userId.value = id
        try {
            settingsFile.writeText("$url\n$user\n$pass\n$token\n$id")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

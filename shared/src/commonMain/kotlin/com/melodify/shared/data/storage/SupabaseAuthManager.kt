package com.melodify.shared.data.storage

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object SupabaseAuthManager {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _currentUser = MutableStateFlow<SupabaseUser?>(null)
    val currentUser: StateFlow<SupabaseUser?> = _currentUser.asStateFlow()

    fun login(sessionCode: String) {
        scope.launch {
            val user = SupabaseApi.getUserProfile(sessionCode)
            if (user != null) {
                _currentUser.value = user
            } else {
                // Invalid or expired token
                _currentUser.value = null
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }
}

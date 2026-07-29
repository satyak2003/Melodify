package com.melodify.shared.data.storage

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ExperimentalSettingsStorage {
    private val _isMlTaggingEnabled = MutableStateFlow(false)
    val isMlTaggingEnabled: StateFlow<Boolean> = _isMlTaggingEnabled.asStateFlow()

    private val _isSyncListeningEnabled = MutableStateFlow(true)
    val isSyncListeningEnabled: StateFlow<Boolean> = _isSyncListeningEnabled.asStateFlow()

    fun setMlTaggingEnabled(enabled: Boolean) {
        _isMlTaggingEnabled.value = enabled
    }

    fun setSyncListeningEnabled(enabled: Boolean) {
        _isSyncListeningEnabled.value = enabled
    }
}

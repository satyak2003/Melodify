package com.melodify.shared.navigation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Shared singleton to handle deep links received from the OS (like Android Intents).
 */
object DeepLinkHandler {
    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deepLinks = _deepLinks.asSharedFlow()

    fun handleDeepLink(url: String) {
        _deepLinks.tryEmit(url)
    }
}

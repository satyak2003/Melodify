package com.melodify.shared.utils

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import java.net.InetSocketAddress
import java.net.Socket

actual object NetworkMonitor {
    private val _isOffline = MutableStateFlow(false)
    actual val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private var monitoring = false

    actual fun startMonitoring() {
        if (monitoring) return
        monitoring = true
        GlobalScope.launch(Dispatchers.IO) {
            while (monitoring) {
                try {
                    val socket = Socket()
                    socket.connect(InetSocketAddress("8.8.8.8", 53), 2000)
                    socket.close()
                    _isOffline.value = false
                } catch (e: Exception) {
                    _isOffline.value = true
                }
                delay(5000)
            }
        }
    }

    actual fun stopMonitoring() {
        monitoring = false
    }
}

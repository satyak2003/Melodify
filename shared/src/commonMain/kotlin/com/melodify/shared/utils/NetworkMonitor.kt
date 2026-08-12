package com.melodify.shared.utils

import kotlinx.coroutines.flow.StateFlow

expect object NetworkMonitor {
    val isOffline: StateFlow<Boolean>
    fun startMonitoring()
    fun stopMonitoring()
}

package com.melodify.shared.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.melodify.shared.data.storage.AppStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual object NetworkMonitor {
    private val _isOffline = MutableStateFlow(false)
    actual val isOffline: StateFlow<Boolean> = _isOffline.asStateFlow()

    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    actual fun startMonitoring() {
        if (networkCallback != null) return
        val context = AppStorage.applicationContext ?: return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _isOffline.value = false
            }

            override fun onLost(network: Network) {
                _isOffline.value = true
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        cm.registerNetworkCallback(request, networkCallback!!)
        
        // Initial state
        val activeNetwork = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(activeNetwork)
        _isOffline.value = !(caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET))
    }

    actual fun stopMonitoring() {
        val context = AppStorage.applicationContext ?: return
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            cm.unregisterNetworkCallback(it)
        }
        networkCallback = null
    }
}

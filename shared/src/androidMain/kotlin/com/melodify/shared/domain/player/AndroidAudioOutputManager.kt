package com.melodify.shared.domain.player

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioDeviceCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.os.Build

class AndroidAudioOutputManager(private val context: Context) : AudioOutputManager {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    private val _activeDevice = MutableStateFlow(AudioOutputDevice("Unknown", AudioDeviceType.UNKNOWN))
    override val activeDevice = _activeDevice.asStateFlow()
    
    private val deviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            updateActiveDevice()
        }
    }
    
    fun start() {
        audioManager.registerAudioDeviceCallback(deviceCallback, null)
        updateActiveDevice()
    }
    
    fun stop() {
        audioManager.unregisterAudioDeviceCallback(deviceCallback)
    }
    
    private fun updateActiveDevice() {
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        
        var bestDevice: AudioDeviceInfo? = null
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || device.type == AudioDeviceInfo.TYPE_BLE_HEADSET || device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO) {
                bestDevice = device
                break
            }
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET || device.type == AudioDeviceInfo.TYPE_USB_HEADSET) {
                if (bestDevice == null || bestDevice.type != AudioDeviceInfo.TYPE_BLUETOOTH_A2DP) {
                    bestDevice = device
                }
            }
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                if (bestDevice == null) bestDevice = device
            }
        }
        
        val type = when (bestDevice?.type) {
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_BLE_HEADSET, AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> AudioDeviceType.BLUETOOTH
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_USB_HEADSET -> AudioDeviceType.WIRED_HEADPHONES
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioDeviceType.BUILTIN_SPEAKER
            else -> AudioDeviceType.UNKNOWN
        }
        
        val name = bestDevice?.productName?.toString() ?: "Phone Speaker"
        _activeDevice.value = AudioOutputDevice(
            name = if (type == AudioDeviceType.BUILTIN_SPEAKER) "Phone Speaker" else if (name.isBlank()) "Audio Device" else name,
            type = type
        )
    }
}
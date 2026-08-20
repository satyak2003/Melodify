package com.melodify.shared.domain.player

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.sound.sampled.AudioSystem

class DesktopAudioOutputManager : AudioOutputManager {
    private val _activeDevice = MutableStateFlow(AudioOutputDevice("Unknown", AudioDeviceType.UNKNOWN))
    override val activeDevice = _activeDevice.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        updateDevice()
        scope.launch {
            while(isActive) {
                delay(5000)
                updateDevice()
            }
        }
    }

    private fun updateDevice() {
        try {
            val mixers = AudioSystem.getMixerInfo()
            var bestName = "System Audio"
            var bestType = AudioDeviceType.BUILTIN_SPEAKER
            
            for (mixer in mixers) {
                val name = mixer.name.lowercase()
                
                if ("headphone" in name || "headset" in name || "earphone" in name) {
                    bestType = AudioDeviceType.WIRED_HEADPHONES
                    bestName = mixer.name
                    break
                }
                if ("bluetooth" in name || "a2dp" in name || "hands-free" in name || "bth" in name) {
                    bestType = AudioDeviceType.BLUETOOTH
                    bestName = mixer.name
                    break
                }
                if ("speaker" in name) {
                    bestType = AudioDeviceType.BUILTIN_SPEAKER
                    bestName = mixer.name
                }
            }
            
            val cleanName = bestName.replace(Regex("\\(.*\\)"), "").trim().takeIf { it.isNotBlank() } ?: "Speaker"
            _activeDevice.value = AudioOutputDevice(cleanName, bestType)
        } catch (e: Exception) {
            _activeDevice.value = AudioOutputDevice("System Audio", AudioDeviceType.BUILTIN_SPEAKER)
        }
    }
    
    fun stop() {
        scope.cancel()
    }
}
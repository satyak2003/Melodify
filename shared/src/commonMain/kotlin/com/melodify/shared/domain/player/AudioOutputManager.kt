package com.melodify.shared.domain.player

import kotlinx.coroutines.flow.StateFlow

enum class AudioDeviceType {
    BUILTIN_SPEAKER,
    WIRED_HEADPHONES,
    BLUETOOTH,
    UNKNOWN
}

data class AudioOutputDevice(
    val name: String,
    val type: AudioDeviceType
)

interface AudioOutputManager {
    val activeDevice: StateFlow<AudioOutputDevice>
}

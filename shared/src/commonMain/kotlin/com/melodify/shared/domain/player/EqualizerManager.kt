package com.melodify.shared.domain.player

import kotlinx.coroutines.flow.StateFlow

data class EqBand(val index: Int, val centerFreqHz: Int, val levelMb: Int)
data class EqPreset(val name: String, val index: Int)

interface EqualizerManager {
    val isEnabled: StateFlow<Boolean>
    val bands: StateFlow<List<EqBand>>
    val presets: StateFlow<List<EqPreset>>
    val currentPreset: StateFlow<EqPreset?>
    val minBandLevelMb: Int
    val maxBandLevelMb: Int

    fun setEnabled(enabled: Boolean)
    fun setBandLevel(bandIndex: Int, levelMb: Int)
    fun usePreset(presetIndex: Int)
}

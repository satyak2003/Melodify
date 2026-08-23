package com.melodify.shared.domain.player

import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class AndroidEqualizerManager : EqualizerManager {
    private var equalizer: Equalizer? = null

    private val _isEnabled = MutableStateFlow(false)
    override val isEnabled = _isEnabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    override val bands = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<EqPreset>>(emptyList())
    override val presets = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow<EqPreset?>(null)
    override val currentPreset = _currentPreset.asStateFlow()

    override var minBandLevelMb: Int = -1500
        private set
    override var maxBandLevelMb: Int = 1500
        private set

    companion object {
        val STANDARD_PRESETS = listOf(
            EqPreset("Normal", 0),
            EqPreset("Classical", 1),
            EqPreset("Dance", 2),
            EqPreset("Flat", 3),
            EqPreset("Folk", 4),
            EqPreset("Heavy Metal", 5),
            EqPreset("Hip Hop", 6),
            EqPreset("Jazz", 7),
            EqPreset("Pop", 8),
            EqPreset("Rock", 9)
        )

        // Standard band adjustments per preset in millibels (mB)
        val PRESET_LEVELS = mapOf(
            0 to listOf(300, 0, 0, 0, 300), // Normal
            1 to listOf(500, 300, -200, 400, 400), // Classical
            2 to listOf(600, 0, 200, 400, 100), // Dance
            3 to listOf(0, 0, 0, 0, 0), // Flat
            4 to listOf(300, 0, 0, 200, -100), // Folk
            5 to listOf(400, 100, 900, 300, 0), // Heavy Metal
            6 to listOf(500, 300, 0, 100, 300), // Hip Hop
            7 to listOf(400, 200, -200, 200, 500), // Jazz
            8 to listOf(-100, 200, 500, 100, -200), // Pop
            9 to listOf(500, 300, -100, 300, 500) // Rock
        )
    }

    init {
        // Provide standard 5-band defaults before a session is attached so the UI is not empty
        val defaults = listOf(60, 230, 910, 3600, 14000)
        _bands.value = defaults.mapIndexed { index, freq -> EqBand(index, freq, 0) }
        _presets.value = STANDARD_PRESETS
        _currentPreset.value = STANDARD_PRESETS.find { it.name == "Flat" }
    }

    fun attachSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) return
        release()
        try {
            val newEq = Equalizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }
            equalizer = newEq
            
            val currentPresetVal = _currentPreset.value
            if (currentPresetVal != null) {
                try { newEq.usePreset(currentPresetVal.index.toShort()) } catch (e: Exception) {}
            } else {
                val existingBands = _bands.value
                if (existingBands.isNotEmpty() && existingBands.any { it.levelMb != 0 }) {
                    for (i in existingBands.indices) {
                        try { newEq.setBandLevel(i.toShort(), existingBands[i].levelMb.toShort()) } catch (e: Exception) {}
                    }
                }
            }
            
            loadFromEqualizer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFromEqualizer() {
        val eq = equalizer ?: return
        minBandLevelMb = eq.bandLevelRange[0].toInt()
        maxBandLevelMb = eq.bandLevelRange[1].toInt()

        val numBands = eq.numberOfBands.toInt()
        val bandList = mutableListOf<EqBand>()
        for (i in 0 until numBands) {
            val centerFreq = eq.getCenterFreq(i.toShort()) / 1000 // mHz to Hz
            val level = eq.getBandLevel(i.toShort()).toInt()
            bandList.add(EqBand(i, centerFreq, level))
        }
        _bands.value = bandList

        val numPresets = eq.numberOfPresets.toInt()
        if (numPresets > 0) {
            val presetList = mutableListOf<EqPreset>()
            for (i in 0 until numPresets) {
                presetList.add(EqPreset(eq.getPresetName(i.toShort()), i))
            }
            _presets.value = presetList
            
            val currPreset = eq.currentPreset.toInt()
            _currentPreset.value = presetList.find { it.index == currPreset }
        } else {
            // Keep the standard presets if the device returns 0 presets natively
            val currBands = _bands.value.map { it.levelMb }
            _currentPreset.value = STANDARD_PRESETS.find { PRESET_LEVELS[it.index] == currBands }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        equalizer?.enabled = enabled
    }

    override fun setBandLevel(bandIndex: Int, levelMb: Int) {
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), levelMb.toShort())
            val currentBands = _bands.value.toMutableList()
            if (bandIndex in currentBands.indices) {
                currentBands[bandIndex] = currentBands[bandIndex].copy(levelMb = levelMb)
                _bands.value = currentBands
                _currentPreset.value = null // Custom
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun usePreset(presetIndex: Int) {
        val preset = _presets.value.find { it.index == presetIndex } ?: return
        _currentPreset.value = preset

        if (equalizer != null) {
            try {
                equalizer?.usePreset(presetIndex.toShort())
                loadFromEqualizer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Fallback so UI updates immediately when music is not playing
            val levels = PRESET_LEVELS[presetIndex] ?: listOf(0, 0, 0, 0, 0)
            val currentBands = _bands.value.toMutableList()
            for (i in currentBands.indices) {
                val lvl = levels.getOrElse(i) { 0 }
                if (i < currentBands.size) {
                    currentBands[i] = currentBands[i].copy(levelMb = lvl)
                }
            }
            _bands.value = currentBands
        }
    }

    fun release() {
        try { equalizer?.release() } catch (e: Exception) {}
        equalizer = null
    }
}

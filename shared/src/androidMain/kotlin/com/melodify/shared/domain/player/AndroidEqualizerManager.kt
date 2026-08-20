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

    init {
        // Provide standard 5-band defaults before a session is attached so the UI is not empty
        val defaults = listOf(60, 230, 910, 3600, 14000)
        _bands.value = defaults.mapIndexed { index, freq -> EqBand(index, freq, 0) }
    }

fun attachSession(sessionId: Int) {
        if (sessionId == 0 || sessionId == android.media.AudioManager.AUDIO_SESSION_ID_GENERATE) return
        release()
        try {
            val newEq = Equalizer(0, sessionId).apply {
                enabled = _isEnabled.value
            }
            equalizer = newEq
            
            val existingBands = _bands.value
            if (existingBands.isNotEmpty() && existingBands.any { it.levelMb != 0 }) {
                for (i in existingBands.indices) {
                    try { newEq.setBandLevel(i.toShort(), existingBands[i].levelMb.toShort()) } catch (e: Exception) {}
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
        val presetList = mutableListOf<EqPreset>()
        for (i in 0 until numPresets) {
            presetList.add(EqPreset(eq.getPresetName(i.toShort()), i))
        }
        _presets.value = presetList
        
        val currPreset = eq.currentPreset.toInt()
        _currentPreset.value = presetList.find { it.index == currPreset }
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
        try {
            equalizer?.usePreset(presetIndex.toShort())
            loadFromEqualizer()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun release() {
        try { equalizer?.release() } catch (e: Exception) {}
        equalizer = null
    }
}

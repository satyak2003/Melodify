package com.melodify.shared.domain.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.co.caprica.vlcj.factory.MediaPlayerFactory

class DesktopEqualizerManager(
    private val onEqChanged: (enabled: Boolean, bands: List<EqBand>, defaultPresetName: String?) -> Unit
) : EqualizerManager {
    private val _isEnabled = MutableStateFlow(false)
    override val isEnabled = _isEnabled.asStateFlow()

    private val _bands = MutableStateFlow<List<EqBand>>(emptyList())
    override val bands = _bands.asStateFlow()

    private val _presets = MutableStateFlow<List<EqPreset>>(emptyList())
    override val presets = _presets.asStateFlow()

    private val _currentPreset = MutableStateFlow<EqPreset?>(null)
    override val currentPreset = _currentPreset.asStateFlow()

    override val minBandLevelMb: Int = -2000
    override val maxBandLevelMb: Int = 2000

    private val factory = try { MediaPlayerFactory() } catch (e: Exception) { null }

    init {
        try {
            val vlcEq = factory?.equalizer()?.newEqualizer()
            if (vlcEq != null) {
                val presetNames = factory.equalizer().presets()
                val presetList = presetNames.mapIndexed { index, name -> EqPreset(name, index) }
                _presets.value = presetList

                val bandFrequencies = listOf(31.25f, 62.5f, 125f, 250f, 500f, 1000f, 2000f, 4000f, 8000f, 16000f)
                val bandList = bandFrequencies.mapIndexed { index, freq -> 
                    EqBand(index, freq.toInt(), 0)
                }
                _bands.value = bandList
            }
        } catch (e: Exception) {
            val defaults = listOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)
            _bands.value = defaults.mapIndexed { index, freq -> EqBand(index, freq, 0) }
        }
    }

    override fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        onEqChanged(enabled, _bands.value, _currentPreset.value?.name)
    }

    override fun setBandLevel(bandIndex: Int, levelMb: Int) {
        val current = _bands.value.toMutableList()
        if (bandIndex in current.indices) {
            current[bandIndex] = current[bandIndex].copy(levelMb = levelMb)
            _bands.value = current
            _currentPreset.value = null
            onEqChanged(_isEnabled.value, current, null)
        }
    }

    override fun usePreset(presetIndex: Int) {
        val preset = _presets.value.find { it.index == presetIndex } ?: return
        try {
            val vlcEq = factory?.equalizer()?.newEqualizer(preset.name)
            if (vlcEq != null) {
                val current = _bands.value.toMutableList()
                for (i in current.indices) {
                    current[i] = current[i].copy(levelMb = (vlcEq.amp(i) * 100).toInt())
                }
                _bands.value = current
                _currentPreset.value = preset
                onEqChanged(_isEnabled.value, current, preset.name)
            }
        } catch (e: Exception) {}
    }
}

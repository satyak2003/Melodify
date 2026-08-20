package com.melodify.desktop.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.*
import org.koin.compose.viewmodel.koinViewModel
import com.melodify.shared.domain.player.EqBand
import com.melodify.shared.domain.player.EqPreset
import com.melodify.shared.presentation.PlayerViewModel

@Composable
fun DesktopEqualizerScreen(viewModel: PlayerViewModel = koinViewModel()) {
    val eqManager = viewModel.equalizerManager
    val isEnabled by eqManager.isEnabled.collectAsState()
    val bands by eqManager.bands.collectAsState()
    val presets by eqManager.presets.collectAsState()
    val currentPreset by eqManager.currentPreset.collectAsState()

    Column(Modifier.fillMaxSize().padding(32.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Equalizer", modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineMedium)
            Switch(checked = isEnabled, onCheckedChange = { eqManager.setEnabled(it) })
        }

        Spacer(Modifier.height(24.dp))

        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text("preset: ${currentPreset?.name ?: "Custom"}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                presets.forEach { preset ->
                    DropdownMenuItem(
                        text = { Text(preset.name) },
                        onClick = {
                            eqManager.usePreset(preset.index)
                            expanded = false
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        var activeBandIndex by remember { mutableStateOf<Int?>(null) }
        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
            bands.forEach { band ->
                val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                val isDragged by interactionSource.collectIsDraggedAsState()
                val isPressed by interactionSource.collectIsPressedAsState()
                val isActive = isDragged || isPressed

                LaunchedEffect(isActive) {
                    if (isActive) {
                        activeBandIndex = band.index
                    } else if (activeBandIndex == band.index) {
                        activeBandIndex = null
                    }
                }

                val targetOpacity = if (activeBandIndex == null || activeBandIndex == band.index) 1f else 0.7f
                val opacity by androidx.compose.animation.core.animateFloatAsState(targetOpacity, label = "opacity")

                val targetScale = if (isActive) 1.15f else 1f
                val scale by androidx.compose.animation.core.animateFloatAsState(targetScale, label = "scale")

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (band.centerFreqHz < 1000) "${band.centerFreqHz}" else "${band.centerFreqHz / 1000}k",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(Modifier.height(16.dp))
                    BoxWithConstraints(Modifier.weight(1f).width(40.dp), contentAlignment = Alignment.Center) {
                        val sliderHeight = this.maxHeight
                        Slider(
                            value = band.levelMb.toFloat(),
                            onValueChange = { eqManager.setBandLevel(band.index, it.toInt()) },
                            valueRange = eqManager.minBandLevelMb.toFloat()..eqManager.maxBandLevelMb.toFloat(),
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .requiredWidth(sliderHeight)
                                .graphicsLayer {
                                    rotationZ = -90f
                                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                                    alpha = opacity
                                    scaleX = scale
                                    scaleY = scale
                                }
                        )
                    }
                }
            }
        }
    }
}
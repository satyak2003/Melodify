package com.melodify.desktop.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.domain.model.durationMs
import com.melodify.shared.domain.model.isPlaying
import com.melodify.shared.domain.model.positionMs
import com.melodify.shared.domain.model.queue
import com.melodify.shared.domain.model.RepeatMode
import com.melodify.shared.domain.model.PlayerState
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SleepOption

@Composable
fun DesktopPlayerBar(
    playerViewModel: PlayerViewModel,
    isExpanded: Boolean = false,
    onOpenNowPlaying: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState by playerViewModel.playerState.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val sleepOption by playerViewModel.sleepOption.collectAsState()
    val sleepRemainingMs by playerViewModel.sleepRemainingMs.collectAsState()
    val track = playerState.currentTrack

    val positionMs = playerState.positionMs
    val durationMs = playerState.durationMs
    val progress = if (durationMs > 0) positionMs.toFloat() / durationMs.toFloat() else 0f
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "progress")

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 16.dp,
        tonalElevation = 4.dp
    ) {
        Column {
            // Thin progress bar at top of the bar
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Transparent,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Left: Track info ──────────────────────────────────────
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (track != null) {
                        key(track.id) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = track.thumbnailUrl,
                                    contentDescription = track.title,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(6.dp)),
                                    contentScale = ContentScale.Crop
                                )

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        track.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        track.artistNames,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            "No track playing",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Center: Playback controls + seek bar ──────────────────
                Column(
                    modifier = Modifier.weight(2f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Shuffle
                        IconButton(
                            onClick = playerViewModel::toggleShuffle,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                modifier = Modifier.size(16.dp),
                                tint = if (queue.isShuffleEnabled) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Previous
                        IconButton(
                            onClick = playerViewModel::playPrevious,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Play/Pause
                        val isPlaying = playerState.isPlaying
                        val isBuffering = playerState is PlayerState.Buffering
                        FilledIconButton(
                            onClick = playerViewModel::togglePlayPause,
                            modifier = Modifier.size(40.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isBuffering) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Next
                        IconButton(
                            onClick = playerViewModel::playNext,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.SkipNext,
                                contentDescription = "Next",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Repeat
                        IconButton(
                            onClick = playerViewModel::cycleRepeatMode,
                            modifier = Modifier.size(32.dp)
                        ) {
                            val repeatTint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                                             else MaterialTheme.colorScheme.onSurfaceVariant
                            Icon(
                                when (queue.repeatMode) {
                                    RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                    else -> Icons.Rounded.Repeat
                                },
                                contentDescription = "Repeat",
                                modifier = Modifier.size(16.dp),
                                tint = repeatTint
                            )
                        }
                    }

                    // Seek slider
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Text(
                            formatBarDuration(positionMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp)
                        )
                        Slider(
                            value = animatedProgress.coerceIn(0f, 1f),
                            onValueChange = { frac ->
                                if (durationMs > 0) playerViewModel.seekTo((frac * durationMs).toLong())
                            },
                            modifier = Modifier.weight(1f).height(16.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                        Text(
                            formatBarDuration(durationMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.width(36.dp)
                        )
                    }
                }

                // ── Right: Volume + Sleep Timer + Now Playing ──────────────
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var volume by remember { mutableFloatStateOf(0.8f) }
                    var previousVolume by remember { mutableFloatStateOf(0.8f) }
                    var showSleepMenu by remember { mutableStateOf(false) }

                    val isMuted = volume <= 0.01f
                    val volumeIcon = when {
                        isMuted -> Icons.Rounded.VolumeOff
                        volume < 0.5f -> Icons.Rounded.VolumeDown
                        else -> Icons.Rounded.VolumeUp
                    }

                    IconButton(
                        onClick = {
                            if (!isMuted) {
                                previousVolume = volume
                                volume = 0f
                                playerViewModel.setVolume(0f)
                            } else {
                                volume = if (previousVolume > 0.05f) previousVolume else 0.8f
                                playerViewModel.setVolume(volume)
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            volumeIcon,
                            contentDescription = if (isMuted) "Unmute" else "Mute",
                            modifier = Modifier.size(20.dp),
                            tint = if (isMuted) MaterialTheme.colorScheme.error
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Slider(
                        value = volume,
                        onValueChange = { newVol ->
                            volume = newVol
                            if (newVol > 0.01f) previousVolume = newVol
                            playerViewModel.setVolume(newVol)
                        },
                        modifier = Modifier.width(90.dp).padding(horizontal = 2.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            activeTrackColor = if (isMuted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )

                    Spacer(Modifier.width(4.dp))

                    // Sleep Timer
                    Box {
                        IconButton(
                            onClick = { showSleepMenu = true },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = "Sleep Timer",
                                modifier = Modifier.size(18.dp),
                                tint = if (sleepOption != SleepOption.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showSleepMenu,
                            onDismissRequest = { showSleepMenu = false }
                        ) {
                            Text(
                                "Sleep Timer",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            SleepOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(option.label)
                                            if (option == sleepOption) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        playerViewModel.setSleepTimer(option)
                                        showSleepMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.width(4.dp))

                    // Expand to Now Playing
                    if (track != null) {
                        IconButton(
                            onClick = onOpenNowPlaying,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                if (isExpanded) Icons.Rounded.Close else Icons.Rounded.OpenInFull,
                                contentDescription = if (isExpanded) "Collapse Now Playing" else "Expand Now Playing",
                                modifier = Modifier.size(18.dp),
                                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatBarDuration(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

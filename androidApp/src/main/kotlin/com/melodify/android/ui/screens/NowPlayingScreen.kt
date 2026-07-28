package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.RepeatMode
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.domain.model.durationMs
import com.melodify.shared.domain.model.isPlaying
import com.melodify.shared.domain.model.positionMs
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SleepOption

@Composable
fun NowPlayingScreen(playerViewModel: PlayerViewModel, onBack: () -> Unit) {
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    val sleepOption by playerViewModel.sleepOption.collectAsStateWithLifecycle()
    val sleepRemainingMs by playerViewModel.sleepRemainingMs.collectAsStateWithLifecycle()
    val track = playerState.currentTrack
    var showSleepTimerMenu by remember { mutableStateOf(false) }

    if (track == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onBackground) }
                    Text("Now Playing", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                Spacer(Modifier.height(16.dp))
                Text("No track playing right now", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                MaterialTheme.colorScheme.background
            ))
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
    ) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(16.dp))
                // Top bar with Sleep Timer
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MaterialTheme.colorScheme.onBackground) }
                    Text("Now Playing", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                    
                    Box {
                        IconButton(onClick = { showSleepTimerMenu = true }) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepOption != SleepOption.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                        DropdownMenu(expanded = showSleepTimerMenu, onDismissRequest = { showSleepTimerMenu = false }) {
                            DropdownMenuItem(text = { Text("Sleep Timer Off") }, onClick = { playerViewModel.setSleepOption(SleepOption.OFF); showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("15 Minutes") }, onClick = { playerViewModel.setSleepOption(SleepOption.MIN_15, 15); showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("30 Minutes") }, onClick = { playerViewModel.setSleepOption(SleepOption.MIN_30, 30); showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("45 Minutes") }, onClick = { playerViewModel.setSleepOption(SleepOption.MIN_45, 45); showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("60 Minutes") }, onClick = { playerViewModel.setSleepOption(SleepOption.MIN_60, 60); showSleepTimerMenu = false })
                            DropdownMenuItem(text = { Text("End of Song") }, onClick = { playerViewModel.setSleepOption(SleepOption.END_OF_TRACK); showSleepTimerMenu = false })
                        }

                    }
                }

                if (sleepOption != SleepOption.OFF) {
                    val label = if (sleepRemainingMs != null) {
                        val sec = sleepRemainingMs!! / 1000
                        "Sleep timer: %d:%02d".format(sec / 60, sec % 60)
                    } else "Sleep timer: ${sleepOption.name.replace("_", " ")}"
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
                        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Album art
                Card(
                    modifier = Modifier.size(280.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
                ) {
                    AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }

                Spacer(Modifier.height(24.dp))

                // Track info
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
                        Text(track.artistNames, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (track.isFlac) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("FLAC", Modifier.padding(8.dp, 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text("HQ", Modifier.padding(8.dp, 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Progress slider
                val position = playerState.positionMs
                val duration = playerState.durationMs
                val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

                Slider(
                    value = progress,
                    onValueChange = { playerViewModel.seekTo((it * duration).toLong()) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary)
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDuration(position), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Spacer(Modifier.height(16.dp))

                // Controls
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = playerViewModel::toggleShuffle) {
                        Icon(Icons.Rounded.Shuffle, null, tint = if (queue.isShuffleEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = playerViewModel::playPrevious, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.SkipPrevious, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                    FilledIconButton(
                        onClick = playerViewModel::togglePlayPause,
                        modifier = Modifier.size(64.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            null,
                            modifier = Modifier.size(38.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    IconButton(onClick = playerViewModel::playNext, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Rounded.SkipNext, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onBackground)
                    }
                    IconButton(onClick = { playerViewModel.cycleRepeatMode() }) {

                        Icon(
                            when (queue.repeatMode) {
                                RepeatMode.OFF -> Icons.Rounded.Repeat
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                RepeatMode.ALL -> Icons.Rounded.Repeat
                            },
                            null,
                            tint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Up Next / Queue section
        if (queue.tracks.isNotEmpty()) {
            item {
                Text(
                    "Up Next (${queue.tracks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(queue.tracks) { index, qTrack ->
                val isCurrent = index == queue.currentIndex
                ListItem(
                    headlineContent = { Text(qTrack.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground) },
                    supportingContent = { Text(qTrack.artistNames, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingContent = {
                        AsyncImage(model = qTrack.thumbnailUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                    },
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (index > 0) {
                                IconButton(onClick = { playerViewModel.moveQueueItemUp(index) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            if (index < queue.tracks.lastIndex) {
                                IconButton(onClick = { playerViewModel.moveQueueItemDown(index) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { playerViewModel.removeFromQueue(index) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .clickable { playerViewModel.skipToIndex(index) }
                )
            }
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

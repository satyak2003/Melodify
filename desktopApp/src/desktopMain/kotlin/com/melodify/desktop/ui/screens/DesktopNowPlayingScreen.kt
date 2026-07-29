package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.RepeatMode
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.domain.model.durationMs
import com.melodify.shared.domain.model.isPlaying
import com.melodify.shared.domain.model.positionMs
import com.melodify.shared.domain.model.queue
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

import com.melodify.shared.presentation.SleepOption

@Composable
fun DesktopNowPlayingScreen(playerViewModel: PlayerViewModel) {
    val playerState by playerViewModel.playerState.collectAsState()
    val queue by playerViewModel.queue.collectAsState()
    val sleepOption by playerViewModel.sleepOption.collectAsState()
    val sleepRemainingMs by playerViewModel.sleepRemainingMs.collectAsState()
    val track = playerState.currentTrack

    if (track == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.MusicNote,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Nothing playing yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    "Search for a song or browse your library",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
        return
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp)
    ) {
        // Left — album art + info + controls
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sleep Timer Banner if active
            if (sleepOption != SleepOption.OFF) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (sleepRemainingMs != null) {
                                val sec = (sleepRemainingMs!! / 1000) % 60
                                val min = (sleepRemainingMs!! / 1000) / 60
                                "Sleep Timer: %02d:%02d".format(min, sec)
                            } else {
                                "Sleep Timer: ${sleepOption.label}"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            key(track.id) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Album art
                    Card(
                        modifier = Modifier.size(340.dp),
                        shape = RoundedCornerShape(24.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 32.dp)
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = track.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Quality badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (track.isFlac) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    "FLAC",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "HQ · 256kbps",
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Track info
                    Text(
                        track.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        track.artistNames,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    track.album?.title?.let { album ->
                        Text(
                            album,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Progress slider
            val position = playerState.positionMs
            val duration = playerState.durationMs
            val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

            Slider(
                value = progress,
                onValueChange = { playerViewModel.seekTo((it * duration).toLong()) },
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    formatDesktopDuration(position),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    formatDesktopDuration(duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(16.dp))

            // Playback controls
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = playerViewModel::toggleShuffle, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = if (queue.isShuffleEnabled) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = playerViewModel::playPrevious, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipPrevious,
                        null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                FilledIconButton(
                    onClick = playerViewModel::togglePlayPause,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = playerViewModel::playNext, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.SkipNext,
                        null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                IconButton(onClick = playerViewModel::cycleRepeatMode, modifier = Modifier.size(48.dp)) {
                    Icon(
                        when (queue.repeatMode) {
                            RepeatMode.ONE -> Icons.Rounded.RepeatOne
                            else -> Icons.Rounded.Repeat
                        },
                        null,
                        modifier = Modifier.size(24.dp),
                        tint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Right — interactive reorderable queue list
        Column(
            modifier = Modifier
                .width(360.dp)
                .fillMaxHeight()
        ) {
            Text(
                "Up Next (${queue.tracks.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (queue.tracks.isEmpty()) {
                Text(
                    "Queue is empty",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(queue.tracks.size, key = { idx -> "${queue.tracks[idx].id}_$idx" }) { index ->
                        val queueTrack = queue.tracks[index]
                        val isPlayingItem = index == queue.currentIndex
                        var dragOffsetY by remember { mutableStateOf(0f) }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPlayingItem) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Rounded.DragHandle,
                                    contentDescription = "Drag to reorder",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .pointerInput(index) {
                                            detectDragGestures(
                                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                                    change.consume()
                                                    dragOffsetY += dragAmount.y
                                                    if (dragOffsetY > 45f && index < queue.tracks.lastIndex) {
                                                        playerViewModel.reorderQueue(index, index + 1)
                                                        dragOffsetY = 0f
                                                    } else if (dragOffsetY < -45f && index > 0) {
                                                        playerViewModel.reorderQueue(index, index - 1)
                                                        dragOffsetY = 0f
                                                    }
                                                },
                                                onDragEnd = { dragOffsetY = 0f },
                                                onDragCancel = { dragOffsetY = 0f }
                                            )
                                        }
                                )

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { playerViewModel.skipToIndex(index) }
                                ) {

                                    Text(
                                        queueTrack.title,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isPlayingItem) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isPlayingItem) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        queueTrack.artistNames,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = { playerViewModel.removeFromQueue(index) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun formatDesktopDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}


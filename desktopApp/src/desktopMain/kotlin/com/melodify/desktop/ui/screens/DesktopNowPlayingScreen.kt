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
import kotlinx.coroutines.launch
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import com.melodify.shared.domain.model.positionMs
import com.melodify.shared.domain.model.queue
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

import com.melodify.shared.presentation.SleepOption
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.blur
import androidx.compose.ui.zIndex
import com.melodify.shared.ui.components.rememberReorderableLazyListState

import com.melodify.shared.presentation.LibraryViewModel

@Composable
fun DesktopNowPlayingScreen(playerViewModel: PlayerViewModel) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val libraryState by libraryViewModel.uiState.collectAsState()
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

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .blur(50.dp)
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f))
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
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

                    Spacer(Modifier.height(16.dp))

                    // Track info
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    track.title,
                                    style = MaterialTheme.typography.displaySmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Spacer(Modifier.width(16.dp))
                                // Quality badge
                                if (track.isFlac) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            "FLAC",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
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
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.secondary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                track.artistNames,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            track.album?.title?.let { album ->
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    album,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                        }
                        
                        val isLiked = (libraryState as? com.melodify.shared.presentation.LibraryUiState.Success)
                            ?.likedTracks?.any { it.id == track.id } == true
                            
                        IconButton(onClick = { libraryViewModel.toggleLike(track) }, modifier = Modifier.size(48.dp)) {
                            Icon(
                                if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
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
                val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
                    playerViewModel.reorderQueue(from, to)
                }

                androidx.compose.foundation.lazy.LazyColumn(
                    state = lazyListState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(queue.tracks.size, key = { idx -> queue.tracks[idx].id }) { index ->
                        val queueTrack = queue.tracks[index]
                        val isPlayingItem = index == queue.currentIndex
                        val isDragging = reorderState.draggingItemIndex == index
                        val scale by animateFloatAsState(targetValue = if (isDragging) 1.04f else 1.0f)

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isPlayingItem) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = if (isDragging) reorderState.draggedDistance else 0f
                                    scaleX = scale
                                    scaleY = scale
                                    shadowElevation = if (isDragging) 8.dp.toPx() else 0f
                                }
                                .zIndex(if (isDragging) 1f else 0f)
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
                                        .pointerInput(queueTrack.id) {
                                            detectDragGestures(
                                                onDragStart = { reorderState.onDragStart(index) },
                                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                                    change.consume()
                                                    reorderState.onDrag(dragAmount)
                                                },
                                                onDragEnd = { reorderState.onDragInterrupted() },
                                                onDragCancel = { reorderState.onDragInterrupted() }
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
}

fun formatDesktopDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}


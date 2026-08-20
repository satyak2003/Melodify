package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
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
import androidx.compose.ui.zIndex
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SleepOption
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import com.melodify.shared.ui.components.rememberReorderableLazyListState
import com.melodify.shared.ui.modifiers.bounceClick
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun NowPlayingContent(
    playerViewModel: PlayerViewModel,
    showAlbumArt: Boolean = true,
    onBack: () -> Unit
) {
    val libraryViewModel: com.melodify.shared.presentation.LibraryViewModel = koinViewModel()
    val libraryState by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val queue by playerViewModel.queue.collectAsStateWithLifecycle()
    val sleepOption by playerViewModel.sleepOption.collectAsStateWithLifecycle()
    val sleepRemainingMs by playerViewModel.sleepRemainingMs.collectAsStateWithLifecycle()
    val activeDevice by playerViewModel.audioOutputManager.activeDevice.collectAsStateWithLifecycle()
    val track = playerState.currentTrack
    var showSleepTimerMenu by remember { mutableStateOf(false) }
    var showCustomTimeDialog by remember { mutableStateOf(false) }
    var customTimeInput by remember { mutableStateOf("") }

    if (track == null) {
        Box(
            modifier = Modifier.fillMaxSize()
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

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        playerViewModel.reorderQueue(from, to)
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
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
                    IconButton(onClick = onBack) { Icon(Icons.Rounded.KeyboardArrowDown, null, tint = androidx.compose.ui.graphics.Color.White) }
                    Text("Now Playing", Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, color = androidx.compose.ui.graphics.Color.White)
                    
                    Box {
                        IconButton(onClick = { showSleepTimerMenu = true }) {
                            Icon(
                                Icons.Rounded.Timer,
                                contentDescription = "Sleep Timer",
                                tint = if (sleepOption != SleepOption.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                if (showSleepTimerMenu) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    ModalBottomSheet(
                        onDismissRequest = { showSleepTimerMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                        ) {
                            Text(
                                "Sleep Timer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(16.dp)
                            )
                            HorizontalDivider()
                            
                            data class SleepTimerOption(val label: String, val option: SleepOption, val minutes: Int? = null)
                            val options = listOf(
                                SleepTimerOption("Sleep Timer Off", SleepOption.OFF),
                                SleepTimerOption("15 Minutes", SleepOption.MIN_15, 15),
                                SleepTimerOption("30 Minutes", SleepOption.MIN_30, 30),
                                SleepTimerOption("45 Minutes", SleepOption.MIN_45, 45),
                                SleepTimerOption("60 Minutes", SleepOption.MIN_60, 60),
                                SleepTimerOption("Custom Time...", SleepOption.CUSTOM),
                                SleepTimerOption("End of Song", SleepOption.END_OF_TRACK)
                            )
                            
                            options.forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (opt.option == SleepOption.CUSTOM) {
                                                showCustomTimeDialog = true
                                                showSleepTimerMenu = false
                                            } else {
                                                if (opt.minutes != null) {
                                                    playerViewModel.setSleepOption(opt.option, opt.minutes)
                                                } else {
                                                    playerViewModel.setSleepOption(opt.option)
                                                }
                                                showSleepTimerMenu = false
                                            }
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (opt.option == SleepOption.OFF) Icons.Rounded.TimerOff else Icons.Rounded.Timer,
                                        contentDescription = null,
                                        tint = if (sleepOption == opt.option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(Modifier.width(16.dp))
                                    Text(
                                        opt.label,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (sleepOption == opt.option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        fontWeight = if (sleepOption == opt.option) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }

                if (showCustomTimeDialog) {
                    AlertDialog(
                        onDismissRequest = { showCustomTimeDialog = false },
                        title = { Text("Custom Sleep Timer") },
                        text = {
                            OutlinedTextField(
                                value = customTimeInput,
                                onValueChange = { if (it.all { char -> char.isDigit() }) customTimeInput = it },
                                label = { Text("Minutes") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                val mins = customTimeInput.toIntOrNull()
                                if (mins != null && mins > 0) {
                                    playerViewModel.setSleepOption(SleepOption.CUSTOM, mins)
                                }
                                showCustomTimeDialog = false
                            }) {
                                Text("Start Timer")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showCustomTimeDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
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

                // Album art & Track info keyed by track.id for reliable updates on track change
                key(track.id) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Card(
                            modifier = Modifier.size(280.dp),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
                        ) {
                            if (showAlbumArt) {
                                AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        // Track info
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(track.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = androidx.compose.ui.graphics.Color.White)
                                Text(track.artistNames, style = MaterialTheme.typography.bodyLarge, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                            }
                            
                            val isLiked = (libraryState as? com.melodify.shared.presentation.LibraryUiState.Success)
                                ?.likedTracks?.any { it.id == track.id } == true
                                
                            IconButton(onClick = { libraryViewModel.toggleLike(track) }) {
                                Icon(
                                    if (isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    contentDescription = "Like",
                                    tint = if (isLiked) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White
                                )
                            }
                            
                            Spacer(Modifier.width(8.dp))
                            
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
                    Text(formatDuration(position), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                    Text(formatDuration(duration), style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                }

                Spacer(Modifier.height(16.dp))

                // Playback Controls
                val coroutineScope = rememberCoroutineScope()
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val shuffleScale = remember { Animatable(1f) }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            shuffleScale.animateTo(0f, animationSpec = tween(150))
                            playerViewModel.toggleShuffle()
                            shuffleScale.animateTo(1f, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                        }
                    }) {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (queue.isShuffleEnabled) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.graphicsLayer {
                                scaleX = shuffleScale.value
                                scaleY = shuffleScale.value
                            }
                        )
                    }
                    Box(Modifier.bounceClick(scaleDown = 0.8f) { playerViewModel.playPrevious() }.padding(12.dp)) {
                        Icon(
                            Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous Track",
                            modifier = Modifier.size(36.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    Box(Modifier.bounceClick(scaleDown = 0.8f) { playerViewModel.togglePlayPause() }) {
                        Surface(
                            modifier = Modifier.size(64.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    modifier = Modifier.size(38.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    Box(Modifier.bounceClick(scaleDown = 0.8f) { playerViewModel.playNext() }.padding(12.dp)) {
                        Icon(
                            Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            modifier = Modifier.size(36.dp),
                            tint = androidx.compose.ui.graphics.Color.White
                        )
                    }
                    val repeatRotation = remember { Animatable(0f) }
                    IconButton(onClick = {
                        coroutineScope.launch {
                            repeatRotation.animateTo(
                                targetValue = repeatRotation.value + 360f,
                                animationSpec = tween(400, easing = FastOutSlowInEasing)
                            )
                        }
                        playerViewModel.cycleRepeatMode()
                    }) {
                        Icon(
                            when (queue.repeatMode) {
                                RepeatMode.OFF -> Icons.Rounded.Repeat
                                RepeatMode.ONE -> Icons.Rounded.RepeatOne
                                RepeatMode.ALL -> Icons.Rounded.Repeat
                            },
                            contentDescription = "Repeat Mode",
                            tint = if (queue.repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.graphicsLayer {
                                rotationZ = repeatRotation.value
                            }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Volume Slider Row
                var volumeState by remember { mutableStateOf(1f) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.VolumeMute,
                        contentDescription = "Volume Mute",
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                    Slider(
                        value = volumeState,
                        onValueChange = {
                            volumeState = it
                            playerViewModel.setVolume(it)
                        },
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Icon(
                        Icons.Rounded.VolumeUp,
                        contentDescription = "Volume Up",
                        tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Output Device Indicator
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val deviceIcon = when (activeDevice.type) {
                        com.melodify.shared.domain.player.AudioDeviceType.BLUETOOTH -> Icons.Rounded.Bluetooth
                        com.melodify.shared.domain.player.AudioDeviceType.WIRED_HEADPHONES -> Icons.Rounded.Headphones
                        else -> Icons.Rounded.Speaker
                    }
                    Icon(
                        deviceIcon,
                        contentDescription = "Output Device",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = activeDevice.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            itemsIndexed(queue.tracks, key = { index, item -> "${item.id}_$index" }) { index, qTrack ->
                val isCurrent = index == queue.currentIndex
                val isDragging = reorderState.draggingItemIndex == index
                val scale by animateFloatAsState(targetValue = if (isDragging) 1.04f else 1.0f)

                ListItem(
                    headlineContent = { Text(qTrack.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal, color = if (isCurrent) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.White) },
                    supportingContent = { Text(qTrack.artistNames, maxLines = 1, overflow = TextOverflow.Ellipsis, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f)) },
                    leadingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.DragHandle,
                                contentDescription = "Drag to reorder",
                                tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .pointerInput(qTrack.id) {
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
                            AsyncImage(model = qTrack.thumbnailUrl, contentDescription = null, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { playerViewModel.removeFromQueue(index) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Remove", tint = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f))
                        }
                    },
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                        .graphicsLayer {
                            translationY = if (isDragging) reorderState.draggedDistance else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                        }
                        .zIndex(if (isDragging) 1f else 0f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else androidx.compose.ui.graphics.Color(0x22FFFFFF))
                        .clickable { playerViewModel.skipToIndex(index) },
                    colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent)
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

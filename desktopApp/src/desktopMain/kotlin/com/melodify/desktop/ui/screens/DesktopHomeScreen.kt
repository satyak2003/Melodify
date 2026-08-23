package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.melodify.shared.ui.modifiers.shimmerEffect

import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.graphics.Color
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import com.melodify.shared.presentation.HomeUiState
import com.melodify.shared.presentation.HomeViewModel
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SleepOption

import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieAnimation
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import org.jetbrains.compose.resources.ExperimentalResourceApi
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode

@Composable
fun DesktopHomeScreen(
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel,
    onNavigateToEqualizer: () -> Unit = {}
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()

    val localPlaylists: List<Playlist> = (libraryState as? com.melodify.shared.presentation.LibraryUiState.Success)?.localPlaylists ?: emptyList()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Diagonal background circles
        Box(
            modifier = Modifier
                .size(400.dp)
                .offset(x = (-100).dp, y = (-100).dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), shape = CircleShape)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(500.dp)
                .offset(x = 150.dp, y = 150.dp)
                .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.05f), shape = CircleShape)
        )

        when (val state = homeState) {
            is HomeUiState.Loading -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Spacer(Modifier.height(300.dp)) // Header Banner space
                    }
                    item {
                        Text(
                            "Jump Back In",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 16.dp, horizontal = 16.dp)
                        )
                    }
                    item {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            repeat(2) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    repeat(4) {
                                        Box(modifier = Modifier.weight(1f).height(64.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            "Most Played",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(horizontal = 16.dp), userScrollEnabled = false) {
                            items(6) {
                                Column(modifier = Modifier.width(180.dp)) {
                                    Box(modifier = Modifier.size(180.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                                    Spacer(Modifier.height(12.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                    Spacer(Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                                }
                            }
                        }
                    }
                }
            }
            is HomeUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { homeViewModel.refresh() }) {
                        Text("Retry")
                    }
                }
            }
            is HomeUiState.Success -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(24.dp)
                ) {
                    // Header Banner
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.Transparent
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp)
                            ) {
                                                                // EQ Button & Audio Output
                                val activeDevice by playerViewModel.audioOutputManager.activeDevice.collectAsState()
                                val deviceIcon = when (activeDevice.type) {
                                    com.melodify.shared.domain.player.AudioDeviceType.BLUETOOTH -> Icons.Rounded.Bluetooth
                                    com.melodify.shared.domain.player.AudioDeviceType.WIRED_HEADPHONES -> Icons.Rounded.Headphones
                                    else -> Icons.Rounded.Speaker
                                }
                                Surface(
                                    modifier = Modifier.align(Alignment.TopStart).clickable { onNavigateToEqualizer() },
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(deviceIcon, contentDescription = "Audio Output", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.width(8.dp))
                                        Text("EQ", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

val lottieBytes by produceState<ByteArray?>(null) {
                                    value = runCatching { 
                                        val stream = object {}.javaClass.getResourceAsStream("/composeResources/com.melodify.shared.resources/files/melodify_text.json")
                                            ?: object {}.javaClass.classLoader.getResourceAsStream("composeResources/com.melodify.shared.resources/files/melodify_text.json")
                                        stream?.readBytes()
                                    }.getOrNull()
                                }

                                Column(modifier = Modifier.align(Alignment.CenterEnd)) {
                                    if (lottieBytes != null) {
                                        val composition by rememberLottieComposition(
                                            LottieCompositionSpec.JsonString(lottieBytes!!.decodeToString())
                                        )
                                        if (composition != null) {
                                            LottieAnimation(
                                                composition = composition,
                                                iterations = Compottie.IterateForever,
                                                modifier = Modifier
                                                    .height(80.dp)
                                                    .width(400.dp)
                                                    .scale(1.5f)
                                                    .graphicsLayer { alpha = 0.99f }
                                                    .drawWithContent {
                                                        drawContent()
                                                        drawRect(
                                                            brush = Brush.horizontalGradient(
                                                                colors = listOf(Color.Transparent, Color.Black),
                                                                startX = 0f,
                                                                endX = size.width * 0.2f
                                                            ),
                                                            blendMode = BlendMode.DstIn
                                                        )
                                                    }
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Welcome to Melodify",
                                            style = MaterialTheme.typography.headlineLarge,
                                            fontFamily = FontFamily.Default,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Sleep Timer Dedicated Card
                    item {
                        val sleepRemainingMs by playerViewModel.sleepRemainingMs.collectAsState()
                        if (sleepRemainingMs != null) {
                            val remainingSec = sleepRemainingMs!! / 1000
                            val text = "Sleep Timer Active: %02d:%02d remaining".format(remainingSec / 60, remainingSec % 60)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.weight(1f))
                                    TextButton(onClick = { playerViewModel.setSleepTimer(SleepOption.OFF) }) {
                                        Text("Cancel")
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }

                    // 1. Jump Back In
                    if (state.jumpBackInTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Jump Back In",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                items(state.jumpBackInTracks) { track ->
                                    DesktopTrackCard(
                                        track = track, localPlaylists = localPlaylists,
                                        onClick = { playerViewModel.playTracks(state.jumpBackInTracks, state.jumpBackInTracks.indexOf(track)) },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onDownload = { playerViewModel.downloadTrack(track) },
                                        onAddToPlaylist = { libraryViewModel.addTrackToPlaylist(it, track) }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Most Played
                    item {
                        Text(
                            "Most Played",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    item {
                        if (state.mostPlayedTracks.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                items(state.mostPlayedTracks) { track ->
                                    DesktopTrackCard(
                                        track = track, localPlaylists = localPlaylists,
                                        onClick = { playerViewModel.playTracks(state.mostPlayedTracks, state.mostPlayedTracks.indexOf(track)) },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onDownload = { playerViewModel.downloadTrack(track) },
                                        onAddToPlaylist = { libraryViewModel.addTrackToPlaylist(it, track) }
                                    )
                                }
                            }
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                item {
                                    Box(
                                        modifier = Modifier.height(140.dp).padding(horizontal = 16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Bruh, more data needed",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Recommended Tracks
                    if (state.recommendedTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Recommended For You",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                items(state.recommendedTracks) { track ->
                                    DesktopTrackCard(
                                        track = track, localPlaylists = localPlaylists,
                                        onClick = { playerViewModel.playTracks(state.recommendedTracks, state.recommendedTracks.indexOf(track)) },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onDownload = { playerViewModel.downloadTrack(track) },
                                        onAddToPlaylist = { libraryViewModel.addTrackToPlaylist(it, track) }
                                    )
                                }
                            }
                        }
                    }

                    // 4. Moods
                    item {
                        Text(
                            "Feeling Like?",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(state.moods) { mood ->
                                val isSelected = state.selectedMood == mood
                                Surface(
                                    modifier = Modifier.clickable { homeViewModel.loadMoodTracks(mood) },
                                    shape = CircleShape,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                ) {
                                    Text(
                                        text = mood,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    item {
                        androidx.compose.animation.AnimatedVisibility(visible = state.selectedMood != null) {
                            Column {
                                val moodTracks = state.selectedMoodTracks
                                if (moodTracks == null) {
                                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator()
                                    }
                                } else if (moodTracks.isEmpty()) {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text("No tracks found for ${state.selectedMood}")
                                    }
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                        contentPadding = PaddingValues(bottom = 16.dp)
                                    ) {
                                        items(moodTracks) { track ->
                                            DesktopTrackCard(
                                                track = track, localPlaylists = localPlaylists,
                                                onClick = { playerViewModel.playTracks(moodTracks, moodTracks.indexOf(track)) },
                                                onAddToQueue = { playerViewModel.addToQueue(track) },
                                                onDownload = { playerViewModel.downloadTrack(track) },
                                                onAddToPlaylist = { libraryViewModel.addTrackToPlaylist(it, track) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // 5. Offline Ready
                    if (state.offlineTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Offline Ready",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                items(state.offlineTracks) { track ->
                                    DesktopTrackCard(
                                        track = track, localPlaylists = localPlaylists,
                                        onClick = { playerViewModel.playTracks(state.offlineTracks, state.offlineTracks.indexOf(track)) },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onDownload = { playerViewModel.downloadTrack(track) },
                                        onAddToPlaylist = { libraryViewModel.addTrackToPlaylist(it, track) }
                                    )
                                }
                            }
                        }
                    }
                    
                    // 6. Live FM Radio
                    if (state.fmStations.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Live FM Radio",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                var showFmFilter by remember { mutableStateOf(false) }
                                Box {
                                    IconButton(onClick = { showFmFilter = true }) {
                                        Icon(Icons.Rounded.FilterList, contentDescription = "Filter by Country", tint = MaterialTheme.colorScheme.onBackground)
                                    }
                                    DropdownMenu(expanded = showFmFilter, onDismissRequest = { showFmFilter = false }) {
                                        DropdownMenuItem(text = { Text("Global (Default)") }, onClick = { homeViewModel.loadFmStationsForCountry(null); showFmFilter = false })
                                        DropdownMenuItem(text = { Text("United States") }, onClick = { homeViewModel.loadFmStationsForCountry("US"); showFmFilter = false })
                                        DropdownMenuItem(text = { Text("United Kingdom") }, onClick = { homeViewModel.loadFmStationsForCountry("GB"); showFmFilter = false })
                                        DropdownMenuItem(text = { Text("India") }, onClick = { homeViewModel.loadFmStationsForCountry("IN"); showFmFilter = false })
                                        DropdownMenuItem(text = { Text("Germany") }, onClick = { homeViewModel.loadFmStationsForCountry("DE"); showFmFilter = false })
                                        DropdownMenuItem(text = { Text("Canada") }, onClick = { homeViewModel.loadFmStationsForCountry("CA"); showFmFilter = false })
                                    }
                                }
                            }
                        }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                                items(state.fmStations) { station ->
                                    Card(
                                        modifier = Modifier
                                            .width(140.dp)
                                            .height(140.dp)
                                            .clickable {
                                                val fakeTrack = com.melodify.shared.domain.model.Track(
                                                    id = station.id,
                                                    title = station.name,
                                                    artists = emptyList(),
                                                    album = null,
                                                    durationMs = 0L,
                                                    thumbnailUrl = station.favicon,
                                                    source = com.melodify.shared.domain.model.TrackSource.LOCAL,
                                                    localPath = station.streamUrl
                                                )
                                                playerViewModel.playTrack(fakeTrack)
                                            },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            if (station.favicon.isNotEmpty()) {
                                                AsyncImage(
                                                    model = station.favicon,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp).clip(CircleShape),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.GraphicEq,
                                                    null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(48.dp)
                                                )
                                            }
                                            Spacer(Modifier.height(16.dp))
                                            Text(station.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("Live Radio", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
}

@Composable
fun DesktopMusicTimelineChart(stats: Map<String, Int>) {
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Music Timeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                Text("Listening Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(16.dp))
            val maxVal = (stats.values.maxOrNull() ?: 60).coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                days.forEach { day ->
                    val minutes = stats[day] ?: 0
                    val barHeightFraction = (minutes.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        val timeStr = if (minutes >= 60) {
                            val hrs = minutes / 60
                            val mins = minutes % 60
                            if (mins > 0) "${hrs}h${mins}m" else "${hrs}h"
                        } else {
                            "${minutes}m"
                        }
                        Text(timeStr, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier.height(65.dp).fillMaxWidth(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(20.dp)
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(day, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

        }
    }
}

@Composable
fun DesktopTrackCard(
    track: Track,
    localPlaylists: List<Playlist>,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistSubmenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .width(180.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    com.melodify.desktop.ui.components.DesktopTrackOptionsPanel(
                        track = track,
                        isVisible = showMenu,
                        allPlaylists = localPlaylists,
                        isCustomPlaylist = false,
                        onDismiss = { showMenu = false },
                        onAddToQueue = onAddToQueue,
                        onDownload = onDownload,
                        onAddToPlaylist = onAddToPlaylist,
                        onRemoveFromPlaylist = {}
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = track.artistNames,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun DesktopTrackListItem(
    track: Track,
    allPlaylists: List<Playlist> = emptyList(),
    onClick: () -> Unit,
    onAddToQueue: () -> Unit = {},
    onDownload: () -> Unit = {},
    onAddToPlaylist: (String) -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistSubmenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(model = track.thumbnailUrl, contentDescription = track.title, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onBackground)
            Text(track.artistNames, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            com.melodify.desktop.ui.components.DesktopTrackOptionsPanel(
                track = track,
                isVisible = showMenu,
                allPlaylists = allPlaylists,
                isCustomPlaylist = false,
                onDismiss = { showMenu = false },
                onAddToQueue = onAddToQueue,
                onDownload = onDownload,
                onAddToPlaylist = onAddToPlaylist,
                onRemoveFromPlaylist = {}
            )
        }
    }
}


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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import com.melodify.shared.presentation.HomeUiState
import com.melodify.shared.presentation.HomeViewModel
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel

@Composable
fun DesktopHomeScreen(
    playerViewModel: PlayerViewModel,
    libraryViewModel: LibraryViewModel,
    homeViewModel: HomeViewModel
) {
    val homeState by homeViewModel.uiState.collectAsState()
    val libraryState by libraryViewModel.uiState.collectAsState()

    val localPlaylists: List<Playlist> = (libraryState as? com.melodify.shared.presentation.LibraryUiState.Success)?.localPlaylists ?: emptyList()


    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = homeState) {
            is HomeUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
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
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                MaterialTheme.colorScheme.primaryContainer,
                                                MaterialTheme.colorScheme.surfaceVariant
                                            )
                                        )
                                    )
                                    .padding(24.dp)
                            ) {
                                Column(modifier = Modifier.align(Alignment.CenterStart)) {
                                    Text(
                                        text = "Welcome to Melodify",
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "Discover trending music or play your local library.",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Hero Row: Surprise Me & Continue Listening
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                modifier = Modifier.weight(1f).height(90.dp).clickable {
                                    if (state.trending.isNotEmpty()) {
                                        playerViewModel.playTracks(state.trending.shuffled(), 0)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(modifier = Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text("Surprise Me!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text("Play a random song", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                    Icon(Icons.Rounded.PlayArrow, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                }
                            }

                            val lastTrack = state.lastPlayedTrack
                            if (lastTrack != null) {
                                Card(
                                    modifier = Modifier.weight(1f).height(90.dp).clickable {
                                        playerViewModel.playTrack(lastTrack)
                                        if (state.lastPlayedPositionMs > 0) playerViewModel.seekTo(state.lastPlayedPositionMs)
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(modifier = Modifier.fillMaxSize().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        AsyncImage(model = lastTrack.thumbnailUrl, contentDescription = null, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Crop)
                                        Spacer(Modifier.width(12.dp))
                                        Column(Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.PlayCircleOutline, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Continue Listening", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                            }
                                            Spacer(Modifier.height(2.dp))
                                            Text(lastTrack.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                    }

                    // Music Timeline Chart Card
                    item {
                        DesktopMusicTimelineChart(stats = state.weeklyStats)
                        Spacer(Modifier.height(20.dp))
                    }

                    if (state.trending.isNotEmpty()) {
                        item {
                            Text(
                                "Trending Now",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(state.trending) { track ->
                                    DesktopTrackCard(
                                        track = track,
                                        localPlaylists = localPlaylists,
                                        onClick = {
                                            playerViewModel.playTracks(
                                                state.trending,
                                                state.trending.indexOf(track)
                                            )
                                        },
                                        onAddToQueue = { playerViewModel.addToQueue(track) },
                                        onDownload = { playerViewModel.downloadTrack(track) },
                                        onAddToPlaylist = { playlistId ->
                                            libraryViewModel.addTrackToPlaylist(playlistId, track)
                                        }
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
                Text("Minutes Listened", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Text("${minutes}m", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = {
                            showMenu = false
                            showPlaylistSubmenu = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue") },
                            leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                            onClick = {
                                onAddToQueue()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Download") },
                            leadingIcon = { Icon(Icons.Rounded.Download, null) },
                            onClick = {
                                onDownload()
                                showMenu = false
                            }
                        )
                        if (localPlaylists.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Add to Playlist ▶") },
                                leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                                onClick = { showPlaylistSubmenu = !showPlaylistSubmenu }
                            )
                            if (showPlaylistSubmenu) {
                                localPlaylists.forEach { playlist ->
                                    DropdownMenuItem(
                                        text = { Text("  └ ${playlist.title}") },
                                        onClick = {
                                            onAddToPlaylist(playlist.id)
                                            showMenu = false
                                            showPlaylistSubmenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
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
    localPlaylists: List<Playlist> = emptyList(),
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
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false; showPlaylistSubmenu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                    onClick = { onAddToQueue(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Download") },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = { onDownload(); showMenu = false }
                )
                if (localPlaylists.isNotEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Add to Playlist") },
                        leadingIcon = { Icon(Icons.Rounded.PlaylistAdd, null) },
                        onClick = { showPlaylistSubmenu = !showPlaylistSubmenu }
                    )
                    if (showPlaylistSubmenu) {
                        localPlaylists.forEach { playlist ->
                            DropdownMenuItem(
                                text = { Text("  ${playlist.title}", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                onClick = { onAddToPlaylist(playlist.id); showMenu = false; showPlaylistSubmenu = false }
                            )
                        }
                    }
                }
            }
        }
    }
}


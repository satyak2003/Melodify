package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import com.melodify.desktop.ui.components.DesktopTrackOptionsPanel
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalUriHandler
import coil3.compose.AsyncImage
import com.melodify.shared.data.storage.TrackDownloader
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.ImportProgress
import com.melodify.shared.presentation.LibraryUiState
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopLibraryScreen(playerViewModel: PlayerViewModel) {
    val viewModel: LibraryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val downloadingTracks by playerViewModel.downloadingTracks.collectAsState()
    val isOffline by com.melodify.shared.utils.NetworkMonitor.isOffline.collectAsState()
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var showImportLinkDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistLinkInput by remember { mutableStateOf("") }
    var newPlaylistTitleInput by remember { mutableStateOf("") }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }

    // Import Link Dialog
    if (showImportLinkDialog) {
        AlertDialog(
            onDismissRequest = { showImportLinkDialog = false },
            title = { Text("Import Playlist", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Paste a Spotify playlist link:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = playlistLinkInput,
                        onValueChange = { playlistLinkInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("https://open.spotify.com/playlist/...") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (playlistLinkInput.isNotBlank()) {
                        viewModel.importPlaylistFromLink(playlistLinkInput)
                        playlistLinkInput = ""
                        showImportLinkDialog = false
                    }
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImportLinkDialog = false }) { Text("Cancel") }
            }
        )
    }

    // Create Playlist Dialog
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Column {
                    Text("Enter a name for your new playlist:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPlaylistTitleInput,
                        onValueChange = { newPlaylistTitleInput = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("My Favorites") }
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPlaylistTitleInput.isNotBlank()) {
                        viewModel.createLocalPlaylist(newPlaylistTitleInput)
                        newPlaylistTitleInput = ""
                        showCreatePlaylistDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var playlistToDelete by remember { mutableStateOf<Playlist?>(null) }

    if (showDeleteDialog && playlistToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Playlist") },
            text = { Text("Are you sure you want to delete '${playlistToDelete?.title}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        playlistToDelete?.let {
                            viewModel.deletePlaylist(it.id)
                            if (selectedPlaylist?.id == it.id) {
                                selectedPlaylist = null
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Left column: Playlist list (320dp) ─────────────────────────────
        Column(
            modifier = Modifier
                .width(320.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Create Playlist Button
                    IconButton(
                        onClick = { showCreatePlaylistDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Create playlist",
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Link Import Button
                    IconButton(
                        onClick = { showImportLinkDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = "Import link",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Prominent Import Local Music Files Button
            Button(
                onClick = {
                    try {
                        val dialog = java.awt.FileDialog(null as java.awt.Frame?, "Select Local Music Files", java.awt.FileDialog.LOAD)
                        dialog.isMultipleMode = true
                        dialog.isVisible = true
                        val files = dialog.files
                        if (files != null && files.isNotEmpty()) {
                            val paths = files.map { it.absolutePath }
                            viewModel.importLocalMusicFiles(paths)
                        }
                    } catch (e: Exception) {
                        println("Local file import error: ${e.message}")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Import Local Music", fontWeight = FontWeight.Bold)
            }


            Spacer(Modifier.height(4.dp))

            // Sync YouTube Music button
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        val tokens = AuthManager.loginWithGoogle()
                        viewModel.importYouTubePlaylists(tokens?.accessToken)
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                )
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sync YouTube Music", fontWeight = FontWeight.Bold)
            }

            // Import progress
            importProgress?.let { progress ->
                Spacer(Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            progress.currentPlaylist.ifEmpty { "Importing…" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (progress.currentTrack.isNotEmpty()) {
                            Text(
                                progress.currentTrack,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progress.percentage },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "${progress.imported}/${progress.total}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Playlist list
            when (val state = uiState) {
                is LibraryUiState.Loading -> {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }

                is LibraryUiState.Error -> {
                    Column(
                        Modifier.fillMaxWidth().weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(state.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadLibrary() }) { Text("Retry") }
                    }
                }

                is LibraryUiState.Success -> {
                    val allPlaylists = state.spotifyPlaylists + state.localPlaylists
                    if (allPlaylists.isEmpty() && state.likedTracks.isEmpty()) {
                        Box(
                            Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Rounded.LibraryMusic,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No playlists yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Liked songs
                            if (state.likedTracks.isNotEmpty()) {
                                item {
                                    LibraryPlaylistRow(
                                        title = "Liked Songs",
                                        subtitle = "${state.likedTracks.size} songs",
                                        thumbnailUrl = null,
                                        isLiked = true,
                                        isSelected = selectedPlaylist?.id == "liked",
                                        onClick = {
                                            val fakePlaylist = Playlist(
                                                id = "liked",
                                                title = "Liked Songs",
                                                tracks = state.likedTracks
                                            )
                                            selectedPlaylist = fakePlaylist
                                        }
                                    )
                                }
                            }

                            if (state.localPlaylists.isNotEmpty()) {
                                item {
                                    Text(
                                        "My Playlists",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                                items(state.localPlaylists) { playlist ->
                                    LibraryPlaylistRow(
                                        title = playlist.title,
                                        subtitle = "${playlist.trackCount} songs",
                                        thumbnailUrl = playlist.thumbnailUrl,
                                        isSelected = selectedPlaylist?.id == playlist.id,
                                        onClick = { selectedPlaylist = playlist }
                                    )
                                }
                            }

                            if (state.spotifyPlaylists.isNotEmpty()) {
                                item {
                                    Text(
                                        "Spotify",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                                items(state.spotifyPlaylists) { playlist ->
                                    LibraryPlaylistRow(
                                        title = playlist.title,
                                        subtitle = "${playlist.trackCount} songs",
                                        thumbnailUrl = playlist.thumbnailUrl,
                                        isSelected = selectedPlaylist?.id == playlist.id,
                                        onClick = { selectedPlaylist = playlist }
                                    )
                                }
                            }

                            if (state.youtubePlaylists.isNotEmpty()) {
                                item {
                                    Text(
                                        "YouTube Music",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                                    )
                                }
                                items(state.youtubePlaylists) { playlist ->
                                    LibraryPlaylistRow(
                                        title = playlist.title,
                                        subtitle = "${playlist.trackCount} songs",
                                        thumbnailUrl = playlist.thumbnailUrl,
                                        isSelected = selectedPlaylist?.id == playlist.id,
                                        onClick = { selectedPlaylist = playlist }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Right column: Track list ────────────────────────────────────────
        val playlist = selectedPlaylist
        val localPlaylists = (uiState as? LibraryUiState.Success)?.localPlaylists ?: emptyList()

        if (playlist == null) {
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Select a playlist to view tracks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            TrackListPanel(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                playlist = playlist,
                currentTrack = playerState.currentTrack,
                isOffline = isOffline,
                allPlaylists = localPlaylists + ((uiState as? LibraryUiState.Success)?.spotifyPlaylists ?: emptyList()),
                downloadingTracks = downloadingTracks,
                onPlayAll = { playerViewModel.playTracks(playlist.tracks, 0) },
                onPlayTrack = { index -> playerViewModel.playTracks(playlist.tracks, index) },
                onDownloadTrack = playerViewModel::downloadTrack,
                onAddToQueue = { track -> playerViewModel.addToQueue(track) },
                onAddToPlaylist = viewModel::addTrackToPlaylist,
                onRemoveFromPlaylist = { trackId -> viewModel.removeTrackFromPlaylist(playlist.id, trackId) },
                onDeletePlaylist = {
                    playlistToDelete = playlist
                    showDeleteDialog = true
                }
            )
        }
    }
}

// ── Playlist row ────────────────────────────────────────────────────────────

@Composable
private fun LibraryPlaylistRow(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    isSelected: Boolean,
    isLiked: Boolean = false,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                  else MaterialTheme.colorScheme.surface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail
        Box(
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (isLiked) {
                Icon(Icons.Rounded.Favorite, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            } else if (thumbnailUrl != null) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Rounded.MusicNote, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

// ── Track list panel ────────────────────────────────────────────────────────

@Composable
private fun TrackListPanel(
    modifier: Modifier = Modifier,
    playlist: Playlist,
    currentTrack: Track?,
    isOffline: Boolean,
    allPlaylists: List<Playlist>,
    downloadingTracks: Map<String, Float>,
    onPlayAll: () -> Unit,
    onPlayTrack: (Int) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onAddToPlaylist: (String, Track) -> Unit,
    onRemoveFromPlaylist: (String) -> Unit,
    onDeletePlaylist: () -> Unit
) {
    val listState = rememberLazyListState()

    Column(modifier = modifier.padding(start = 0.dp)) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist cover
            Box(
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (playlist.thumbnailUrl != null) {
                    AsyncImage(
                        model = playlist.thumbnailUrl,
                        contentDescription = playlist.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote, null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(20.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    playlist.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!playlist.description.isNullOrBlank()) {
                    Text(
                        playlist.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                val totalSeconds = playlist.tracks.sumOf { it.durationSeconds }
                val hours = totalSeconds / 3600
                val minutes = (totalSeconds % 3600) / 60
                val durationString = buildString {
                    if (hours > 0) append("$hours hr ")
                    append("$minutes min")
                }

                Text(
                    "${playlist.tracks.size} songs • $durationString",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            val anyDownloading = playlist.tracks.any { downloadingTracks.containsKey(it.id) }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                if (anyDownloading) {
                    val downloadingList = playlist.tracks.filter { downloadingTracks.containsKey(it.id) }
                    val progressSum = downloadingList.mapNotNull { downloadingTracks[it.id] }.sum()
                    val overallProgress = if (downloadingList.isNotEmpty()) progressSum / downloadingList.size else 0f
                    CircularProgressIndicator(
                        progress = { overallProgress },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp
                    )
                    IconButton(onClick = {}) {
                        Icon(Icons.Rounded.Download, contentDescription = "Downloading", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    }
                } else {
                    IconButton(
                        onClick = { playlist.tracks.forEach { onDownloadTrack(it) } }
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = "Download All", modifier = Modifier.size(24.dp))
                    }
                }
            }
            
            Spacer(Modifier.width(8.dp))

            // Play all button
            FilledIconButton(
                onClick = onPlayAll,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play All", modifier = Modifier.size(32.dp))
            }
            
            if (playlist.id.startsWith("local_")) {
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = onDeletePlaylist,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Rounded.Delete, 
                        contentDescription = "Delete Playlist", 
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 32.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        )

        if (playlist.tracks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.MusicOff, null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "No tracks in this playlist",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Column headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp))
                Text("Title", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f))
                Text("Artist", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(0.6f))
                Text("Duration", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(56.dp))
                Text("Actions", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(72.dp))
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                itemsIndexed(playlist.tracks, key = { idx, item -> "${item.id}_$idx" }) { index, track ->

                    val isCurrentlyPlaying = currentTrack != null && (
                        currentTrack?.id == track.id ||
                        (currentTrack?.youtubeVideoId != null && currentTrack?.youtubeVideoId == track.youtubeVideoId)
                    )

                    TrackRow(
                        index = index + 1,
                        track = track,
                        isPlaying = isCurrentlyPlaying,
                        isDownloading = downloadingTracks.containsKey(track.id),
                        downloadProgress = downloadingTracks[track.id],
                        isCustomPlaylist = playlist.id.startsWith("local_"),
                        isOffline = isOffline,
                        allPlaylists = allPlaylists,
                        onClick = { onPlayTrack(index) },
                        onDownload = { onDownloadTrack(track) },
                        onAddToQueue = { onAddToQueue(track) },
                        onAddToPlaylist = { playlistId -> onAddToPlaylist(playlistId, track) },
                        onRemoveFromPlaylist = { onRemoveFromPlaylist(track.id) }
                    )
                }
            }
        }
    }
}

// ── Individual track row ────────────────────────────────────────────────────

@Composable
private fun TrackRow(
    index: Int,
    track: Track,
    isPlaying: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float?,
    isCustomPlaylist: Boolean,
    isOffline: Boolean,
    allPlaylists: List<Playlist>,
    onClick: () -> Unit,
    onDownload: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onRemoveFromPlaylist: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val isDownloaded = remember(track) { TrackDownloader.isDownloaded(track) }
    var showMenu by remember { mutableStateOf(false) }
    
    val isAvailable = isDownloaded || !isOffline
    val rowAlpha = if (isAvailable) 1f else 0.4f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isPlaying) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else MaterialTheme.colorScheme.surface
            )
            .clickable(enabled = isAvailable, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .alpha(rowAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Index / playing indicator
        Box(modifier = Modifier.width(32.dp), contentAlignment = Alignment.Center) {
            if (isPlaying) {
                Icon(
                    Icons.Rounded.Equalizer,
                    contentDescription = "Playing",
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    "$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariant
                )
            }
        }

        // Thumbnail
        Box(
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (track.thumbnailUrl != null) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = track.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    null,
                    tint = onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.align(Alignment.Center).size(20.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        // Title
        Text(
            track.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isPlaying) FontWeight.Bold else FontWeight.Medium,
            color = if (isPlaying) primaryColor else onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )

        // Artist
        Text(
            track.artistNames,
            style = MaterialTheme.typography.bodySmall,
            color = onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.6f)
        )

        // Duration
        Text(
            formatTrackDuration(track),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(56.dp)
        )


        // Actions
        Row(modifier = Modifier.width(72.dp), horizontalArrangement = Arrangement.End) {
            if (isDownloading) {
                CircularProgressIndicator(
                    progress = { downloadProgress ?: 0f },
                    modifier = Modifier.size(18.dp), 
                    strokeWidth = 2.dp
                )
            } else if (isDownloaded) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = "Downloaded",
                    tint = primaryColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                IconButton(onClick = onDownload, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = "Download",
                        modifier = Modifier.size(16.dp),
                        tint = onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = "Options",
                        modifier = Modifier.size(16.dp),
                        tint = onSurfaceVariant
                    )
                }

                DesktopTrackOptionsPanel(
                    track = track,
                    isVisible = showMenu,
                    allPlaylists = allPlaylists,
                    isCustomPlaylist = isCustomPlaylist,
                    onDismiss = { showMenu = false },
                    onAddToQueue = onAddToQueue,
                    onDownload = onDownload,
                    onAddToPlaylist = onAddToPlaylist,
                    onRemoveFromPlaylist = onRemoveFromPlaylist
                )
            }
        }
    }
}


private fun formatTrackDuration(track: Track): String {
    val ms = if (track.durationMs > 0) track.durationMs else (track.durationSeconds * 1000L)
    if (ms <= 0L) return "--:--"
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}



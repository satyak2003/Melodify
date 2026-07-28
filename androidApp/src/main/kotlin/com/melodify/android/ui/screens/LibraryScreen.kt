package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.presentation.ImportProgress
import com.melodify.shared.presentation.LibraryUiState
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.melodify.shared.api.spotify.SpotifyAuthHelper
import com.melodify.android.ui.navigation.Screen
import org.koin.compose.viewmodel.koinViewModel



@Composable
fun LibraryScreen(
    navController: NavController,
    playerViewModel: PlayerViewModel
) {
    val viewModel: LibraryViewModel = koinViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val importProgress by viewModel.importProgress.collectAsStateWithLifecycle()
    val isSpotifyConnected by viewModel.isSpotifyConnected.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current

    val context = androidx.compose.ui.platform.LocalContext.current
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var playlistTitleInput by remember { mutableStateOf("") }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val paths = uris.mapNotNull { uri ->
                try {
                    val localDir = java.io.File(context.filesDir, "local_music")
                    if (!localDir.exists()) localDir.mkdirs()
                    val fileName = "local_${System.currentTimeMillis()}_${(1000..9999).random()}.mp3"
                    val destFile = java.io.File(localDir, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                    destFile.absolutePath
                } catch (e: Exception) {
                    println("Failed to copy audio URI $uri: ${e.message}")
                    null
                }
            }
            if (paths.isNotEmpty()) {
                viewModel.importLocalMusicFiles(paths)
            }
        }
    }


    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = { showCreatePlaylistDialog = false },
            title = { Text("Create Playlist") },
            text = {
                OutlinedTextField(
                    value = playlistTitleInput,
                    onValueChange = { playlistTitleInput = it },
                    label = { Text("Playlist Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (playlistTitleInput.isNotBlank()) {
                            viewModel.createLocalPlaylist(playlistTitleInput)
                            playlistTitleInput = ""
                            showCreatePlaylistDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false }) { Text("Cancel") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Your Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.navigate(Screen.About.route) }) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = "About",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    IconButton(onClick = { showCreatePlaylistDialog = true }) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = "Create Playlist",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Import Local Music Files & Import Link row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { audioPickerLauncher.launch("audio/*") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            ) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Local Audio", fontWeight = FontWeight.Bold)
            }

            if (isSpotifyConnected) {
                OutlinedButton(
                    onClick = { viewModel.logoutSpotify() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Logout Spotify")
                }
            }
        }

        // Spotify import progress
        importProgress?.let { progress ->
            ImportProgressCard(progress)
        }

        // Spotify connect button
        if (!isSpotifyConnected && importProgress == null) {
            SpotifyConnectCard(
                onConnect = {
                    val authUrl = viewModel.startSpotifyLogin(SpotifyAuthHelper.ANDROID_REDIRECT_URI)
                    uriHandler.openUri(authUrl)
                }
            )
        }

        when (val state = uiState) {
            is LibraryUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is LibraryUiState.Success -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                    if (state.spotifyPlaylists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Spotify Playlists",
                                icon = Icons.Rounded.LibraryMusic
                            )
                        }
                        items(state.spotifyPlaylists) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                                }
                            )
                        }
                    }

                    if (state.localPlaylists.isNotEmpty()) {
                        item {
                            SectionHeader(
                                title = "Local Playlists",
                                icon = Icons.Rounded.Folder
                            )
                        }
                        items(state.localPlaylists) { playlist ->
                            PlaylistItem(
                                playlist = playlist,
                                onClick = {
                                    navController.navigate(Screen.PlaylistDetail.createRoute(playlist.id))
                                }
                            )
                        }
                    }
                }
            }
            is LibraryUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun SpotifyConnectCard(onConnect: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onConnect),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            ) {
                Icon(
                    Icons.Rounded.LibraryMusic,
                    null,
                    modifier = Modifier.padding(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Import Spotify Playlists",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "Connect to import your playlists",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun ImportProgressCard(progress: ImportProgress) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Importing from Spotify...",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (progress.currentPlaylist.isNotEmpty()) {
                Text(
                    progress.currentPlaylist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress.percentage },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${progress.imported} / ${progress.total} tracks",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PlaylistItem(playlist: Playlist, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                playlist.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Text(
                "${playlist.trackCount} songs • ${playlist.source.name.lowercase().replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            if (playlist.thumbnailUrl != null) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Surface(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        null,
                        modifier = Modifier.padding(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
fun LikedSongsCard(count: Int, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    Icons.Rounded.Favorite,
                    null,
                    modifier = Modifier.padding(14.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Liked Songs",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    "$count songs",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            Icon(
                Icons.Rounded.PlayCircle,
                null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun EmptyLibraryState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.LibraryMusic,
            null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Your library is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Import your Spotify playlists or add local music files",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

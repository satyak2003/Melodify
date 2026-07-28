package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import com.melodify.shared.presentation.LibraryUiState
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PlaylistDetailScreen(
    playlistId: String,
    playerViewModel: PlayerViewModel,
    onBack: () -> Unit
) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val uiState by libraryViewModel.uiState.collectAsStateWithLifecycle()

    val playlist: Playlist? = when (val state = uiState) {
        is LibraryUiState.Success -> {
            (state.spotifyPlaylists + state.localPlaylists).firstOrNull { it.id == playlistId }
        }
        else -> null
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        if (playlist != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                // Header Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        MaterialTheme.colorScheme.background
                                    )
                                )
                            )
                    ) {
                        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)
                            ) {
                                Card(
                                    modifier = Modifier.size(130.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
                                ) {
                                    if (playlist.thumbnailUrl != null) {
                                        AsyncImage(
                                            model = playlist.thumbnailUrl,
                                            contentDescription = playlist.title,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.MusicNote, null, modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }

                                Spacer(Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            playlist.source.name,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        playlist.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "${playlist.tracks.size} songs",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Controls Row: Big Play Circle + Shuffle Button
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(
                                onClick = {
                                    if (playlist.tracks.isNotEmpty()) {
                                        playerViewModel.playTracks(playlist.tracks.shuffled(), 0)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Rounded.Shuffle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Shuffle")
                            }
                        }

                        Button(
                            onClick = {
                                if (playlist.tracks.isNotEmpty()) {
                                    playerViewModel.playTracks(playlist.tracks, 0)
                                }
                            },
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play All", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Song list with Track Numbers
                itemsIndexed(playlist.tracks) { index, track ->
                    SpotifyStyleTrackItem(
                        index = index + 1,
                        track = track,
                        onClick = { playerViewModel.playTracks(playlist.tracks, index) },
                        onAddToQueue = { playerViewModel.addToQueue(track) },
                        onDownload = { playerViewModel.downloadTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpotifyStyleTrackItem(
    index: Int,
    track: Track,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$index",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(28.dp)
        )

        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = track.title,
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                track.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                track.artistNames,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Add to Queue") },
                    leadingIcon = { Icon(Icons.Rounded.QueueMusic, null) },
                    onClick = { onAddToQueue(); showMenu = false }
                )
                DropdownMenuItem(
                    text = { Text("Download Track") },
                    leadingIcon = { Icon(Icons.Rounded.Download, null) },
                    onClick = { onDownload(); showMenu = false }
                )
            }
        }
    }
}

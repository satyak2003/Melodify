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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.melodify.shared.presentation.LibraryUiState
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.presentation.SearchUiState
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel


@Composable
fun DesktopSearchScreen(playerViewModel: PlayerViewModel) {
    val viewModel: SearchViewModel = koinViewModel()
    val query by viewModel.query.collectAsState()
    val searchState by viewModel.searchResults.collectAsState()
    val searchHistory by viewModel.searchHistory.collectAsState()

    val libraryViewModel: LibraryViewModel = koinViewModel()
    val libraryState by libraryViewModel.uiState.collectAsState()
    val localPlaylists = (libraryState as? LibraryUiState.Success)?.localPlaylists ?: emptyList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Text(
            "Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = viewModel::updateQuery,
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = viewModel::clearSearch) {
                        Icon(Icons.Rounded.Clear, null)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )

        Spacer(Modifier.height(16.dp))

        // Recent search history chips
        if (searchHistory.isNotEmpty() && query.isBlank()) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Recent Searches",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = viewModel::clearHistory) {
                    Text("Clear All", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(searchHistory) { historyItem ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { viewModel.updateQuery(historyItem) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.History,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                historyItem,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .clickable { viewModel.removeHistoryItem(historyItem) }
                                    .padding(2.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

        }

        Spacer(Modifier.height(12.dp))

        when (val state = searchState) {
            is SearchUiState.Empty -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.Search,
                            null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Search for music, artists, or albums",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is SearchUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is SearchUiState.Success -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    if (state.results.tracks.isNotEmpty()) {
                        item {
                            Text(
                                "Songs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(state.results.tracks) { track ->
                            DesktopTrackListItem(
                                track = track,
                                localPlaylists = localPlaylists,
                                onClick = {
                                    playerViewModel.playTracks(
                                        state.results.tracks,
                                        state.results.tracks.indexOf(track)
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


            is SearchUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.ErrorOutline,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

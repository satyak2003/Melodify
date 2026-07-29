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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.melodify.shared.data.storage.SearchHistoryStorage
import com.melodify.shared.domain.model.Track
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.presentation.SearchUiState
import com.melodify.shared.presentation.SearchViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(navController: NavController, playerViewModel: PlayerViewModel) {
    val viewModel: SearchViewModel = koinViewModel()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchState by viewModel.searchResults.collectAsStateWithLifecycle()
    
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Top bar with About icon
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Search",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { navController.navigate("about") }) {
                Icon(
                    Icons.Rounded.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        val isCreatorMode by viewModel.isCreatorMode.collectAsStateWithLifecycle()
        val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()

        SearchBar(
            query = query,
            onQueryChange = viewModel::updateQuery,
            onSearch = {},
            active = false,
            onActiveChange = {},
            placeholder = { Text(if (isCreatorMode) "Search cinematic & creator safe audio..." else "Search songs, artists, albums...") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = if (query.isNotEmpty()) { { IconButton(onClick = viewModel::clearSearch) { Icon(Icons.Rounded.Clear, null) } } } else null,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.FilterChip(
                selected = isCreatorMode,
                onClick = viewModel::toggleCreatorMode,
                label = { Text(if (isCreatorMode) "🎬 Creator Mode: ON" else "🎬 Creator Mode") }
            )
        }

        if (isCreatorMode) {
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(com.melodify.shared.presentation.CreatorCategory.entries.toTypedArray()) { cat ->
                    androidx.compose.material3.FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { viewModel.setCreatorCategory(cat) },
                        label = { Text(cat.displayName) }
                    )
                }
            }
        }
        
        when (val state = searchState) {
            is SearchUiState.Empty -> SearchEmptyState(onQuerySelect = viewModel::updateQuery)
            is SearchUiState.Loading -> CircularProgressIndicator(modifier = Modifier.fillMaxWidth().padding(32.dp).wrapContentWidth())

            is SearchUiState.Success -> {
                LazyColumn {
                    if (state.results.tracks.isNotEmpty()) {
                        item { SectionHeader("Songs") }
                        items(state.results.tracks) { track ->
                            TrackListItem(
                                track = track,
                                onClick = { playerViewModel.playTracks(state.results.tracks, state.results.tracks.indexOf(track)) },
                                onAddToQueue = { playerViewModel.addToQueue(track) },
                                onDownload = { playerViewModel.downloadTrack(track) }
                            )
                        }
                    }
                }
            }
            is SearchUiState.Error -> ErrorMessage(state.message)
        }
    }
}

@Composable
fun SearchEmptyState(onQuerySelect: (String) -> Unit) {
    var history by remember { mutableStateOf(SearchHistoryStorage.loadHistory()) }

    if (history.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Search for songs, artists, albums", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                TextButton(
                    onClick = {
                        SearchHistoryStorage.clearHistory()
                        history = emptyList()
                    }
                ) {
                    Text("Clear All")
                }
            }
            Spacer(Modifier.height(8.dp))
            LazyColumn {
                items(history) { item ->
                    ListItem(
                        headlineContent = { Text(item) },
                        leadingContent = { Icon(Icons.Rounded.History, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                        modifier = Modifier.clickable { onQuerySelect(item) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackListItem(
    track: Track,
    onClick: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onDownload: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text("${track.artistNames} • ${track.album?.title ?: ""}", maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        leadingContent = {
            AsyncImage(model = track.thumbnailUrl, contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)), contentScale = ContentScale.Crop)
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (track.isFlac) {
                    Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.padding(end = 4.dp)) {
                        Text("FLAC", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "Options", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Add to Queue") },
                            leadingIcon = { Icon(Icons.Rounded.Queue, null) },
                            onClick = {
                                onAddToQueue?.invoke()
                                showMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Download Track") },
                            leadingIcon = { Icon(Icons.Rounded.Download, null) },
                            onClick = {
                                onDownload?.invoke()
                                showMenu = false
                            }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

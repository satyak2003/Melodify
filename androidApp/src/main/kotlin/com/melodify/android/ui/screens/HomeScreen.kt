package com.melodify.android.ui.screens

import kotlinx.coroutines.launch

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.melodify.android.ui.navigation.Screen
import com.melodify.shared.api.radio.FmStation
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.domain.model.Artist
import com.melodify.shared.domain.model.Album
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track
import com.melodify.shared.domain.model.TrackSource
import com.melodify.shared.presentation.HomeUiState
import com.melodify.shared.presentation.HomeViewModel
import com.melodify.shared.presentation.PlayerViewModel
import com.melodify.shared.ui.modifiers.bounceClick
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(navController: NavController, playerViewModel: PlayerViewModel) {
    val viewModel = koinViewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val userProfileUrl by AuthManager.userProfileUrl.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
    ) {
        item {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Melodify",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "Good ${getTimeGreeting()}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .bounceClick(scaleDown = 0.85f) {
                                navController.navigate(Screen.Equalizer.route)
                            }
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Rounded.GraphicEq,
                            contentDescription = "Equalizer",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "EQ",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.85f) {
                                navController.navigate(Screen.Settings.route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .bounceClick(scaleDown = 0.85f) {
                                navController.navigate(Screen.Profile.route)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (userProfileUrl != null) {
                            AsyncImage(
                                model = userProfileUrl,
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Rounded.AccountCircle,
                                contentDescription = "Profile",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        val state = uiState
        if (state is HomeUiState.Loading) {
            item {
                SectionHeader("Jump Back In")
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(2) {
                                Box(modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                SectionHeader("Most Played")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    userScrollEnabled = false
                ) {
                    items(5) {
                        Column(modifier = Modifier.width(140.dp)) {
                            Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            item {
                SectionHeader("Recommended For You")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    userScrollEnabled = false
                ) {
                    items(5) {
                        Column(modifier = Modifier.width(140.dp)) {
                            Box(modifier = Modifier.size(140.dp).clip(RoundedCornerShape(12.dp)).shimmerEffect())
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.8f).height(16.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                            Spacer(Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth(0.5f).height(12.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        } else if (state is HomeUiState.Success) {
            // Jump Back In (Recently Played Grid)
            if (state.jumpBackInTracks.isNotEmpty()) {
                item {
                    SectionHeader("Jump Back In")
                    val recentTracks = state.jumpBackInTracks.take(4)
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val chunked = recentTracks.chunked(2)
                        chunked.forEach { rowTracks ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowTracks.forEach { track ->
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(56.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.5f))
                                            .clickable { playerViewModel.playTrack(track) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = track.thumbnailUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = track.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                }
                                if (rowTracks.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            
            // Resume Last Played
            val lastPlayedTrackLocal = state.lastPlayedTrack
            if (lastPlayedTrackLocal != null) {
                item {
                    SectionHeader("Resume Last Played")
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .bounceClick(scaleDown = 0.95f) {
                                playerViewModel.playTrack(lastPlayedTrackLocal, state.lastPlayedPositionMs)
                            },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = lastPlayedTrackLocal.thumbnailUrl,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = lastPlayedTrackLocal.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = lastPlayedTrackLocal.artists.joinToString { it.name },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            val progress = if (state.lastPlayedDurationMs > 0) {
                                state.lastPlayedPositionMs.toFloat() / state.lastPlayedDurationMs.toFloat()
                            } else 0f
                            LinearProgressIndicator(
                                progress = progress,
                                modifier = Modifier.fillMaxWidth().height(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            if (state.recommendedTracks.isNotEmpty()) {
                item {
                    SectionHeader("Recommendations")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.recommendedTracks) { track ->
                            TrackCard(track) { playerViewModel.playTrack(track) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }

            item {
                SectionHeader("Most Played")
                if (state.mostPlayedTracks.size >= 3) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.mostPlayedTracks) { track ->
                            TrackCard(track) { playerViewModel.playTrack(track) }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .padding(horizontal = 16.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Bruh, more data needed",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            item {
                SectionHeader("Feeling like?")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp)
                ) {
                    items(state.moods) { mood ->
                        val isSelected = state.selectedMood == mood
                        Surface(
                            modifier = Modifier.clickable { 
                                viewModel.loadMoodTracks(mood)
                            },
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
                
                // Show mood tracks if selected
                androidx.compose.animation.AnimatedVisibility(visible = state.selectedMood != null) {
                    Column {
                        Spacer(Modifier.height(16.dp))
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
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(moodTracks) { track ->
                                    TrackCard(track) { playerViewModel.playTrack(track) }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
            
            if (state.offlineTracks.isNotEmpty()) {
                item {
                    SectionHeader("Offline Ready")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.offlineTracks) { track ->
                            TrackCard(track) { playerViewModel.playTrack(track) }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
            
            if (state.fmStations.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
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
                                DropdownMenuItem(text = { Text("Global (Default)") }, onClick = { viewModel.loadFmStationsForCountry(null); showFmFilter = false })
                                DropdownMenuItem(text = { Text("United States") }, onClick = { viewModel.loadFmStationsForCountry("US"); showFmFilter = false })
                                DropdownMenuItem(text = { Text("United Kingdom") }, onClick = { viewModel.loadFmStationsForCountry("GB"); showFmFilter = false })
                                DropdownMenuItem(text = { Text("India") }, onClick = { viewModel.loadFmStationsForCountry("IN"); showFmFilter = false })
                                DropdownMenuItem(text = { Text("Germany") }, onClick = { viewModel.loadFmStationsForCountry("DE"); showFmFilter = false })
                                DropdownMenuItem(text = { Text("Canada") }, onClick = { viewModel.loadFmStationsForCountry("CA"); showFmFilter = false })
                            }
                        }
                    }
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        items(state.fmStations) { station ->
                            Card(
                                modifier = Modifier
                                    .width(140.dp)
                                    .height(140.dp)
                                    .bounceClick(scaleDown = 0.95f) {
                                        val fakeTrack = Track(
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
                    Spacer(Modifier.height(24.dp))
                }
            }

        } else if (state is HomeUiState.Error) {
            item {
                Text(
                    text = state.message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun TrackCard(track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .bounceClick(scaleDown = 0.95f) { onClick() }
    ) {
        AsyncImage(
            model = track.thumbnailUrl,
            contentDescription = null,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artists.joinToString { it.name },
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun getTimeGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..11 -> "Morning"
        in 12..16 -> "Afternoon"
        else -> "Evening"
    }
}

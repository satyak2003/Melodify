package com.melodify.desktop.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Playlist
import com.melodify.shared.domain.model.Track

@Composable
fun DesktopTrackOptionsPanel(
    track: Track?,
    isVisible: Boolean,
    allPlaylists: List<Playlist>,
    isCustomPlaylist: Boolean,
    onDismiss: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onAddToPlaylist: (String) -> Unit,
    onRemoveFromPlaylist: () -> Unit
) {
    if (!isVisible && track == null) return

    Popup(
        alignment = Alignment.CenterEnd,
        onDismissRequest = onDismiss
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
                    .padding(vertical = 16.dp, horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Options", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    if (track != null) {
                        // Track Info
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AsyncImage(
                                model = track.thumbnailUrl,
                                contentDescription = track.title,
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(track.artistNames, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        
                        Spacer(Modifier.height(24.dp))
                        
                        // Actions
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OptionItem(icon = Icons.Rounded.QueueMusic, label = "Add to Queue") {
                                onAddToQueue()
                                onDismiss()
                            }
                            OptionItem(icon = Icons.Rounded.Download, label = "Download Track") {
                                onDownload()
                                onDismiss()
                            }
                            if (isCustomPlaylist) {
                                OptionItem(icon = Icons.Rounded.Delete, label = "Remove from Playlist", color = MaterialTheme.colorScheme.error) {
                                    onRemoveFromPlaylist()
                                    onDismiss()
                                }
                            }
                        }
                        
                        if (allPlaylists.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(Modifier.height(16.dp))
                            Text("Add to Playlist", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allPlaylists) { pl ->
                                    OptionItem(icon = Icons.Rounded.PlaylistAdd, label = pl.title) {
                                        onAddToPlaylist(pl.id)
                                        onDismiss()
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
private fun OptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

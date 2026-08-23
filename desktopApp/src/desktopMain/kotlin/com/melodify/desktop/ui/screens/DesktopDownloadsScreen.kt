package com.melodify.desktop.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.melodify.shared.domain.download.DownloadItem
import com.melodify.shared.domain.download.DownloadQuality
import com.melodify.shared.domain.download.DownloadQueueManager
import com.melodify.shared.domain.download.DownloadState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopDownloadsScreen() {
    val downloads by DownloadQueueManager.downloads.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text("Downloads Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }

        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "No downloads yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DesktopDownloadItemRow(item)
                }
            }
        }
    }
}

@Composable
fun DesktopDownloadItemRow(item: DownloadItem) {
    var showDialog by remember { mutableStateOf(false) }
    val state = item.state

    if (showDialog && state is DownloadState.Failed) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Download Failed") },
            text = { Text("Error: ${state.error}") },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    DownloadQueueManager.remove(item.id)
                    DownloadQueueManager.enqueue(item.track, item.quality)
                }) {
                    Text("Retry")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (item.state is DownloadState.Failed) {
                showDialog = true
            }
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.track.artistNames, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                Spacer(Modifier.height(8.dp))
                
                when (val st = item.state) {
                    is DownloadState.Queued -> {
                        Text("Queued (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(progress = { st.progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Downloading ${(st.progress * 100).toInt()}% (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Paused -> {
                        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Paused (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is DownloadState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            Spacer(Modifier.width(4.dp))
                            Text("Completed (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        }
                    }
                    is DownloadState.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Failed: ${st.error.take(40)}...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state is DownloadState.Downloading) {
                    IconButton(onClick = { DownloadQueueManager.pause(item.id) }) {
                        Icon(Icons.Rounded.Pause, contentDescription = "Pause")
                    }
                } else if (state is DownloadState.Paused) {
                    IconButton(onClick = { DownloadQueueManager.resume(item.id) }) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Resume")
                    }
                }
                
                IconButton(onClick = { DownloadQueueManager.remove(item.id) }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Remove")
                }
            }
        }
    }
}

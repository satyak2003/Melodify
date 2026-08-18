package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.melodify.shared.domain.download.DownloadItem
import com.melodify.shared.domain.download.DownloadQueueManager
import com.melodify.shared.domain.download.DownloadState
import coil3.compose.AsyncImage

@Composable
fun DesktopDownloadsScreen() {
    val downloads by DownloadQueueManager.downloads.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text("Downloads Manager", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(24.dp))

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
                modifier = Modifier.fillMaxSize(),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.track.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.track.artistNames, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                Spacer(Modifier.height(8.dp))
                
                when (val state = item.state) {
                    is DownloadState.Queued -> {
                        Text("Queued (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Paused -> {
                        Text("Paused", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(6.dp))
                        Text("Downloading ${(state.progress * 100).toInt()}% (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            Spacer(Modifier.width(6.dp))
                            Text("Completed (${item.quality.name})", style = MaterialTheme.typography.labelMedium, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        }
                    }
                    is DownloadState.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text("Failed: ${state.error}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.width(16.dp))
            IconButton(onClick = { DownloadQueueManager.remove(item.id) }) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove")
            }
        }
    }
}

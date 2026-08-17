package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodify.shared.domain.download.DownloadItem
import com.melodify.shared.domain.download.DownloadQuality
import com.melodify.shared.domain.download.DownloadQueueManager
import com.melodify.shared.domain.download.DownloadState
import com.melodify.shared.ui.modifiers.bounceClick
import coil3.compose.AsyncImage
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.clickable
import com.melodify.android.ui.components.DownloadErrorDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(navController: NavController, onBack: () -> Unit) {
    val downloads by DownloadQueueManager.downloads.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Downloads Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (downloads.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
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
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(downloads, key = { it.id }) { item ->
                    DownloadItemRow(item)
                }
            }
        }
    }
}

@Composable
fun DownloadItemRow(item: DownloadItem) {
    var showDialog by remember { mutableStateOf(false) }

    val state = item.state
    if (showDialog && state is DownloadState.Failed) {
        DownloadErrorDialog(
            errorDetails = state.error,
            onDismiss = { showDialog = false },
            onRetry = {
                showDialog = false
                DownloadQueueManager.remove(item.id)
                DownloadQueueManager.enqueue(item.track, item.quality)
            },
            onDownloadNormal = {
                showDialog = false
                DownloadQueueManager.remove(item.id)
                DownloadQueueManager.enqueue(item.track, DownloadQuality.NORMAL)
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable {
            if (item.state is DownloadState.Failed) {
                showDialog = true
            }
        },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.track.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.track.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.track.artistNames, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                
                Spacer(Modifier.height(4.dp))
                
                when (val state = item.state) {
                    is DownloadState.Queued -> {
                        Text("Queued (${item.quality.name})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Downloading -> {
                        LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Downloading ${(state.progress * 100).toInt()}% (${item.quality.name})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    is DownloadState.Paused -> {
                        LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth().height(4.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Paused (${item.quality.name})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    is DownloadState.Completed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp), tint = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                            Spacer(Modifier.width(4.dp))
                            Text("Completed (${item.quality.name})", style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color(0xFF4CAF50))
                        }
                    }
                    is DownloadState.Failed -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Error, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Failed: ${state.error.take(20)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
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

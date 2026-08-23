package com.melodify.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.Track
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOptionsBottomSheet(
    track: Track,
    onDismissRequest: () -> Unit,
    onPlayNext: () -> Unit = {},
    onAddToQueue: () -> Unit = {},
    onLikeToggle: () -> Unit = {},
    onDownload: () -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    allPlaylists: List<com.melodify.shared.domain.model.Playlist> = emptyList(),
    onAddToPlaylist: ((String) -> Unit)? = null,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            // Header with track info
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = track.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.artistNames,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Options
            BottomSheetItem(
                icon = Icons.Rounded.PlayArrow,
                text = "Play Next",
                onClick = { onPlayNext(); onDismissRequest() }
            )
            BottomSheetItem(
                icon = Icons.Rounded.QueueMusic,
                text = "Add to Queue",
                onClick = { onAddToQueue(); onDismissRequest() }
            )
            BottomSheetItem(
                icon = if (track.isLiked) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                text = if (track.isLiked) "Remove from Liked" else "Like",
                onClick = { onLikeToggle(); onDismissRequest() }
            )
            BottomSheetItem(
                icon = Icons.Rounded.Download,
                text = "Download",
                onClick = { onDownload(); onDismissRequest() }
            )
            
            if (onRemoveFromPlaylist != null) {
                BottomSheetItem(
                    icon = Icons.Rounded.Delete,
                    text = "Remove from Playlist",
                    onClick = { onRemoveFromPlaylist(); onDismissRequest() },
                    tint = MaterialTheme.colorScheme.error
                )
            }
            
            if (allPlaylists.isNotEmpty() && onAddToPlaylist != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Add to Playlist:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
                allPlaylists.forEach { pl ->
                    BottomSheetItem(
                        icon = Icons.Rounded.PlaylistAdd,
                        text = pl.title,
                        onClick = { onAddToPlaylist(pl.id); onDismissRequest() }
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

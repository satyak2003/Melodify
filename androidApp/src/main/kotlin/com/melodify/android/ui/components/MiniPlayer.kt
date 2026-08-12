package com.melodify.android.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.domain.model.durationMs
import com.melodify.shared.domain.model.isPlaying
import com.melodify.shared.domain.model.positionMs
import com.melodify.shared.presentation.PlayerViewModel


import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import com.melodify.shared.ui.modifiers.bounceClick
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope

@Composable
fun MiniPlayerContent(
    viewModel: PlayerViewModel,
    onClick: () -> Unit
) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val track = playerState.currentTrack ?: return
    val progress = if (playerState.durationMs > 0) playerState.positionMs.toFloat() / playerState.durationMs.toFloat() else 0f
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    if (dragAmount < -20f) {
                        onClick()
                    }
                }
            }
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color(0xCC000000)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0x33FFFFFF))
    ) {
        Box {
            // Drag handle indicator at top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .padding(top = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f))
                        .align(Alignment.TopCenter)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            // Progress bar at bottom of card
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(2.dp).align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary
            )
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.runtime.key(track.id) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = track.thumbnailUrl, 
                            contentDescription = null, 
                            modifier = Modifier.size(48.dp)
                                .clip(RoundedCornerShape(8.dp)), 
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = androidx.compose.ui.graphics.Color.White)
                            Text(track.artistNames, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall, color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
                Box(Modifier.bounceClick(scaleDown = 0.7f) { viewModel.playPrevious() }.padding(12.dp)) {
                    Icon(Icons.Rounded.SkipPrevious, null, tint = androidx.compose.ui.graphics.Color.White)
                }
                Box(Modifier.bounceClick(scaleDown = 0.7f) { viewModel.togglePlayPause() }.padding(12.dp)) {
                    Icon(if (playerState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, null, tint = androidx.compose.ui.graphics.Color.White)
                }
                Box(Modifier.bounceClick(scaleDown = 0.7f) { viewModel.playNext() }.padding(12.dp)) {
                    Icon(Icons.Rounded.SkipNext, null, tint = androidx.compose.ui.graphics.Color.White)
                }
            }
        }
    }
}

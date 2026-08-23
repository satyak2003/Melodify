package com.melodify.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.melodify.android.ui.screens.NowPlayingContent
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.PlayerViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class PlayerDragState { Collapsed, Expanded }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PlayerBottomSheet(
    playerViewModel: PlayerViewModel,
    bottomNavHeight: androidx.compose.ui.unit.Dp,
    showPlayer: Boolean = true,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val playerState by playerViewModel.playerState.collectAsStateWithLifecycle()
    val track = playerState.currentTrack
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val coroutineScope = rememberCoroutineScope()

    val collapsedHeight = 90.dp
    val expandedHeight = screenHeight

    
    val minHeightPx = with(density) { collapsedHeight.toPx() }
    val bottomNavHeightPx = with(density) { bottomNavHeight.toPx() }

    androidx.compose.foundation.layout.BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxHeightPx = constraints.maxHeight.toFloat()
val decayAnimationSpec = androidx.compose.animation.rememberSplineBasedDecay<Float>()
    val draggableState = remember(decayAnimationSpec) {
        AnchoredDraggableState<PlayerDragState>(
            initialValue = PlayerDragState.Collapsed,
            anchors = DraggableAnchors<PlayerDragState> {
                PlayerDragState.Collapsed at (maxHeightPx - minHeightPx - bottomNavHeightPx)
                PlayerDragState.Expanded at 0f
            },
            positionalThreshold = { distance: Float -> distance * 0.3f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(dampingRatio = 1f, stiffness = 400f),
            decayAnimationSpec = decayAnimationSpec
        )
    }

    val offsetValue = draggableState.offset
    val offset = if (java.lang.Float.isNaN(offsetValue)) (maxHeightPx - minHeightPx - bottomNavHeightPx) else offsetValue
    val totalDistance = (maxHeightPx - minHeightPx - bottomNavHeightPx)
    val rawProgress = if (totalDistance == 0f) 0f else (1f - (offset / totalDistance)).coerceIn(0f, 1f)

    // Smooth progress follows swipe exactly
    val progress = rawProgress

    // Animated values for transitions
    val albumArtScale = 1f + (1.2f * progress) // 48dp -> ~105dp midway
    val albumArtTranslationY = -120f * progress // Moves up to top portion
    val albumArtTranslationX = 0f
    
    val controlsAlpha = (1f - progress).coerceIn(0f, 1f)
    val nowPlayingAlpha = progress.coerceIn(0f, 1f)
    val backgroundAlpha = (0.3f + 0.6f * progress).coerceIn(0f, 1f)
    val miniPlayerTranslationY = 20f * progress // Slight upward movement

    
        content()




        if (track != null && showPlayer) {
            val isExpanded = draggableState.currentValue == PlayerDragState.Expanded
            val isAnimating = draggableState.targetValue != draggableState.currentValue

            // Back handler to collapse when expanded
            androidx.activity.compose.BackHandler(enabled = isExpanded || isAnimating) {
                coroutineScope.launch {
                    draggableState.animateTo(PlayerDragState.Collapsed)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, offset.roundToInt()) }
                    .anchoredDraggable(
                        state = draggableState,
                        orientation = Orientation.Vertical
                    )
            ) {
                // Background overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = backgroundAlpha }
                        .background(Color.Black.copy(alpha = 0.85f))
                )

                // Main content area with both mini player and now playing
                Box(modifier = Modifier.fillMaxSize()) {
                    // Mini Player Content - scales and fades out
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(collapsedHeight)
                            .graphicsLayer {
                                alpha = controlsAlpha
                                translationY = miniPlayerTranslationY
                                scaleX = 1f - (0.05f * progress)
                                scaleY = 1f - (0.05f * progress)
                            }
                    ) {
                        MiniPlayerContent(
                            viewModel = playerViewModel,
                            showAlbumArt = progress == 0f,
                            onClick = {
                                coroutineScope.launch {
                                    draggableState.animateTo(PlayerDragState.Expanded)
                                }
                            }
                        )
                    }

                    // Now Playing Content - fades and scales in
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                alpha = nowPlayingAlpha
                                translationY = (1f - progress) * 50f
                                scaleX = 0.95f + (0.05f * progress)
                                scaleY = 0.95f + (0.05f * progress)
                            }
                    ) {
                        if (progress > 0.05f) {
                            NowPlayingContent(
                                playerViewModel = playerViewModel,
                                showAlbumArt = progress == 1f,
                                onBack = {
                                    coroutineScope.launch {
                                        draggableState.animateTo(PlayerDragState.Collapsed)
                                    }
                                }
                            )
                        }
                    }

                    // Floating Shared Element Album Art
                    if (progress > 0f && progress < 1f) {
                        val imageSize = androidx.compose.ui.unit.lerp(48.dp, 280.dp, progress)
                        val startX = 20.dp
                        val endX = (configuration.screenWidthDp.dp - 280.dp) / 2
                        val currentX = androidx.compose.ui.unit.lerp(startX, endX, progress)
                        
                        val startY = 8.dp
                        val endY = 88.dp
                        val currentY = androidx.compose.ui.unit.lerp(startY, endY, progress)
                        
                        val cornerRadius = androidx.compose.ui.unit.lerp(8.dp, 20.dp, progress)
                        
                        coil3.compose.AsyncImage(
                            model = track.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .offset(x = currentX, y = currentY)
                                .size(imageSize)
                                .clip(RoundedCornerShape(cornerRadius)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                }
            }
        }
        // Render BottomBar dynamically so it is above the sheet when collapsed, and animates out when sheet expands
        if (bottomNavHeight > 0.dp) {
            Box(
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .graphicsLayer {
                        alpha = (1f - (progress * 4f)).coerceIn(0f, 1f)
                        translationY = progress * 150f
                    }
            ) {
                bottomBar()
            }
        }
    }
}
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

    val collapsedHeight = 72.dp
    val expandedHeight = screenHeight

    val maxHeightPx = with(density) { expandedHeight.toPx() }
    val minHeightPx = with(density) { collapsedHeight.toPx() }
    val bottomNavHeightPx = with(density) { bottomNavHeight.toPx() }

    val decayAnimationSpec = androidx.compose.animation.rememberSplineBasedDecay<Float>()
    val draggableState = remember(decayAnimationSpec) {
        AnchoredDraggableState<PlayerDragState>(
            initialValue = PlayerDragState.Collapsed,
            anchors = DraggableAnchors<PlayerDragState> {
                PlayerDragState.Collapsed at (maxHeightPx - minHeightPx - bottomNavHeightPx - with(density) { 4.dp.toPx() })
                PlayerDragState.Expanded at 0f
            },
            positionalThreshold = { distance: Float -> distance * 0.3f },
            velocityThreshold = { with(density) { 200.dp.toPx() } },
            snapAnimationSpec = spring(dampingRatio = 0.9f, stiffness = 1500f),
            decayAnimationSpec = decayAnimationSpec
        )
    }

    val offsetValue = draggableState.offset
    val offset = if (java.lang.Float.isNaN(offsetValue)) (maxHeightPx - minHeightPx - bottomNavHeightPx - with(density) { 4.dp.toPx() }) else offsetValue
    val totalDistance = (maxHeightPx - minHeightPx - bottomNavHeightPx - with(density) { 4.dp.toPx() })
    val rawProgress = if (totalDistance == 0f) 0f else (1f - (offset / totalDistance)).coerceIn(0f, 1f)

    // Smooth progress with spring animation for fluid feel
    val progress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 1200f)
    )

    // Animated values for shared element transitions
    val albumArtScale = 1f + (0.8f * progress) // Scales from 1x to 1.8x (48dp -> ~86dp, then NowPlaying takes over at 280dp)
    val albumArtTranslationY = -100f * progress // Moves up as it expands
    val albumArtTranslationX = 0f // Center horizontally
    
    val controlsAlpha = (1f - progress).coerceIn(0f, 1f)
    val nowPlayingAlpha = progress.coerceIn(0f, 1f)
    val backgroundAlpha = (0.3f + 0.6f * progress).coerceIn(0f, 1f)
    val miniPlayerTranslationY = 20f * progress // Slight upward movement

    Box(modifier = Modifier.fillMaxSize()) {
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
                                onBack = {
                                    coroutineScope.launch {
                                        draggableState.animateTo(PlayerDragState.Collapsed)
                                    }
                                }
                            )
                        }
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
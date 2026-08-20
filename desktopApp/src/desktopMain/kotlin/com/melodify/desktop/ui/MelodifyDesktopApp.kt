@file:OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
package com.melodify.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode

import com.melodify.desktop.ui.theme.MelodifyDesktopTheme
import com.melodify.desktop.ui.screens.*
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

import androidx.compose.ui.window.WindowScope
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.hoverable
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.Image

enum class DesktopScreen { HOME, SEARCH, LIBRARY, NOW_PLAYING, PROFILE, SETTINGS, ABOUT, DOWNLOADS }

@Composable
fun WindowScope.MelodifyDesktopApp(onClose: () -> Unit, onMinimize: () -> Unit, onMaximize: () -> Unit) {
    MelodifyDesktopTheme {
        val playerViewModel: PlayerViewModel = koinViewModel()
        val libraryViewModel: com.melodify.shared.presentation.LibraryViewModel = koinViewModel()
        val homeViewModel: com.melodify.shared.presentation.HomeViewModel = koinViewModel()
        val playerState by playerViewModel.playerState.collectAsState()

        var currentScreen by remember { mutableStateOf(DesktopScreen.HOME) }
        var previousScreen by remember { mutableStateOf(DesktopScreen.HOME) }
        val hasTrack = playerState.currentTrack != null

        fun navigateTo(screen: DesktopScreen) {
            if (screen != currentScreen) {
                if (currentScreen != DesktopScreen.NOW_PLAYING) {
                    previousScreen = currentScreen
                }
                currentScreen = screen
            }
        }

        fun toggleNowPlaying() {
            if (currentScreen == DesktopScreen.NOW_PLAYING) {
                currentScreen = if (previousScreen != DesktopScreen.NOW_PLAYING) previousScreen else DesktopScreen.HOME
            } else {
                previousScreen = currentScreen
                currentScreen = DesktopScreen.NOW_PLAYING
            }
        }
        
        val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "aurora")
        val auroraPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(15000, easing = androidx.compose.animation.core.LinearEasing),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "aurora_phase"
        )

        val auroraColor1 = Color(
            red = (0x00 + (0x10 * auroraPhase)).toInt().coerceIn(0, 255),
            green = (0xFF + (-0x44 * auroraPhase)).toInt().coerceIn(0, 255), // Cyan leaning
            blue = (0xAA + (0x55 * auroraPhase)).toInt().coerceIn(0, 255)
        )

        val auroraColor2 = Color(
            red = (0xAA + (0x55 * (1f - auroraPhase))).toInt().coerceIn(0, 255), // Magenta leaning
            green = (0x00 + (0x44 * auroraPhase)).toInt().coerceIn(0, 255),
            blue = (0xFF + (-0x22 * (1f - auroraPhase))).toInt().coerceIn(0, 255)
        )

        val backgroundGradient = Brush.verticalGradient(
            colors = listOf(
                auroraColor1.copy(alpha = 0.2f),
                auroraColor2.copy(alpha = 0.1f),
                Color.Black,
                Color.Black
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black)
                .background(backgroundGradient)
        ) {
            
            // Custom Title Bar
            WindowDraggableArea {
                Row(
                    modifier = Modifier.fillMaxWidth().height(40.dp).background(Color.Black.copy(alpha = 0.3f)),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(16.dp))
                    Text("Melodify", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                    Spacer(Modifier.weight(1f))
                    // Window controls (Windows style: top right)
                    IconButton(onClick = onMinimize, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Remove, "Minimize", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMaximize, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.CropSquare, "Maximize", tint = Color.Gray, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Rounded.Close, "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Main area: sidebar + content
            Row(modifier = Modifier.weight(1f)) {
                DesktopSidebar(
                    currentScreen = currentScreen,
                    onNavigate = ::navigateTo,
                    hasTrack = hasTrack
                )

                // Content area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            if (targetState == DesktopScreen.NOW_PLAYING) {
                                androidx.compose.animation.slideInVertically(
                                    animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.LinearOutSlowInEasing),
                                    initialOffsetY = { it }
                                ) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400))
                            } else if (initialState == DesktopScreen.NOW_PLAYING) {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) togetherWith androidx.compose.animation.slideOutVertically(
                                    animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutLinearInEasing),
                                    targetOffsetY = { it }
                                )
                            } else {
                                androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(200)) togetherWith androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(200))
                            }
                        }
                    ) { screen ->
                        when (screen) {
                            DesktopScreen.HOME -> DesktopHomeScreen(playerViewModel = playerViewModel, libraryViewModel = libraryViewModel, homeViewModel = homeViewModel)
                            DesktopScreen.SEARCH -> DesktopSearchScreen(playerViewModel)
                            DesktopScreen.LIBRARY -> DesktopLibraryScreen(playerViewModel)
                            DesktopScreen.NOW_PLAYING -> DesktopNowPlayingScreen(playerViewModel)
                            DesktopScreen.SETTINGS -> DesktopSettingsScreen()
                            else -> { }
                        }
                    }
                }
            }

            // Persistent bottom player bar
            DesktopPlayerBar(
                playerViewModel = playerViewModel,
                isExpanded = currentScreen == DesktopScreen.NOW_PLAYING,
                onOpenNowPlaying = ::toggleNowPlaying
            )
        }
    }
}

@Composable
fun DesktopSidebar(
    currentScreen: DesktopScreen,
    onNavigate: (DesktopScreen) -> Unit,
    hasTrack: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val sidebarWidth by animateDpAsState(
        targetValue = if (isHovered) 220.dp else 72.dp,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessLow)
    )

    var indicatorY by remember { mutableStateOf(0f) }
    var indicatorHeight by remember { mutableStateOf(0f) }
    
    val animatedIndicatorY by androidx.compose.animation.core.animateFloatAsState(
        targetValue = indicatorY,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
        )
    )
    val animatedIndicatorHeight by androidx.compose.animation.core.animateFloatAsState(
        targetValue = indicatorHeight,
        animationSpec = androidx.compose.animation.core.spring(stiffness = androidx.compose.animation.core.Spring.StiffnessMedium)
    )

    Box(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.5f))
            .hoverable(interactionSource = interactionSource)
    ) {
        if (indicatorHeight > 0f) {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp, horizontal = 12.dp)
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { animatedIndicatorHeight.toDp() })
                    .graphicsLayer { translationY = animatedIndicatorY }
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            )
        }
        
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            // Logo
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp, top = 8.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                val logoBitmap = remember {
                    runCatching {
                        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.jpg")
                        if (stream != null) javax.imageio.ImageIO.read(stream).toComposeImageBitmap() else null
                    }.getOrNull()
                }

                Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
                    if (logoBitmap != null) {
                        Image(
                            bitmap = logoBitmap,
                            contentDescription = "Melodify Logo",
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                androidx.compose.animation.AnimatedVisibility(
                    visible = isHovered,
                    enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, delayMillis = 100)),
                    exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 150))
                ) {
                    Text(
                        "Melodify",
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Default,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 12.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Nav items
            SidebarNavItem(
                icon = Icons.Rounded.Home,
                label = "Home",
                selected = currentScreen == DesktopScreen.HOME,
                isExpanded = isHovered,
                onPositioned = { y, h -> if (currentScreen == DesktopScreen.HOME) { indicatorY = y; indicatorHeight = h } }
            ) { onNavigate(DesktopScreen.HOME) }
            
            Spacer(Modifier.height(8.dp))

            SidebarNavItem(
                icon = Icons.Rounded.Search,
                label = "Search",
                selected = currentScreen == DesktopScreen.SEARCH,
                isExpanded = isHovered,
                onPositioned = { y, h -> if (currentScreen == DesktopScreen.SEARCH) { indicatorY = y; indicatorHeight = h } }
            ) { onNavigate(DesktopScreen.SEARCH) }
            
            Spacer(Modifier.height(8.dp))

            SidebarNavItem(
                icon = Icons.Rounded.LibraryMusic,
                label = "Your Library",
                selected = currentScreen == DesktopScreen.LIBRARY,
                isExpanded = isHovered,
                onPositioned = { y, h -> if (currentScreen == DesktopScreen.LIBRARY) { indicatorY = y; indicatorHeight = h } }
            ) { onNavigate(DesktopScreen.LIBRARY) }

            // Now Playing — only shown when a track is active
            if (hasTrack) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(8.dp))
                SidebarNavItem(
                    icon = Icons.Rounded.Equalizer,
                    label = "Now Playing",
                    selected = currentScreen == DesktopScreen.NOW_PLAYING,
                    isExpanded = isHovered,
                    onPositioned = { y, h -> if (currentScreen == DesktopScreen.NOW_PLAYING) { indicatorY = y; indicatorHeight = h } }
                ) { onNavigate(DesktopScreen.NOW_PLAYING) }
            }

            Spacer(Modifier.weight(1f))

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            SidebarNavItem(
                icon = Icons.Rounded.Settings,
                label = "Settings",
                selected = currentScreen == DesktopScreen.SETTINGS,
                isExpanded = isHovered,
                onPositioned = { y, h -> if (currentScreen == DesktopScreen.SETTINGS) { indicatorY = y; indicatorHeight = h } }
            ) { onNavigate(DesktopScreen.SETTINGS) }
        }
    }
}


@Composable
fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    isExpanded: Boolean,
    onPositioned: (Float, Float) -> Unit,
    onClick: () -> Unit
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selected) 1.2f else 1.0f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                if (selected) {
                    onPositioned(coords.positionInParent().y, coords.size.height.toFloat())
                }
            }
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(modifier = Modifier.width(48.dp), contentAlignment = Alignment.Center) {
            Icon(
                icon, 
                contentDescription = label, 
                tint = contentColor, 
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = isExpanded,
            enter = androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, delayMillis = 100)),
            exit = androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(durationMillis = 100))
        ) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(start = 12.dp),
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

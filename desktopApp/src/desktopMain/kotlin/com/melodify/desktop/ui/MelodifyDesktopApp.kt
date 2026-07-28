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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.melodify.desktop.ui.theme.MelodifyDesktopTheme
import com.melodify.desktop.ui.screens.*
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

enum class DesktopScreen { HOME, SEARCH, LIBRARY, NOW_PLAYING, ABOUT }

@Composable
fun MelodifyDesktopApp() {
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

        Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Main area: sidebar + content
            Row(modifier = Modifier.weight(1f)) {
                DesktopSidebar(
                    currentScreen = currentScreen,
                    onNavigate = ::navigateTo,
                    hasTrack = hasTrack
                )

                // Content area
                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    when (currentScreen) {
                        DesktopScreen.HOME -> DesktopHomeScreen(playerViewModel = playerViewModel, libraryViewModel = libraryViewModel, homeViewModel = homeViewModel)

                        DesktopScreen.SEARCH -> DesktopSearchScreen(playerViewModel)
                        DesktopScreen.LIBRARY -> DesktopLibraryScreen(playerViewModel)
                        DesktopScreen.NOW_PLAYING -> DesktopNowPlayingScreen(playerViewModel)
                        DesktopScreen.ABOUT -> DesktopAboutScreen()
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
    Column(
        modifier = Modifier
            .width(220.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        // Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 32.dp, top = 8.dp)
        ) {
            val logoBitmap = remember {
                runCatching {
                    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")
                    if (stream != null) javax.imageio.ImageIO.read(stream).toComposeImageBitmap() else null
                }.getOrNull()
            }

            if (logoBitmap != null) {
                androidx.compose.foundation.Image(
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
            Spacer(Modifier.width(10.dp))
            Text(
                "Melodify",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Nav items
        SidebarNavItem(
            icon = Icons.Rounded.Home,
            label = "Home",
            selected = currentScreen == DesktopScreen.HOME
        ) { onNavigate(DesktopScreen.HOME) }

        SidebarNavItem(
            icon = Icons.Rounded.Search,
            label = "Search",
            selected = currentScreen == DesktopScreen.SEARCH
        ) { onNavigate(DesktopScreen.SEARCH) }

        SidebarNavItem(
            icon = Icons.Rounded.LibraryMusic,
            label = "Your Library",
            selected = currentScreen == DesktopScreen.LIBRARY
        ) { onNavigate(DesktopScreen.LIBRARY) }

        // Now Playing — only shown when a track is active
        if (hasTrack) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(8.dp))
            SidebarNavItem(
                icon = Icons.Rounded.Equalizer,
                label = "Now Playing",
                selected = currentScreen == DesktopScreen.NOW_PLAYING
            ) { onNavigate(DesktopScreen.NOW_PLAYING) }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(Modifier.height(8.dp))
        SidebarNavItem(
            icon = Icons.Rounded.Info,
            label = "About",
            selected = currentScreen == DesktopScreen.ABOUT
        ) { onNavigate(DesktopScreen.ABOUT) }
    }
}


@Composable
fun SidebarNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
    val contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(bgColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = label, tint = contentColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = contentColor,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}


package com.melodify.android.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.melodify.android.ui.components.MiniPlayer
import com.melodify.android.ui.screens.AboutScreen
import com.melodify.android.ui.screens.HomeScreen
import com.melodify.android.ui.screens.LibraryScreen
import com.melodify.android.ui.screens.NowPlayingScreen
import com.melodify.android.ui.screens.PlaylistDetailScreen
import com.melodify.android.ui.screens.SearchScreen
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Rounded.Home)
    object Search : Screen("search", "Search", Icons.Rounded.Search)
    object Library : Screen("library", "Library", Icons.Rounded.LibraryMusic)
    object NowPlaying : Screen("now_playing", "Now Playing", Icons.Rounded.MusicNote)
    object About : Screen("about", "About", Icons.Rounded.Info)
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Rounded.PlaylistPlay) {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
}

@Composable
fun MelodifyApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = koinViewModel()
    val playerState by playerViewModel.playerState.collectAsState()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isNowPlayingOpen = currentRoute == Screen.NowPlaying.route

    Scaffold(
        bottomBar = {
            if (!isNowPlayingOpen) {
                Column {
                    AnimatedVisibility(
                        visible = playerState.currentTrack != null,
                        enter = slideInVertically { it },
                        exit = slideOutVertically { it }
                    ) {
                        MiniPlayer(playerViewModel, onClick = { navController.navigate(Screen.NowPlaying.route) })
                    }
                    BottomNavBar(navController)
                }
            }
        },
        content = { padding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(if (isNowPlayingOpen) androidx.compose.foundation.layout.PaddingValues(0.dp) else padding)
            ) {
                composable(Screen.Home.route) { HomeScreen(navController) }
                composable(Screen.Search.route) { SearchScreen(navController, playerViewModel) }
                composable(Screen.Library.route) { 
                    LibraryScreen(navController, playerViewModel) 
                }
                composable(
                    route = Screen.NowPlaying.route,
                    enterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                    exitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() },
                    popEnterTransition = { slideInVertically(initialOffsetY = { it }) + fadeIn() },
                    popExitTransition = { slideOutVertically(targetOffsetY = { it }) + fadeOut() }
                ) {
                    NowPlayingScreen(playerViewModel = playerViewModel, onBack = { navController.popBackStack() })
                }
                composable(Screen.About.route) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
                composable(
                    route = Screen.PlaylistDetail.route,
                    arguments = listOf(navArgument("playlistId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
                    PlaylistDetailScreen(
                        playlistId = playlistId,
                        playerViewModel = playerViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    )
}

@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Library)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        items.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.label) },
                label = { Text(screen.label) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

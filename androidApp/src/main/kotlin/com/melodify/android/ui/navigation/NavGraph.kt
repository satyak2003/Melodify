package com.melodify.android.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Download
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.melodify.android.ui.screens.AboutScreen
import com.melodify.android.ui.screens.HomeScreen
import com.melodify.android.ui.screens.LibraryScreen
import com.melodify.android.ui.screens.PlaylistDetailScreen
import com.melodify.android.ui.screens.SearchScreen
import com.melodify.android.ui.screens.ProfileScreen
import com.melodify.shared.domain.model.currentTrack
import com.melodify.shared.presentation.PlayerViewModel
import org.koin.compose.viewmodel.koinViewModel
import com.melodify.android.ui.components.PlayerBottomSheet


sealed class Screen(val route: String, val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon) {
    object Splash : Screen("splash", "Splash", Icons.Rounded.Home)
    object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Rounded.Home)
    object Search : Screen("search", "Search", Icons.Outlined.Search, Icons.Rounded.Search)
    object Library : Screen("library", "Library", Icons.Outlined.LibraryMusic, Icons.Rounded.LibraryMusic)
    object NowPlaying : Screen("now_playing", "Now Playing", Icons.Rounded.MusicNote)
    object Settings : Screen("settings", "Settings", Icons.Rounded.Settings)
    object Profile : Screen("profile", "Profile", Icons.Rounded.Person)
    object Downloads : Screen("downloads", "Downloads", Icons.Rounded.Download)
    object About : Screen("about", "About", Icons.Rounded.Info)
    object Equalizer : Screen("equalizer", "Equalizer", Icons.Rounded.PlayCircle)
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist", Icons.Rounded.PlaylistPlay) {
        fun createRoute(playlistId: String) = "playlist_detail/$playlistId"
    }
}

fun getScreenOrder(route: String?): Int = when (route) {
    Screen.Home.route -> 0
    Screen.Search.route -> 1
    Screen.Library.route -> 2
    else -> 0
}

@OptIn(ExperimentalMaterial3Api::class, org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
fun MelodifyApp() {
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = koinViewModel()
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isSettingsOrAbout = currentRoute == Screen.Settings.route || currentRoute == Screen.About.route || currentRoute == Screen.Profile.route || currentRoute == Screen.Splash.route || currentRoute == Screen.Equalizer.route

    val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // 72dp nav bar pill + 16dp bottom gap + system nav bar inset
    val bottomNavHeight = 72.dp + 16.dp + navBarBottomPadding

    PlayerBottomSheet(
        playerViewModel = playerViewModel, 
        bottomNavHeight = if(isSettingsOrAbout) 0.dp else bottomNavHeight,
        showPlayer = !isSettingsOrAbout,
        bottomBar = {
            if (!isSettingsOrAbout) {
                BottomNavBar(navController)
            }
        }
    ) {
        Scaffold(
            content = { padding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route,
                        modifier = Modifier.padding(if (currentRoute == Screen.Splash.route) androidx.compose.foundation.layout.PaddingValues(0.dp) else padding)
                    ) {
                        composable(Screen.Splash.route) {
                            com.melodify.android.ui.screens.SplashScreen(navController = navController)
                        }

                        composable(
                            Screen.Home.route,
                            enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) + fadeIn() },
                            exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut() }
                        ) { HomeScreen(navController, playerViewModel) }

                        composable(
                            Screen.Search.route,
                            enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
                            exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) + fadeOut() }
                        ) { SearchScreen(navController, playerViewModel) }

                        composable(
                            Screen.Library.route,
                            enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn() },
                            exitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut() }
                        ) { LibraryScreen(navController, playerViewModel) }
                        composable(Screen.About.route) {
                            AboutScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Screen.Equalizer.route) {
                            com.melodify.android.ui.screens.EqualizerScreen(
                                viewModel = playerViewModel,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable(
                            Screen.Settings.route,
                            enterTransition = { androidx.compose.animation.slideInVertically(initialOffsetY = { it }, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)) },
                            exitTransition = { androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(400)) },
                            popExitTransition = { androidx.compose.animation.slideOutVertically(targetOffsetY = { it }, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400)) }
                        ) {
                            com.melodify.android.ui.screens.SettingsScreen(onBack = { navController.popBackStack() }, navController = navController)
                        }
                        composable(Screen.Profile.route) {
                            ProfileScreen(onBack = { navController.popBackStack() }, navController = navController)
                        }
                        composable(Screen.Downloads.route) {
                            com.melodify.android.ui.screens.DownloadsScreen(navController = navController, onBack = { navController.popBackStack() })
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
}
@Composable
fun BottomNavBar(navController: NavController) {
    val items = listOf(Screen.Home, Screen.Search, Screen.Library)
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTab = when {
        currentRoute == Screen.Home.route -> Screen.Home
        currentRoute == Screen.Search.route -> Screen.Search
        currentRoute == Screen.Library.route || currentRoute?.startsWith("playlist_detail/") == true || currentRoute == Screen.Downloads.route -> Screen.Library
        else -> null
    }

    val navBarBottomPadding = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    androidx.compose.material3.Surface(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottomPadding),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = androidx.compose.ui.graphics.Color(0xAA000000),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, androidx.compose.ui.graphics.Color(0x44FFFFFF)),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        NavigationBar(
            modifier = Modifier.height(72.dp),
            containerColor = androidx.compose.ui.graphics.Color.Transparent, // Let the surface show through
            contentColor = androidx.compose.ui.graphics.Color.White,
            tonalElevation = 0.dp,
            windowInsets = WindowInsets(0)
        ) {
        items.forEach { screen ->
            val isSelected = currentTab?.route == screen.route
            
            // Animation for bounce
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1f,
                animationSpec = androidx.compose.animation.core.spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                )
            )

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { 
                    androidx.compose.animation.AnimatedContent(
                        targetState = isSelected,
                        transitionSpec = {
                            androidx.compose.animation.scaleIn(animationSpec = androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )) + androidx.compose.animation.fadeIn() togetherWith androidx.compose.animation.scaleOut() + androidx.compose.animation.fadeOut()
                        },
                        label = "icon_animation"
                    ) { selected ->
                        Icon(
                            if (selected) screen.selectedIcon else screen.icon, 
                            contentDescription = screen.label,
                            modifier = Modifier.scale(scale)
                        ) 
                    }
                },
                label = { Text(screen.label) },
                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                    selectedIconColor = androidx.compose.ui.graphics.Color.White,
                    unselectedIconColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                    selectedTextColor = androidx.compose.ui.graphics.Color.White,
                    unselectedTextColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
                    indicatorColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.1f)
                )
            )
        }
    }
}
}
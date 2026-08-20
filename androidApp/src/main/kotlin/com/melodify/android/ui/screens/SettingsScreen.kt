package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.melodify.shared.data.storage.ExperimentalSettingsStorage
import com.melodify.shared.data.storage.YouTubeAuthManager
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.data.storage.SupabaseApi
import com.melodify.shared.domain.sync.SyncSessionManager
import com.melodify.shared.presentation.LibraryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import android.net.Uri
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.HelpOutline

import androidx.navigation.NavController
import com.melodify.shared.ui.modifiers.bounceClick
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun SettingsScreen(onBack: () -> Unit, navController: NavController) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val isMlTaggingEnabled by ExperimentalSettingsStorage.isMlTaggingEnabled.collectAsState()
    val currentSyncSession by SyncSessionManager.currentSession.collectAsState()
    val isSyncHost by SyncSessionManager.isHost.collectAsState()

    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val uiState by libraryViewModel.uiState.collectAsState()
    val importProgress by libraryViewModel.importProgress.collectAsState()

    var importUrl by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }
    var showSpotifyImportDialog by remember { mutableStateOf(false) }
    
    var currentSettingsPage by remember { mutableStateOf("MAIN") }

    val context = LocalContext.current
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val csvContent = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
            if (csvContent.isNotBlank()) {
                libraryViewModel.importFromCsv(csvContent)
            }
        }
    }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = MaterialTheme.colorScheme.onSurface,
        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        focusedLabelColor = MaterialTheme.colorScheme.primary,
        unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        focusedBorderColor = MaterialTheme.colorScheme.primary,
        unfocusedBorderColor = MaterialTheme.colorScheme.outline
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        BackHandler {
            if (currentSettingsPage != "MAIN") {
                currentSettingsPage = "MAIN"
            } else {
                onBack()
            }
        }
        
        val pageTitle = when (currentSettingsPage) {
            "MAIN" -> "Settings & Integrations"
            "ACCOUNTS" -> "Accounts & Connections"
            "IMPORT" -> "Music Import"
            "PLAYBACK" -> "Playback"
            "SOCIAL" -> "Social"
            "ABOUT" -> "About"
            else -> "Settings"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .bounceClick(scaleDown = 0.85f, onClick = {
                        if (currentSettingsPage != "MAIN") {
                            currentSettingsPage = "MAIN"
                        } else {
                            onBack()
                        }
                    }),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                pageTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(24.dp))

        when (currentSettingsPage) {
            "MAIN" -> {
                SettingsMenuItem("Accounts & Connections", Icons.Rounded.Link) { currentSettingsPage = "ACCOUNTS" }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuItem("Music Import", Icons.Rounded.LibraryMusic) { currentSettingsPage = "IMPORT" }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuItem("Playback", Icons.Rounded.PlayCircle) { currentSettingsPage = "PLAYBACK" }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuItem("Social", Icons.Rounded.Group) { currentSettingsPage = "SOCIAL" }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                SettingsMenuItem("About", Icons.Rounded.Info) { currentSettingsPage = "ABOUT" }
            }
            "ACCOUNTS" -> {
                // Melodify Account Card
                val context = androidx.compose.ui.platform.LocalContext.current
                val prefs = context.getSharedPreferences("MelodifyAuth", android.content.Context.MODE_PRIVATE)
                val currentUser by com.melodify.shared.data.storage.SupabaseAuthManager.currentUser.collectAsState()
                val isMelodifyLoggedIn = currentUser != null
                Card(
                    modifier = Modifier.fillMaxWidth().bounceClick {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://melodify-backend-2469.onrender.com/login"))
                        navController.context.startActivity(intent)
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Melodify Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(Modifier.height(4.dp))
                        if (isMelodifyLoggedIn) {
                            Text(
                                "Logged in as ${currentUser?.email}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    prefs.edit().remove("SUPABASE_SESSION_TOKEN").apply()
                                    com.melodify.shared.data.storage.SupabaseAuthManager.logout()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Log Out", color = MaterialTheme.colorScheme.onError)
                            }
                        } else {
                            Text(
                                "Log in or sign up to sync your library, settings, and profile across devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://melodify-backend-2469.onrender.com/login"))
                                    navController.context.startActivity(intent)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Login / Sign Up")
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // YouTube Account Card
                val isGoogleLoggedIn by AuthManager.isGoogleLoggedIn.collectAsState()
                val googleUserName by AuthManager.userName.collectAsState()
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("YouTube Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(4.dp))
                        if (isGoogleLoggedIn) {
                            Text(
                                "Logged in as ${googleUserName ?: "User"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = { coroutineScope.launch { AuthManager.logout() } },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Log Out")
                            }
                        } else {
                            Text(
                                "Not connected. Sync from the Library tab to log in.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(16.dp))

                // Jellyfin Settings Card
                val jellyfinUrl by com.melodify.shared.data.storage.JellyfinSettings.serverUrl.collectAsState()
                val jellyfinUser by com.melodify.shared.data.storage.JellyfinSettings.username.collectAsState()
                val jellyfinPass by com.melodify.shared.data.storage.JellyfinSettings.password.collectAsState()
                var tempJfUrl by remember(jellyfinUrl) { mutableStateOf(jellyfinUrl) }
                var tempJfUser by remember(jellyfinUser) { mutableStateOf(jellyfinUser) }
                var tempJfPass by remember(jellyfinPass) { mutableStateOf(jellyfinPass) }
                var passwordVisible by remember { mutableStateOf(false) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Jellyfin (FLAC Source)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Connect your Jellyfin server for high-fidelity FLAC streaming and downloads.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempJfUrl,
                            onValueChange = { tempJfUrl = it },
                            label = { Text("Server URL") },
                            placeholder = { Text("http://192.168.1.10:8096") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = fieldColors
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempJfUser,
                            onValueChange = { tempJfUser = it },
                            label = { Text("Jellyfin Username") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = fieldColors
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tempJfPass,
                            onValueChange = { tempJfPass = it },
                            label = { Text("Jellyfin Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = fieldColors,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff
                                val description = if (passwordVisible) "Hide password" else "Show password"
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, description)
                                }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        var isAuthenticating by remember { mutableStateOf(false) }
                        var authError by remember { mutableStateOf<String?>(null) }
                        
                        if (authError != null) {
                            Text(authError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                isAuthenticating = true
                                authError = null
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        val client = com.melodify.shared.data.network.jellyfin.JellyfinClient(io.ktor.client.HttpClient())
                                        val result = client.authenticate(tempJfUrl, tempJfUser, tempJfPass)
                                        com.melodify.shared.data.storage.JellyfinSettings.saveSettings(
                                            url = tempJfUrl,
                                            user = tempJfUser,
                                            pass = tempJfPass,
                                            token = result.token,
                                            id = result.userId
                                        )
                                        // TODO: Reload Jellyfin Library
                                    } catch (e: Exception) {
                                        authError = "Authentication failed: ${e.message}"
                                    } finally {
                                        isAuthenticating = false
                                    }
                                }
                            },
                            enabled = !isAuthenticating && tempJfUrl.isNotBlank() && tempJfUser.isNotBlank() && tempJfPass.isNotBlank(),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            if (isAuthenticating) {
                                androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                            }
                            Text("Save Jellyfin Credentials")
                        }
                    }
                }
            }
            "IMPORT" -> {
                // Import Playlist Card
                if (showSpotifyImportDialog) {
                    AlertDialog(
                        onDismissRequest = { showSpotifyImportDialog = false },
                        title = { Text("How to Import from Spotify") },
                        text = {
                            Column {
                                Text("Spotify playlists larger than 100 tracks cannot be imported directly via links. Instead, use TuneMyMusic to bypass the limit:")
                                Spacer(Modifier.height(8.dp))
                                Text("1. Go to https://www.tunemymusic.com")
                                Text("2. Select Spotify as the source and select your playlist.")
                                Text("3. Choose 'Export to file' as the destination.")
                                Text("4. Select 'CSV' and download the file.")
                                Text("5. Come back here and tap 'Import from CSV File'.")
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showSpotifyImportDialog = false }) {
                                Text("Got it")
                            }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text("Import Playlist (YT Music & Spotify)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { showSpotifyImportDialog = true }) {
                                Icon(Icons.Rounded.HelpOutline, contentDescription = "How to import", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = importUrl,
                            onValueChange = { importUrl = it },
                            label = { Text("Paste YouTube Music or Spotify Playlist Link") },
                            placeholder = { Text("https://music.youtube.com/playlist?list=...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = fieldColors
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { csvLauncher.launch("text/*") }) {
                                Icon(Icons.Rounded.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Import from CSV File")
                            }
                            Button(
                                onClick = {
                                    if (importUrl.isNotBlank()) {
                                        libraryViewModel.importPlaylistFromLink(importUrl)
                                        importUrl = ""
                                    }
                                },
                                enabled = importUrl.isNotBlank() && importProgress == null
                            ) {
                                Text("Import Link")
                            }
                        }
                        
                        importProgress?.let { progress ->
                            Spacer(Modifier.height(16.dp))
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "Importing: ${progress.currentTrack}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(4.dp))
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { progress.percentage },
                                    modifier = Modifier.fillMaxWidth().height(4.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "${progress.imported} / ${progress.total} tracks",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
            "PLAYBACK" -> {
                // Autoplay Recommendations Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text("Autoplay Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "When the queue ends, automatically play recommended songs based on your listening habits",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val isAutoPlay by ExperimentalSettingsStorage.isAutoPlayEnabled.collectAsState()
                        Switch(
                            checked = isAutoPlay,
                            onCheckedChange = { ExperimentalSettingsStorage.setAutoPlayEnabled(it) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Equalizer Card
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { navController.navigate("equalizer") },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.padding(horizontal = 4.dp))
                                Text("Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Adjust audio frequencies and select presets",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Open Equalizer", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            "SOCIAL" -> {
                // Sync Listening & Discord Party Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("Sync Listening & Discord Party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(8.dp))
                        if (currentSyncSession != null) {
                            Text(
                                "Active Room Code: ${currentSyncSession?.sessionCode} (${if (isSyncHost) "Host" else "Listener"})",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { SyncSessionManager.leaveSession() }) {
                                Text("Leave Room")
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { SyncSessionManager.createSession("AndroidUser", null) }) {
                                    Text("Host Room")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                OutlinedTextField(
                                    value = joinCodeInput,
                                    onValueChange = { joinCodeInput = it },
                                    label = { Text("Room Code") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    colors = fieldColors
                                )
                                Button(
                                    onClick = {
                                        if (joinCodeInput.isNotBlank()) {
                                            SyncSessionManager.joinSession(joinCodeInput, "AndroidListener")
                                            joinCodeInput = ""
                                        }
                                    },
                                    enabled = joinCodeInput.isNotBlank()
                                ) {
                                    Text("Join")
                                }
                            }
                        }
                    }
                }
            }
            "ABOUT" -> {
                // About & Information Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text("About & Support", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { navController.navigate("about") }, modifier = Modifier.fillMaxWidth()) {
                            Text("View About Melodify")
                        }
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp)) // Extra padding at the bottom
    }
}

@Composable
fun SettingsMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
    }
}

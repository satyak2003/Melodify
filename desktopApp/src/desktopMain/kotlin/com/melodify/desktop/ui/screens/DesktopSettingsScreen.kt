package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.melodify.shared.data.storage.ExperimentalSettingsStorage
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.data.storage.JellyfinSettings
import com.melodify.shared.domain.sync.SyncSessionManager
import com.melodify.shared.presentation.LibraryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun DesktopSettingsScreen() {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    var selectedCategory by remember { mutableStateOf("IMPORT") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp)
    ) {
        // LEFT PANE: Categories Menu
        Column(
            modifier = Modifier
                .weight(0.3f)
                .fillMaxHeight()
                .padding(end = 24.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(Modifier.height(32.dp))

            DesktopSettingsMenuCategory("IMPORT", "Music Import", Icons.Rounded.LibraryMusic, selectedCategory) { selectedCategory = it }
            DesktopSettingsMenuCategory("ACCOUNTS", "External Audio", Icons.Rounded.Link, selectedCategory) { selectedCategory = it }
            DesktopSettingsMenuCategory("PLAYBACK", "Playback", Icons.Rounded.PlayCircle, selectedCategory) { selectedCategory = it }
            DesktopSettingsMenuCategory("SOCIAL", "Social & Party", Icons.Rounded.Group, selectedCategory) { selectedCategory = it }
        }

        // RIGHT PANE: Content Area
        Card(
            modifier = Modifier.weight(0.7f).fillMaxHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(32.dp)
            ) {
                when (selectedCategory) {
                    "IMPORT" -> DesktopImportSettingsSection(libraryViewModel)
                    "ACCOUNTS" -> DesktopExternalAudioSettingsSection()
                    "PLAYBACK" -> DesktopPlaybackSettingsSection()
                    "SOCIAL" -> DesktopSocialSettingsSection()
                }
            }
        }
    }
}

@Composable
fun DesktopSettingsMenuCategory(
    id: String,
    title: String,
    icon: ImageVector,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    val isSelected = id == selectedId
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent
    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onBackground

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(id) }
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = if (isSelected) MaterialTheme.colorScheme.primary else contentColor)
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = contentColor)
        }
    }
}

@Composable
fun DesktopImportSettingsSection(libraryViewModel: LibraryViewModel) {
    var importUrl by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    val isGoogleLoggedIn by AuthManager.isGoogleLoggedIn.collectAsState()

    Text("Music Import", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(24.dp))

    // Import Playlist
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Import Playlist Link", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = importUrl,
                onValueChange = { importUrl = it },
                label = { Text("YouTube Music or Spotify URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { if (importUrl.isNotBlank()) { libraryViewModel.importPlaylistFromLink(importUrl); importUrl = "" } },
                enabled = importUrl.isNotBlank(),
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Start Import")
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // Sync YouTube Music
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Sync YouTube Library", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Sign in with Google to automatically pull in your YouTube Music library and playlists.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        val tokens = AuthManager.loginWithGoogle()
                        libraryViewModel.importYouTubePlaylists(tokens?.accessToken)
                    }
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(Icons.Rounded.VideoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (isGoogleLoggedIn) "Resync YouTube Music" else "Sign In & Sync")
            }
        }
    }
}

@Composable
fun DesktopExternalAudioSettingsSection() {
    val coroutineScope = rememberCoroutineScope()
    
    // Jellyfin
    val jellyfinUrl by JellyfinSettings.serverUrl.collectAsState()
    val jellyfinUser by JellyfinSettings.username.collectAsState()
    val jellyfinPass by JellyfinSettings.password.collectAsState()
    var tempJfUrl by remember(jellyfinUrl) { mutableStateOf(jellyfinUrl) }
    var tempJfUser by remember(jellyfinUser) { mutableStateOf(jellyfinUser) }
    var tempJfPass by remember(jellyfinPass) { mutableStateOf(jellyfinPass) }
    var passwordVisible by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Text("External Audio Sources", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(24.dp))

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Jellyfin Server (FLAC Streaming)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = tempJfUrl, onValueChange = { tempJfUrl = it }, label = { Text("Server URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = tempJfUser, onValueChange = { tempJfUser = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = tempJfPass, onValueChange = { tempJfPass = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(if (passwordVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, null)
                    }
                }
            )
            if (authError != null) {
                Spacer(Modifier.height(8.dp))
                Text(authError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    isAuthenticating = true
                    authError = null
                    coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val client = com.melodify.shared.data.network.jellyfin.JellyfinClient(io.ktor.client.HttpClient())
                            val result = client.authenticate(tempJfUrl, tempJfUser, tempJfPass)
                            JellyfinSettings.saveSettings(tempJfUrl, tempJfUser, tempJfPass, result.token, result.userId)
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
                if (isAuthenticating) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Connect Jellyfin")
            }
        }
    }
}

@Composable
fun DesktopPlaybackSettingsSection() {
    val isAutoPlay by ExperimentalSettingsStorage.isAutoPlayEnabled.collectAsState()

    Text("Playback Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(24.dp))

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(24.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text("Autoplay Recommendations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Automatically play similar tracks when your queue ends.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(
                checked = isAutoPlay,
                onCheckedChange = { ExperimentalSettingsStorage.setAutoPlayEnabled(it) }
            )
        }
    }
}

@Composable
fun DesktopSocialSettingsSection() {
    val currentSyncSession by SyncSessionManager.currentSession.collectAsState()
    val isSyncHost by SyncSessionManager.isHost.collectAsState()
    var joinCodeInput by remember { mutableStateOf("") }

    Text("Social & Party", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(24.dp))

    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Sync Listening Room", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Listen along with friends in real-time.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            if (currentSyncSession != null) {
                Text(
                    "Active Room Code: ${currentSyncSession?.sessionCode} (${if (isSyncHost) "Host" else "Listener"})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = { SyncSessionManager.leaveSession() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("Leave Room")
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { SyncSessionManager.createSession("DesktopHost", null) }, modifier = Modifier.weight(1f)) {
                        Text("Host New Room")
                    }
                    Text("OR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = joinCodeInput,
                        onValueChange = { joinCodeInput = it },
                        label = { Text("Room Code") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = { if (joinCodeInput.isNotBlank()) { SyncSessionManager.joinSession(joinCodeInput, "DesktopListener"); joinCodeInput = "" } },
                        enabled = joinCodeInput.isNotBlank()
                    ) {
                        Text("Join")
                    }
                }
            }
        }
    }
}

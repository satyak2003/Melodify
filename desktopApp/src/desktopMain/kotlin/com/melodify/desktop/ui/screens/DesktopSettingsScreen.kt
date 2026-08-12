package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melodify.shared.data.storage.ExperimentalSettingsStorage
import com.melodify.shared.data.storage.YouTubeAuthManager
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.domain.sync.SyncSessionManager
import com.melodify.shared.presentation.LibraryViewModel
import com.melodify.shared.data.storage.SupabaseApi
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DesktopSettingsScreen() {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val isMlTaggingEnabled by ExperimentalSettingsStorage.isMlTaggingEnabled.collectAsState()
    val currentSyncSession by SyncSessionManager.currentSession.collectAsState()
    val isSyncHost by SyncSessionManager.isHost.collectAsState()
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

    var importUrl by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }

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
            .padding(horizontal = 32.dp, vertical = 24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(horizontal = 4.dp))
            Text(
                "Settings & Integrations",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(24.dp))



        Spacer(Modifier.height(20.dp))

        // Import Playlist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Import Playlist (YouTube Music & Spotify)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text("Paste YouTube Music or Spotify Playlist Link") },
                    placeholder = { Text("https://music.youtube.com/playlist?list=... or https://open.spotify.com/playlist/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (importUrl.isNotBlank()) {
                            libraryViewModel.importPlaylistFromLink(importUrl)
                            importUrl = ""
                        }
                    },
                    enabled = importUrl.isNotBlank(),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Import Playlist")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Sync YouTube Music Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Sync YouTube Music Playlists", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Sign in with Google to import your YouTube Music playlists and library.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
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
                    Text("Sign in & Sync YouTube Music")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Sync Listening & Discord Party Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Sync Listening & Discord Party Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { SyncSessionManager.createSession("DesktopHost", null) }) {
                            Text("Host Sync Room")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
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
                                    SyncSessionManager.joinSession(joinCodeInput, "DesktopListener")
                                    joinCodeInput = ""
                                }
                            },
                            enabled = joinCodeInput.isNotBlank()
                        ) {
                            Text("Join Room")
                        }
                    }
                }
            }
        }


    }
}

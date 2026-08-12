package com.melodify.android.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.AutoAwesome
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
import com.melodify.shared.data.storage.SupabaseApi
import com.melodify.shared.domain.sync.SyncSessionManager
import com.melodify.shared.presentation.LibraryViewModel
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

import androidx.navigation.NavController
import com.melodify.shared.ui.modifiers.bounceClick
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.activity.compose.BackHandler

@Composable
fun SettingsScreen(onBack: () -> Unit, navController: NavController) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val isMlTaggingEnabled by ExperimentalSettingsStorage.isMlTaggingEnabled.collectAsState()
    val currentSyncSession by SyncSessionManager.currentSession.collectAsState()
    val isSyncHost by SyncSessionManager.isHost.collectAsState()
    val isSpotifyConnected by libraryViewModel.isSpotifyConnected.collectAsState()
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
            .padding(16.dp)
    ) {
        BackHandler(onBack = onBack)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .bounceClick(scaleDown = 0.85f, onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Settings & Integrations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(16.dp))

        // Spotify Account Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isSpotifyConnected) Icons.Rounded.CheckCircle else Icons.Rounded.AccountCircle,
                        contentDescription = null,
                        tint = if (isSpotifyConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(
                        "Spotify Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (isSpotifyConnected) {
                    Text(
                        "Connected to Spotify",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { libraryViewModel.disconnectSpotify() },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                        Text("Disconnect Spotify")
                    }
                } else {
                    Text(
                        "Not connected. Connect your account to import your Spotify playlists and liked songs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                    Button(
                        onClick = { 
                            val authUrl = libraryViewModel.startSpotifyLogin(com.melodify.shared.api.spotify.SpotifyAuthHelper.ANDROID_REDIRECT_URI)
                            uriHandler.openUri(authUrl)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect Spotify")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Import Playlist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Import Playlist (YT Music & Spotify)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
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

        Spacer(Modifier.height(16.dp))

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

        Spacer(Modifier.height(16.dp))

        // ML Audio Fingerprinting removed per user request
        
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

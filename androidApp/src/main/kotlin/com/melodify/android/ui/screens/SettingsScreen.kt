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
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.melodify.shared.domain.sync.SyncSessionManager
import com.melodify.shared.presentation.LibraryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val isMlTaggingEnabled by ExperimentalSettingsStorage.isMlTaggingEnabled.collectAsState()
    val isSyncEnabled by ExperimentalSettingsStorage.isSyncListeningEnabled.collectAsState()
    val currentSyncSession by SyncSessionManager.currentSession.collectAsState()
    val isSyncHost by SyncSessionManager.isHost.collectAsState()

    var importUrl by remember { mutableStateOf("") }
    var joinCodeInput by remember { mutableStateOf("") }
    var accountStatusMessage by remember { mutableStateOf("Connected as Guest User") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text(
                "Settings & Integrations",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(16.dp))

        // Account & Login Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Account & Streaming Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Text(accountStatusMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { accountStatusMessage = "Logged into YouTube Music" }) {
                        Text("Connect YT Music")
                    }
                    TextButton(onClick = { accountStatusMessage = "Logged Out (Guest Mode)" }) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 2.dp))
                        Text("Logout")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Import Playlist Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Import Playlist (YT Music & Spotify)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = importUrl,
                    onValueChange = { importUrl = it },
                    label = { Text("Paste YouTube Music or Spotify Playlist Link") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
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

        // Sync Listening & Discord Party Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Group, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Sync Listening & Discord Party", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                if (currentSyncSession != null) {
                    Text(
                        "Active Session Code: ${currentSyncSession?.sessionCode} (${if (isSyncHost) "Host" else "Listener"})",
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
                        Button(onClick = { SyncSessionManager.createSession("HostUser", null) }) {
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
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (joinCodeInput.isNotBlank()) {
                                    SyncSessionManager.joinSession(joinCodeInput, "Listener")
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

        // Experimental Features Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text("Experimental Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("ML Audio Fingerprinting & Waveform Tagging", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                        Text("Analyze frequency spectrum & calculate estimated BPM", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(
                        checked = isMlTaggingEnabled,
                        onCheckedChange = { ExperimentalSettingsStorage.setMlTaggingEnabled(it) }
                    )
                }
            }
        }
    }
}

package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.data.storage.YouTubeAuthManager
import com.melodify.shared.data.storage.SupabaseApi
import com.melodify.shared.data.storage.SupabaseAuthManager
import kotlinx.coroutines.launch
import com.melodify.shared.presentation.LibraryViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun DesktopProfileScreen() {
    val coroutineScope = rememberCoroutineScope()
    val libraryViewModel: LibraryViewModel = koinViewModel()
    
    // Auth States
    val currentUser by SupabaseAuthManager.currentUser.collectAsState()
    val isMelodifyLoggedIn = currentUser != null

    val isGoogleLoggedIn by AuthManager.isGoogleLoggedIn.collectAsState()
    val isYtLoggedIn by YouTubeAuthManager.isLoggedIn.collectAsState()
    val ytAccountName by YouTubeAuthManager.userAccountName.collectAsState()
    val uriHandler = LocalUriHandler.current

    var showCookieDialog by remember { mutableStateOf(false) }
    var cookieInput by remember { mutableStateOf("") }

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
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Cloud Profile",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(32.dp))

        // --- MELODIFY ACCOUNT CARD ---
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f))
        ) {
            Column(modifier = Modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.AccountCircle, 
                    contentDescription = null, 
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(Modifier.height(16.dp))
                
                if (isMelodifyLoggedIn) {
                    Text("Welcome Back", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.height(8.dp))
                    Text(currentUser?.email ?: "User", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { SupabaseAuthManager.logout() }, 
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out")
                    }
                } else {
                    Text("Sync Across Devices", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(8.dp))
                    Text("Sign in to Melodify to backup your library, playlists, and settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { 
                        uriHandler.openUri("https://melodify-backend-2469.onrender.com/login")
                    }) {
                        Text("Log In or Sign Up")
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // --- CONNECTED SERVICES SECTION ---
        Text(
            "Connected Services",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Spacer(Modifier.height(16.dp))

        // YouTube Music Link Card
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("YouTube Music", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (isYtLoggedIn) "Connected as $ytAccountName" else "Not Connected",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isYtLoggedIn) {
                        OutlinedButton(
                            onClick = { YouTubeAuthManager.logout() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(onClick = { showCookieDialog = !showCookieDialog }) {
                            Text("Connect Manually")
                        }
                    }
                }

                if (showCookieDialog && !isYtLoggedIn) {
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        label = { Text("Paste SAPISID Cookie Header") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (cookieInput.isNotBlank()) {
                                YouTubeAuthManager.loginWithCookies(cookieInput)
                                cookieInput = ""
                                showCookieDialog = false
                            }
                        }, 
                        enabled = cookieInput.isNotBlank(),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Save & Connect")
                    }
                }
            }
        }
    }
}

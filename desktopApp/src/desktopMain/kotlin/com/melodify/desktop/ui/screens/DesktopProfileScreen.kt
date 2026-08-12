package com.melodify.desktop.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.data.storage.YouTubeAuthManager
import com.melodify.shared.data.storage.SupabaseApi
import kotlinx.coroutines.launch
import com.melodify.shared.presentation.LibraryViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.ui.platform.LocalUriHandler

@Composable
fun DesktopProfileScreen() {
    val isGoogleLoggedIn by AuthManager.isGoogleLoggedIn.collectAsState()
    val isYtLoggedIn by YouTubeAuthManager.isLoggedIn.collectAsState()
    val ytAccountName by YouTubeAuthManager.userAccountName.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val libraryViewModel: LibraryViewModel = koinViewModel()
    val isSpotifyConnected by libraryViewModel.isSpotifyConnected.collectAsState()
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
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Profile & Accounts",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(24.dp))

        // Profile / Sign In Card
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(if (isGoogleLoggedIn) "Welcome Back" else "Log In", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(16.dp))
                
                if (isGoogleLoggedIn) {
                    Text("Status: Signed in with Google", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { coroutineScope.launch { AuthManager.logout() } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Logout")
                    }
                } else {

                    OutlinedButton(onClick = {
                        coroutineScope.launch {
                            val tokens = AuthManager.loginWithGoogle()
                            if (tokens?.idToken?.isNotBlank() == true) {
                                SupabaseApi.signInWithGoogleIdToken(tokens.idToken)
                            }
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log In with Google")
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // YouTube Music Link Card
        Card(
            modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Streaming Services", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(16.dp))
                
                Text(
                    if (isYtLoggedIn) "YouTube Music: Connected ($ytAccountName)" else "YouTube Music: Not Connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (!isYtLoggedIn) {
                    TextButton(onClick = { showCookieDialog = !showCookieDialog }) {
                        Text("Manually Connect YT Account (Advanced)")
                    }
                } else {
                    TextButton(onClick = { YouTubeAuthManager.logout() }) {
                        Text("Disconnect YT Music", color = MaterialTheme.colorScheme.error)
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                
                Text(
                    if (isSpotifyConnected) "Spotify: Connected" else "Spotify: Not Connected",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (!isSpotifyConnected) {
                    OutlinedButton(onClick = {
                        val authUrl = libraryViewModel.startSpotifyLogin()
                        uriHandler.openUri(authUrl)
                    }) {
                        Text("Connect Spotify")
                    }
                } else {
                    TextButton(onClick = { libraryViewModel.logoutSpotify() }) {
                        Text("Disconnect Spotify", color = MaterialTheme.colorScheme.error)
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
                    Button(onClick = {
                        if (cookieInput.isNotBlank()) {
                            YouTubeAuthManager.loginWithCookies(cookieInput)
                            cookieInput = ""
                            showCookieDialog = false
                        }
                    }, enabled = cookieInput.isNotBlank()) {
                        Text("Save & Log In")
                    }
                }
            }
        }
    }
}

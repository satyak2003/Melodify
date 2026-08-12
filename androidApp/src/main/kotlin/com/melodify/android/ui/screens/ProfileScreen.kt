package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodify.shared.data.storage.AuthManager
import com.melodify.shared.data.storage.YouTubeAuthManager
import com.melodify.shared.data.storage.SupabaseApi
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler

@Composable
fun ProfileScreen(onBack: () -> Unit, navController: NavController) {
    BackHandler(onBack = onBack)
    val isGoogleLoggedIn by AuthManager.isGoogleLoggedIn.collectAsState()
    val isYtLoggedIn by YouTubeAuthManager.isLoggedIn.collectAsState()
    val ytAccountName by YouTubeAuthManager.userAccountName.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
            }
            Text(
                "Profile",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(16.dp))

        // Profile / Sign In Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    Text(if (isGoogleLoggedIn) "Welcome Back" else "Log In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(Modifier.height(12.dp))
                
                if (isGoogleLoggedIn) {
                    Text("Status: Signed in with Google", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { coroutineScope.launch { AuthManager.logout() } }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.padding(horizontal = 4.dp))
                        Text("Logout")
                    }
                } else {
                    // Email/Password Login
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { /* TODO: Implement Firebase Email Login */ }, modifier = Modifier.fillMaxWidth()) {
                        Text("Log In with Email")
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("OR", modifier = Modifier.align(Alignment.CenterHorizontally), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(16.dp))

        // YouTube Music Link Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Streaming Services", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                
                Text(
                    if (isYtLoggedIn) "YouTube Music: Connected ($ytAccountName)" else "YouTube Music: Not Connected",
                    style = MaterialTheme.typography.bodyMedium,
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

                if (showCookieDialog && !isYtLoggedIn) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cookieInput,
                        onValueChange = { cookieInput = it },
                        label = { Text("Paste SAPISID Cookie Header") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors
                    )
                    Spacer(Modifier.height(8.dp))
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

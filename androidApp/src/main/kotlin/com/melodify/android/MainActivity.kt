package com.melodify.android

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.melodify.android.ui.navigation.MelodifyApp
import com.melodify.android.ui.theme.MelodifyTheme
import com.melodify.shared.navigation.DeepLinkHandler

import com.melodify.shared.presentation.LibraryViewModel
import org.koin.android.ext.android.inject

import android.content.pm.PackageManager
import android.os.Build

import androidx.activity.result.contract.ActivityResultContracts
import com.melodify.shared.data.storage.AuthManager
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import android.content.ComponentName
import com.google.common.util.concurrent.ListenableFuture
import com.melodify.android.service.PlayerService

class MainActivity : ComponentActivity() {
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            AuthManager.handleSignInResult(result.data)
        }
        AuthManager.setActivity(this, googleSignInLauncher)
        
        enableEdgeToEdge()
        
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(android.Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(android.Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissions.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        val missingPermissions = permissions.filter { 
            checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED 
        }
        
        if (missingPermissions.isNotEmpty()) {
            requestPermissions(missingPermissions.toTypedArray(), 101)
        }

        handleIntent(intent)
        
        // Load initial Supabase state
        val prefs = getSharedPreferences("MelodifyAuth", Context.MODE_PRIVATE)
        val savedSessionCode = prefs.getString("SUPABASE_SESSION_TOKEN", null)
        if (savedSessionCode != null) {
            com.melodify.shared.data.storage.SupabaseAuthManager.login(savedSessionCode)
        }
        
        setContent {
            MelodifyTheme {
                MelodifyApp()
            }
        }
    }
    
    override fun onStart() {
        super.onStart()
        val sessionToken = SessionToken(this, ComponentName(this, PlayerService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
    }

    override fun onStop() {
        super.onStop()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            if (uri.scheme == "melodify" && (uri.host == "auth" || uri.host == "callback")) {
                var sessionCode = uri.getQueryParameter("sessionCode")
                
                // Fallback for Supabase OAuth implicit flow which puts token in fragment: #access_token=...
                if (sessionCode == null && uri.fragment != null) {
                    val fragmentParts = uri.fragment?.split("&") ?: emptyList()
                    for (part in fragmentParts) {
                        if (part.startsWith("access_token=")) {
                            sessionCode = part.substringAfter("access_token=")
                            break
                        }
                    }
                }
                
                if (sessionCode != null) {
                    println("Successfully received Supabase session token.")
                    val prefs = getSharedPreferences("MelodifyAuth", Context.MODE_PRIVATE)
                    prefs.edit().putString("SUPABASE_SESSION_TOKEN", sessionCode).apply()
                    com.melodify.shared.data.storage.SupabaseAuthManager.login(sessionCode)
                }
            }
        }
    }
}

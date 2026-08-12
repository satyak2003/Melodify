package com.melodify.android

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
    private val libraryViewModel: LibraryViewModel by inject()
    private var controllerFuture: ListenableFuture<MediaController>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            AuthManager.handleSignInResult(result.data)
        }
        AuthManager.setActivity(this, googleSignInLauncher)
        
        enableEdgeToEdge()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        handleIntent(intent)

        
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
            if (uri.scheme == "melodify" && uri.host == "callback") {
                val code = uri.getQueryParameter("code")
                if (code != null) {
                    libraryViewModel.handleSpotifyAuthCode(code)
                }
            }
        }
    }
}


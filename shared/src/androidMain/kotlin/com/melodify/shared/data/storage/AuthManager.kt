package com.melodify.shared.data.storage

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.GoogleAuthUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual object AuthManager {
    private val _isGoogleLoggedIn = MutableStateFlow(FirebaseAuth.getInstance().currentUser != null)
    actual val isGoogleLoggedIn = _isGoogleLoggedIn.asStateFlow()
    
    private val _userProfileUrl = MutableStateFlow<String?>(FirebaseAuth.getInstance().currentUser?.photoUrl?.toString())
    actual val userProfileUrl = _userProfileUrl.asStateFlow()

    private val _userName = MutableStateFlow<String?>(FirebaseAuth.getInstance().currentUser?.displayName)
    actual val userName = _userName.asStateFlow()

    init {
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            _isGoogleLoggedIn.value = auth.currentUser != null
            _userProfileUrl.value = auth.currentUser?.photoUrl?.toString()
            _userName.value = auth.currentUser?.displayName
        }
    }
    
    // Server Client ID from google-services.json
    private const val CLIENT_ID = "1056247604165-npd0ee1j7jloeakgsk2er6vqhafdf54h.apps.googleusercontent.com"
    
    private var activityContext: Activity? = null
    private var signInLauncher: ActivityResultLauncher<Intent>? = null
    private var signInContinuation: Continuation<GoogleTokens?>? = null
    
    fun setActivity(activity: Activity, launcher: ActivityResultLauncher<Intent>) {
        activityContext = activity
        signInLauncher = launcher
    }

    actual suspend fun loginWithGoogle(): GoogleTokens? = withContext(Dispatchers.Main) {
        val activity = activityContext ?: return@withContext null
        val launcher = signInLauncher ?: return@withContext null
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(CLIENT_ID)
                .requestScopes(Scope("https://www.googleapis.com/auth/youtube.readonly"))
                .requestEmail()
                .build()
                
            val client = GoogleSignIn.getClient(activity, gso)
            
            return@withContext suspendCancellableCoroutine { cont ->
                signInContinuation = cont
                launcher.launch(client.signInIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun handleSignInResult(intent: Intent?) {
        val cont = signInContinuation
        signInContinuation = null
        if (cont == null) return
        val activity = activityContext ?: return cont.resume(null)
        
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            if (task.isSuccessful) {
                val account = task.result
                val idToken = account?.idToken
                val googleAccount = account?.account
                
                if (idToken != null && googleAccount != null) {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential)
                        .addOnCompleteListener { authTask ->
                            if (authTask.isSuccessful) {
                                _isGoogleLoggedIn.value = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        val accessToken = GoogleAuthUtil.getToken(activity, googleAccount, "oauth2:https://www.googleapis.com/auth/youtube.readonly")
                                        cont.resume(GoogleTokens(accessToken, idToken))
                                    } catch (e: Exception) {
                                        cont.resume(GoogleTokens("", idToken))
                                    }
                                }
                            } else {
                                cont.resume(null)
                            }
                        }
                    return
                }
            }
            cont.resume(null)
        } catch (e: Exception) {
            System.err.println("Google SignIn Android Error:")
            e.printStackTrace()
            cont.resume(null)
        }
    }

    actual suspend fun logout() {
        _isGoogleLoggedIn.value = false
        FirebaseAuth.getInstance().signOut()
        val activity = activityContext ?: return
        try {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            val client = GoogleSignIn.getClient(activity, gso)
            suspendCancellableCoroutine { cont ->
                client.signOut().addOnSuccessListener {
                    cont.resume(Unit)
                }.addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
            }
        } catch (e: Exception) {}
    }
}

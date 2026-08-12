package com.melodify.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.melodify.android.ui.navigation.Screen
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
@Composable
fun SplashScreen(navController: NavController) {
    // Load Lottie JSON from CMP resources
    var lottieJson by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val res = runCatching { com.melodify.shared.resources.Res.readBytes("files/logo_animated.json").decodeToString() }
        lottieJson = res.getOrNull()
        delay(4500) // Wait for animation to finish
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.Color(0xFF140D24),
                        MaterialTheme.colorScheme.background
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (lottieJson != null) {
            val composition by rememberLottieComposition(LottieCompositionSpec.JsonString(lottieJson!!))
            LottieAnimation(
                composition = composition,
                iterations = 1,
                modifier = Modifier.size(200.dp)
            )
        } else {
            androidx.compose.material3.CircularProgressIndicator()
        }
    }
}

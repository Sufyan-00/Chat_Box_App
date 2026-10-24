package com.example.chat_box_android.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.ui.components.UCLogo
import com.example.chat_box_android.ui.theme.White
import com.example.chat_box_android.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: (isAuthenticated: Boolean) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2000) // 2 second splash duration
        
        // Check if user is authenticated
        val isAuthenticated = viewModel.isAuthenticated()
        onSplashFinished(isAuthenticated)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .windowInsetsPadding(WindowInsets.systemBars),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        UCLogo(
            modifier = Modifier.alpha(alphaAnim),
            height = 50.dp
        )
    }
}

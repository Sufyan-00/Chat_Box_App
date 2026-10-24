package com.example.chat_box_android.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Types of custom toast messages
 */
enum class ToastType {
    SUCCESS,  // Green with checkmark
    ERROR,    // Red with error icon
    INFO      // Blue with info icon
}

/**
 * Show a custom toast message with Material3 styling
 */
fun showCustomToast(
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    message: String,
    type: ToastType = ToastType.INFO,
    duration: SnackbarDuration = SnackbarDuration.Short
) {
    scope.launch {
        // Store the type in the message as prefix (will be parsed in CustomSnackbar)
        val prefixedMessage = "${type.name}|$message"
        snackbarHostState.showSnackbar(
            message = prefixedMessage,
            duration = duration
        )
    }
}

/**
 * Custom Snackbar with colored background and icon based on type
 */
@Composable
fun CustomSnackbar(
    snackbarData: SnackbarData,
    modifier: Modifier = Modifier
) {
    // Parse type from message prefix
    val fullMessage = snackbarData.visuals.message
    val parts = fullMessage.split("|", limit = 2)
    val type = if (parts.size == 2) {
        try {
            ToastType.valueOf(parts[0])
        } catch (e: Exception) {
            ToastType.INFO
        }
    } else {
        ToastType.INFO
    }
    val message = if (parts.size == 2) parts[1] else fullMessage
    
    val (backgroundColor, contentColor, icon) = when (type) {
        ToastType.SUCCESS -> Triple(
            Color(0xFF4CAF50),  // Green
            Color.White,
            Icons.Default.CheckCircle
        )
        ToastType.ERROR -> Triple(
            Color(0xFFF44336),  // Red
            Color.White,
            Icons.Default.Error
        )
        ToastType.INFO -> Triple(
            Color(0xFF2196F3),  // Blue
            Color.White,
            Icons.Default.Info
        )
    }
    
    Snackbar(
        modifier = modifier.padding(12.dp),
        shape = RoundedCornerShape(12.dp),
        containerColor = backgroundColor,
        contentColor = contentColor,
        action = null
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.padding(end = 12.dp)
            )
            Text(text = message, color = contentColor)
        }
    }
}

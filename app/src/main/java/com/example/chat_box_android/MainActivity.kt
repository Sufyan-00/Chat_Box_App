package com.example.chat_box_android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.chat_box_android.navigation.ChatboxNavigation
import com.example.chat_box_android.ui.theme.ChatboxTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ChatboxTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ChatboxNavigation()
                }
            }
        }
    }
}
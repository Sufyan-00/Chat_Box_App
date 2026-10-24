package com.example.chat_box_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat_box_android.R
import com.example.chat_box_android.ui.components.UCIcon
import com.example.chat_box_android.ui.theme.DarkBackground
import com.example.chat_box_android.ui.theme.GradientPurple
import com.example.chat_box_android.ui.theme.MediumGray
import com.example.chat_box_android.ui.theme.TealPrimary
import com.example.chat_box_android.ui.theme.TextWhite
import com.example.chat_box_android.ui.theme.White

@Composable
fun OnboardingScreen(
    onLoginClick: (Boolean) -> Unit
) {
    val context = LocalContext.current
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GradientPurple,
                        DarkBackground
                    ),
                    radius = 800f
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo at top
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                UCIcon(size = 32.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "UNYTED.CHAT",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main headline
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = White, fontWeight = FontWeight.Bold)) {
                        append("Connect\nfriends\n")
                    }
                    withStyle(style = SpanStyle(color = TealPrimary, fontWeight = FontWeight.Bold)) {
                        append("easily &\nquickly")
                    }
                },
                fontSize = 48.sp,
                lineHeight = 56.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Subtitle
            Text(
                text = "Our chat app is the perfect way to stay\nconnected with friends and family.",
                color = MediumGray,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.weight(1f))

            // Sign up with mail button
            Button(
                onClick = { onLoginClick(true) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = White
                )
            ) {
                Text(
                    text = "Sign up with mail",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Existing account link
            Row {
                Text(
                    text = "Existing account? ",
                    color = MediumGray,
                    fontSize = 14.sp
                )
                Text(
                    text = "Log in",
                    color = White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onLoginClick(false) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

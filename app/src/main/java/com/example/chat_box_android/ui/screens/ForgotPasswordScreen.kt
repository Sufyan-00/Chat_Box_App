package com.example.chat_box_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.ui.theme.*
import com.example.chat_box_android.ui.viewmodels.AuthState
import com.example.chat_box_android.ui.viewmodels.AuthViewModel

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit,
    onOTPSent: (String) -> Unit, // Navigate to OTP screen with email
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    
    var email by remember { mutableStateOf("") }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.ForgotPasswordOTPSent -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.ForgotPasswordOTPSent).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAuthState()
                onOTPSent(email)
            }
            is AuthState.Error -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.Error).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            
            // Back button
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Title
            Text(
                text = "Forgot Password?",
                color = TealPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle
            Text(
                text = "Enter your email address and we'll send you a verification code to reset your password.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Email field
            Text(
                text = "Email Address",
                color = TealPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter your email", color = MediumGray) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = LightGray,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Send OTP button
            Button(
                onClick = {
                    viewModel.sendForgotPasswordOTP(email)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealPrimary,
                    disabledContainerColor = LightGray
                ),
                enabled = authState !is AuthState.Loading
            ) {
                Text(
                    text = if (authState is AuthState.Loading) "Sending..." else "Send Verification Code",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

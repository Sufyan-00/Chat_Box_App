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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.R
import com.example.chat_box_android.ui.theme.LightGray
import com.example.chat_box_android.ui.theme.MediumGray
import com.example.chat_box_android.ui.theme.TealPrimary
import com.example.chat_box_android.ui.theme.TextPrimary
import com.example.chat_box_android.ui.theme.TextSecondary
import com.example.chat_box_android.ui.theme.White
import com.example.chat_box_android.ui.viewmodels.AuthState
import com.example.chat_box_android.ui.viewmodels.AuthViewModel

@Composable
fun LoginScreen(
    isSignUp: Boolean,
    onBackClick: () -> Unit,
    onSendOTP: (String, Boolean) -> Unit,
    onLoginSuccess: () -> Unit = {},
    onForgotPasswordClick: () -> Unit = {},
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.RegisterSuccess -> {
                Toast.makeText(context, (authState as AuthState.RegisterSuccess).message, Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
                // Navigate to OTP screen for email verification
                onSendOTP(email, false)
            }
            is AuthState.LoginSuccess -> {
                val loginSuccess = authState as AuthState.LoginSuccess
                if (loginSuccess.needsEmailVerification) {
                    // Navigate to OTP screen for email verification
                    onSendOTP(email, false)
                } else {
                    // Navigate directly to home
                    onLoginSuccess()
                }
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
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
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(color = TealPrimary, fontWeight = FontWeight.Bold)) {
                    append(if (isSignUp) "Sign up" else "Log in")
                }
                withStyle(style = SpanStyle(color = TextPrimary, fontWeight = FontWeight.Bold)) {
                    append(" to UNYTED.CHAT")
                }
            },
            fontSize = 24.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle
        Text(
            text = if (isSignUp) 
                "Create your account to start chatting"
            else 
                "Welcome back! Sign in to continue",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Username field (for both signup and login)
        Text(
            text = "Username",
            color = TealPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your username", color = MediumGray) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = LightGray,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Email field (only for signup)
        if (isSignUp) {
            Text(
                text = "Email",
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = LightGray,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Password field
        Text(
            text = "Password",
            color = TealPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Enter your password", color = MediumGray) },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
                        tint = MediumGray
                    )
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = TealPrimary,
                unfocusedBorderColor = LightGray,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Submit button
        Button(
            onClick = {
                if (isSignUp) {
                    viewModel.register(username, email, password)
                } else {
                    viewModel.login(username, password)
                }
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
                text = if (authState is AuthState.Loading) "Loading..." else if (isSignUp) "Sign Up" else "Log In",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Forgot Password (only show on login screen)
        if (!isSignUp) {
            Text(
                text = "Forgot Password?",
                color = TealPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .clickable {
                        onForgotPasswordClick()
                    }
                    .padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
        } // End inner scrollable Column
    } // End outer Column
}


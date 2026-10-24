package com.example.chat_box_android.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.ui.theme.*
import com.example.chat_box_android.ui.viewmodels.AuthState
import com.example.chat_box_android.ui.viewmodels.AuthViewModel

@Composable
fun ResetPasswordScreen(
    resetToken: String,
    onBackClick: () -> Unit,
    onPasswordResetSuccess: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.PasswordResetSuccess -> {
                Toast.makeText(
                    context,
                    (authState as AuthState.PasswordResetSuccess).message,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetAuthState()
                onPasswordResetSuccess()
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
                text = "Reset Password",
                color = TealPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Subtitle
            Text(
                text = "Enter your new password below.",
                color = TextSecondary,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // New Password field
            Text(
                text = "New Password",
                color = TealPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Enter new password", color = MediumGray) },
                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                        Icon(
                            imageVector = if (newPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (newPasswordVisible) "Hide password" else "Show password",
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
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Confirm Password field
            Text(
                text = "Confirm Password",
                color = TealPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Confirm new password", color = MediumGray) },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
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
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Reset Password button
            Button(
                onClick = {
                    viewModel.confirmPasswordReset(resetToken, newPassword, confirmPassword)
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
                    text = if (authState is AuthState.Loading) "Resetting..." else "Reset Password",
                    color = White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

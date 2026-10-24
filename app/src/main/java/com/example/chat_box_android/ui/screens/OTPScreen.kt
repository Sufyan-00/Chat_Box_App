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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.ui.theme.LightGray
import com.example.chat_box_android.ui.theme.MediumGray
import com.example.chat_box_android.ui.theme.TealPrimary
import com.example.chat_box_android.ui.theme.TextPrimary
import com.example.chat_box_android.ui.theme.TextSecondary
import com.example.chat_box_android.ui.theme.White
import com.example.chat_box_android.ui.viewmodels.AuthState
import com.example.chat_box_android.ui.viewmodels.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun OTPScreen(
    email: String,
    isForgotPassword: Boolean = false,
    onBackClick: () -> Unit,
    onVerifySuccess: (String?) -> Unit, // resetToken for forgot password, null for email verification
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val authState by viewModel.authState.collectAsState()
    
    var otpValue by remember { mutableStateOf("") }
    var resendTimer by remember { mutableIntStateOf(60) }
    var canResend by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }


    // Focus on OTP input when screen loads
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }


    // Handle auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.VerificationSuccess -> {
                Toast.makeText(context, "Email verified successfully!", Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
                onVerifySuccess(null)
            }
            is AuthState.ForgotPasswordOTPVerified -> {
                val resetToken = (authState as AuthState.ForgotPasswordOTPVerified).resetToken
                Toast.makeText(context, "OTP verified!", Toast.LENGTH_SHORT).show()
                viewModel.resetAuthState()
                onVerifySuccess(resetToken)
            }
            is AuthState.Success -> {
                // OTP sent successfully
                Toast.makeText(context, (authState as AuthState.Success).message, Toast.LENGTH_SHORT).show()
                viewModel.resetAuthState()
            }
            is AuthState.Error -> {
                Toast.makeText(context, (authState as AuthState.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetAuthState()
            }
            else -> {}
        }
    }

    // Countdown timer for resend
    LaunchedEffect(key1 = resendTimer, key2 = canResend) {
        if (resendTimer > 0 && !canResend) {
            delay(1000)
            resendTimer--
        } else if (resendTimer == 0) {
            canResend = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .windowInsetsPadding(WindowInsets.systemBars)
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
            text = "Enter OTP",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Subtitle with masked email
        val maskedEmail = maskEmail(email)
        Text(
            text = "We've sent a verification code to\n$maskedEmail",
            color = TextSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(48.dp))

        // OTP Input boxes
        BasicTextField(
            value = otpValue,
            onValueChange = { newValue ->
                if (newValue.length <= 6 && newValue.all { it.isDigit() }) {
                    otpValue = newValue
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.focusRequester(focusRequester)
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                repeat(6) { index ->
                    OTPBox(
                        value = otpValue.getOrNull(index)?.toString() ?: "",
                        isFocused = index == otpValue.length
                    )
                    if (index < 5) {
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Resend OTP section
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (canResend) {
                Text(
                    text = "Didn't receive code? ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "Resend",
                    color = TealPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable {
                        // Reset timer and resend
                        canResend = false
                        resendTimer = 60
                        otpValue = ""
                        if (isForgotPassword) {
                            viewModel.sendForgotPasswordOTP(email)
                        } else {
                            viewModel.sendVerificationOTP()
                        }
                    }
                )
            } else {
                Text(
                    text = "Resend code in ",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Text(
                    text = "${resendTimer}s",
                    color = TealPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (isForgotPassword) {
                    viewModel.verifyForgotPasswordOTP(email, otpValue)
                } else {
                    viewModel.verifyOTP(otpValue)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = otpValue.length == 6 && authState !is AuthState.Loading,
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = TealPrimary,
                disabledContainerColor = LightGray
            )
        ) {
            Text(
                text = if (authState is AuthState.Loading) "Verifying..." else "Verify",
                color = White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}


@Composable
fun OTPBox(
    value: String,
    isFocused: Boolean
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(LightGray)
            .border(
                width = 2.dp,
                color = if (isFocused) TealPrimary else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

private fun maskEmail(email: String): String {
    if (!email.contains("@")) return email
    
    val parts = email.split("@")
    val localPart = parts[0]
    val domain = parts[1]
    
    val maskedLocal = if (localPart.length <= 2) {
        localPart
    } else {
        "${localPart.take(2)}${"*".repeat(localPart.length - 2)}"
    }
    
    return "$maskedLocal@$domain"
}

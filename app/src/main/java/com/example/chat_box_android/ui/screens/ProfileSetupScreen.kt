package com.example.chat_box_android.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.chat_box_android.ui.components.UCIcon
import com.example.chat_box_android.ui.theme.*

@Composable
fun ProfileSetupScreen(
    onProfileCompleted: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isError by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets.systemBars
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            UCIcon(size = 80.dp)
            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Complete Your Profile",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter your details to continue",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Image Picker
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.3f))
                    .clickable { launcher.launch("image/*") }
                    .border(2.dp, TealPrimary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Add Image",
                        tint = White,
                        modifier = Modifier.size(40.dp)
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(TealPrimary, CircleShape)
                            .size(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add",
                            tint = White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { 
                    name = it
                    isError = false
                },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = isError,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TealPrimary,
                    unfocusedBorderColor = MediumGray,
                    errorBorderColor = Color.Red,
                    focusedLabelColor = TealPrimary,
                    unfocusedLabelColor = MediumGray,
                    cursorColor = TealPrimary,
                    errorCursorColor = Color.Red,
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    errorTextColor = White
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(12.dp)
            )
            if (isError) {
                Text(
                    text = "Name is required",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        isError = true
                    } else {
                        onProfileCompleted()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Complete Profile",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = White
                )
            }
        }
    }
}

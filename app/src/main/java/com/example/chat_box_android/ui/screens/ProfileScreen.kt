package com.example.chat_box_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.ui.theme.*
import com.example.chat_box_android.ui.viewmodels.ProfileUiState
import com.example.chat_box_android.ui.viewmodels.ProfileViewModel
import com.example.chat_box_android.ui.viewmodels.ProfileViewModelFactory
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(LocalContext.current))
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Populate fields when profile loads
    LaunchedEffect(uiState) {
        if (uiState is ProfileUiState.Success) {
            val user = (uiState as ProfileUiState.Success).user
            if (!isEditing) {
                displayName = user.displayName
                bio = user.bio ?: ""
            }
        }
    }

    // Toast messages
    LaunchedEffect(Unit) {
        viewModel.toast.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = TextPrimary, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    if (isEditing) {
                        TextButton(
                            onClick = {
                                viewModel.saveProfile(displayName, bio)
                                isEditing = false
                            },
                            enabled = !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = TealPrimary, strokeWidth = 2.dp)
                            } else {
                                Text("Save", color = TealPrimary, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TealPrimary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CardDark)
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    LoadingSpinner()
                }
            }
            is ProfileUiState.Error -> {
                Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = TextSecondary)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadProfile() }, colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)) {
                            Text("Retry")
                        }
                    }
                }
            }
            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Avatar header
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(TealPrimary.copy(alpha = 0.8f), CardDark)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(88.dp)
                                    .clip(CircleShape)
                                    .background(TealPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (state.user.displayName.firstOrNull()
                                        ?: state.user.username.firstOrNull()
                                        ?: '?').uppercaseChar().toString(),
                                    color = Color.White,
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "@${state.user.username}",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Info cards
                    ProfileCard {
                        ProfileField(
                            label = "Display Name",
                            value = displayName,
                            isEditing = isEditing,
                            onValueChange = { displayName = it },
                            icon = Icons.Default.Person
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f))
                        ProfileField(
                            label = "Email",
                            value = state.user.email,
                            isEditing = false,
                            onValueChange = {},
                            icon = Icons.Default.Email
                        )
                        Divider(color = Color.White.copy(alpha = 0.08f))
                        ProfileField(
                            label = "Bio",
                            value = bio,
                            isEditing = isEditing,
                            onValueChange = { bio = it },
                            icon = Icons.Default.Info,
                            singleLine = false
                        )
                    }

                    Spacer(Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        content = { Column { content() } }
    )
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    singleLine: Boolean = true
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = TealPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            if (isEditing) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = singleLine,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = TealPrimary
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            } else {
                Text(
                    text = value.ifBlank { "–" },
                    color = TextPrimary,
                    fontSize = 15.sp
                )
            }
        }
    }
}

package com.example.chat_box_android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chat_box_android.data.models.Message
import com.example.chat_box_android.data.models.MessageSendStatus
import com.example.chat_box_android.ui.theme.*
import com.example.chat_box_android.ui.screens.LoadingSpinner

import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.chat_box_android.ui.viewmodels.ChatViewModel
import com.example.chat_box_android.ui.viewmodels.ChatUiState
import com.example.chat_box_android.ui.viewmodels.ChatViewModelFactory

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ChatScreen(
    conversationId: String,
    contactName: String = "Chat",
    onBackClick: () -> Unit,
    viewModel: ChatViewModel = viewModel(
        factory = ChatViewModelFactory(LocalContext.current, conversationId)
    )
) {
    var messageText by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    val wsState by viewModel.connectionState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val listState = rememberLazyListState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = { viewModel.fetchMessages(isSwipeRefresh = true) }
    )

    // Scroll to bottom whenever the message count increases
    val messageCount = (uiState as? ChatUiState.Success)?.messages?.size ?: 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Scaffold(
        containerColor = White,
        topBar = {
            ChatHeader(
                contactName = contactName,
                isOnline = wsState == com.example.chat_box_android.data.network.WsConnectionState.CONNECTED,
                onBackClick = onBackClick
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        viewModel.sendMessage(messageText)
                        messageText = ""
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullRefreshState)
        ) {
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        LoadingSpinner()
                    }
                }
                is ChatUiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFE57373),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(state.message, color = TextSecondary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.fetchMessages() },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Retry")
                            }
                        }
                    }
                }
                is ChatUiState.Success -> {
                    if (state.messages.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = TealPrimary.copy(alpha = 0.5f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("No messages yet", color = TextSecondary, fontSize = 16.sp)
                            Text("Say hello! 👋", color = TextSecondary, fontSize = 14.sp)
                        }
                    }
                } else {
                    var messageToDelete by remember { mutableStateOf<Message?>(null) }
                    
                    if (messageToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { messageToDelete = null },
                            title = { Text("Delete Message") },
                            text = { Text("Are you sure you want to delete this message?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteMessage(messageToDelete!!.id)
                                    messageToDelete = null
                                }) { Text("Delete", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { messageToDelete = null }) { Text("Cancel") }
                            }
                        )
                    }

                    // Group messages by formatted Date string
                    val groupedMessages = state.messages.groupBy {
                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                            .format(java.util.Date(it.rawTimeMs))
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp)
                    ) {
                        groupedMessages.forEach { (date, messagesInDate) ->
                            item { DateDivider(date = date) }
                            items(messagesInDate, key = { it.id }) { message ->
                                MessageBubble(
                                    message = message,
                                    senderName = contactName,
                                    onLongPress = { messageToDelete = message }
                                )
                            }
                        }
                    }
                }
            }
        } // Closes when

        PullRefreshIndicator(
            refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = TealPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatHeader(
    contactName: String,
    isOnline: Boolean,
    onBackClick: () -> Unit
) {
    TopAppBar(
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = contactName,
                        tint = White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = contactName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isOnline) "Active now" else "Offline",
                        fontSize = 12.sp,
                        color = if (isOnline) TealPrimary else TextSecondary
                    )
                }
            }
        },
        actions = {
            // Unused items removed per task criteria
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = White
        )
    )
}

@Composable
private fun DateDivider(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date,
            color = TextSecondary,
            fontSize = 14.sp
        )
    }
}


@Composable
private fun MessageBubble(
    message: Message,
    senderName: String,
    onLongPress: () -> Unit
) {
    val isFromMe = message.isFromMe

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isFromMe) Alignment.End else Alignment.Start
    ) {
        // Show sender name for received messages
        if (!isFromMe) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = senderName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        // Message bubble (text only)
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isFromMe) 16.dp else 4.dp,
                        bottomEnd = if (isFromMe) 4.dp else 16.dp
                    )
                )
                .background(if (isFromMe) TealPrimary else LightGray)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { onLongPress() }
                    )
                }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                color = if (isFromMe) White else TextPrimary,
                fontSize = 14.sp
            )
        }

        // Timestamp + send status row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 4.dp)
        ) {
            Text(
                text = message.timestamp,
                color = TextSecondary,
                fontSize = 11.sp
            )
            if (isFromMe) {
                when (message.sendStatus) {
                    MessageSendStatus.PENDING -> Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = "Pending",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    MessageSendStatus.SENT -> Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Sent",
                        tint = TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    MessageSendStatus.READ -> Icon(
                        Icons.Default.DoneAll,
                        contentDescription = "Read",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(12.dp)
                    )
                    MessageSendStatus.FAILED -> Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = "Failed",
                        tint = Color.Red,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}



@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        color = White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Text input
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightGray)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (messageText.isEmpty()) {
                    Text(
                        text = "Write your message...",
                        color = MediumGray,
                        fontSize = 14.sp
                    )
                }
                BasicTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = LocalTextStyle.current.copy(
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                )
            }


            // Mic or Send
            IconButton(
                onClick = {
                    if (messageText.isNotBlank()) {
                        onSend()
                    }
                }
            ) {
                if (messageText.isNotBlank()) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = TealPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Create,
                        contentDescription = "Voice",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

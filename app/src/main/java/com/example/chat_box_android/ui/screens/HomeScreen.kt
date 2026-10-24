package com.example.chat_box_android.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.chat_box_android.data.models.ChatConversation
import com.example.chat_box_android.data.models.SampleData
import com.example.chat_box_android.data.models.StatusUser
import com.example.chat_box_android.ui.theme.*
import com.example.chat_box_android.data.models.UserSearchResult
import com.example.chat_box_android.data.models.ChatRequest
import com.example.chat_box_android.ui.viewmodels.HomeUiState
import com.example.chat_box_android.ui.viewmodels.HomeViewModel
import com.example.chat_box_android.ui.viewmodels.ViewModelFactory
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(
    onChatClick: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(
        factory = ViewModelFactory(LocalContext.current)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val requestsState by viewModel.requestsState.collectAsState()
    val incomingCount by viewModel.incomingCount.collectAsState()
    var revealedChatId by remember { mutableStateOf<String?>(null) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showRequestsSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    // Toast events
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSearchSheet = true },
                containerColor = TealPrimary,
                contentColor = White,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "New Chat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            HomeHeader(
                onProfileClick = onProfileClick,
                onLogout = onLogout,
                onSearchClick = { showSearchSheet = true },
                incomingBadge = incomingCount,
                onRequestsClick = {
                    viewModel.loadChatRequests()
                    showRequestsSheet = true
                }
            )

            val pullRefreshState = rememberPullRefreshState(
                refreshing = isRefreshing,
                onRefresh = { viewModel.fetchConversations(isSwipeRefresh = true) }
            )

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .pullRefresh(pullRefreshState),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = White
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    var conversationToDelete by remember { mutableStateOf<String?>(null) }

                    if (conversationToDelete != null) {
                        AlertDialog(
                            onDismissRequest = { conversationToDelete = null },
                            title = { Text("Delete Conversation") },
                            text = { Text("Are you sure you want to delete this conversation? This action cannot be undone.") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.deleteConversation(conversationToDelete!!)
                                    if (revealedChatId == conversationToDelete) revealedChatId = null
                                    conversationToDelete = null
                                }) { Text("Delete", color = Color.Red) }
                            },
                            dismissButton = {
                                TextButton(onClick = { conversationToDelete = null }) { Text("Cancel") }
                            }
                        )
                    }

                    MessagesTab(
                        uiState = uiState,
                        revealedChatId = revealedChatId,
                        onChatClick = { id ->
                            if (revealedChatId != null) revealedChatId = null
                            else onChatClick(id)
                        },
                        onDeleteChat = { id ->
                            conversationToDelete = id
                        },
                        onMuteChat = { revealedChatId = null },
                        onReveal = { id -> revealedChatId = if (revealedChatId == id) null else id },
                        onCollapse = { revealedChatId = null },
                        onRetry = { viewModel.fetchConversations() }
                    )

                    PullRefreshIndicator(
                        refreshing = isRefreshing,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        contentColor = TealPrimary
                    )
                }
            }
        }

    // ── User Search Bottom Sheet ──────────────────────────────────────────────
    if (showSearchSheet) {
        UserSearchSheet(
            query = searchQuery,
            results = searchResults,
            isSearching = isSearching,
            onQueryChange = { viewModel.onSearchQueryChange(it) },
            onSendRequest = { result ->
                val existing = (uiState as? HomeUiState.Success)?.conversations?.find {
                    it.memberIds.contains(result.id) && it.memberIds.size == 2
                }
                if (existing != null) {
                    onChatClick(existing.id)
                } else {
                    viewModel.sendChatRequest(result.id, result.username)
                }
                showSearchSheet = false
            },
            onDismiss = {
                viewModel.clearSearch()
                showSearchSheet = false
            }
        )
    }

    // ── Requests Bottom Sheet ─────────────────────────────────────────────────
    if (showRequestsSheet) {
        RequestsSheet(
            incoming = requestsState.incoming,
            outgoing = requestsState.outgoing,
            isLoading = requestsState.isLoading,
            onAccept = { id ->
                val req = requestsState.incoming.find { it.id == id }
                viewModel.acceptRequest(id, req?.senderUsername ?: "")
            },
            onReject = { viewModel.rejectRequest(it) },
            onDismiss = { showRequestsSheet = false }
        )
    }
}
}

@Composable
private fun MessagesTab(
    uiState: HomeUiState,
    revealedChatId: String?,
    onChatClick: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onMuteChat: (String) -> Unit,
    onReveal: (String) -> Unit,
    onCollapse: () -> Unit,
    onRetry: () -> Unit
) {
    when (uiState) {
        is HomeUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingSpinner()
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Loading chats...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
        is HomeUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFE57373),
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Couldn't load chats",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = uiState.message,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = TealPrimary)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retry")
                    }
                }
            }
        }
        is HomeUiState.Success -> {
            if (uiState.conversations.isEmpty()) {
                EmptyChatsState()
            } else {
                ChatList(
                    conversations = uiState.conversations,
                    revealedChatId = revealedChatId,
                    onChatClick = onChatClick,
                    onDeleteChat = onDeleteChat,
                    onMuteChat = onMuteChat,
                    onReveal = onReveal,
                    onCollapse = onCollapse
                )
            }
        }
    }
}

@Composable
private fun EmptyChatsState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(TealPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Email,
                    contentDescription = null,
                    tint = TealPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No conversations yet",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap the ✏ button to start a new chat",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NewChatDialog(
    onDismiss: () -> Unit,
    onCreateChat: (title: String, memberIds: List<String>) -> Unit
) {
    var chatTitle by remember { mutableStateOf("") }
    var memberIdsText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = White,
        title = {
            Text(
                text = "New Conversation",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = TextPrimary
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Enter a title and member user IDs (comma-separated) to create a conversation.", color = TextSecondary, fontSize = 13.sp)
                OutlinedTextField(
                    value = chatTitle,
                    onValueChange = { chatTitle = it },
                    label = { Text("Conversation Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        focusedLabelColor = TealPrimary
                    )
                )
                OutlinedTextField(
                    value = memberIdsText,
                    onValueChange = { memberIdsText = it },
                    label = { Text("Member User IDs (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("uuid1, uuid2, ...", color = MediumGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        focusedLabelColor = TealPrimary
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (chatTitle.isNotBlank()) {
                        val members = memberIdsText
                            .split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        onCreateChat(chatTitle.trim(), members)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                enabled = chatTitle.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun HomeHeader(
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    onSearchClick: () -> Unit,
    incomingBadge: Int = 0,
    onRequestsClick: () -> Unit = {}
) {
    var showLogoutDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // New chat / Search icon
        IconButton(
            onClick = onSearchClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "New Chat",
                tint = White
            )
        }

        Text(
            text = "Messages",
            color = White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Requests button with badge
            Box {
                IconButton(
                    onClick = onRequestsClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkChatUnread,
                        contentDescription = "Requests",
                        tint = White
                    )
                }
                if (incomingBadge > 0) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                            .align(Alignment.TopEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(incomingBadge.toString(), color = White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            // Logout button
            IconButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout",
                    tint = White
                )
            }

            // Profile avatar
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(TealPrimary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatusRow(statusUsers: List<StatusUser>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(statusUsers) { user ->
            StatusItem(user = user)
        }
    }
}

@Composable
private fun StatusItem(user: StatusUser) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Box {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (user.isMyStatus) Color.Gray else TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = user.name,
                    tint = White,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Add icon for "My status"
            if (user.isMyStatus) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(TealPrimary)
                        .align(Alignment.BottomEnd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add status",
                        tint = White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = user.name,
            color = White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ChatList(
    conversations: List<ChatConversation>,
    revealedChatId: String?,
    onChatClick: (String) -> Unit,
    onDeleteChat: (String) -> Unit,
    onMuteChat: (String) -> Unit,
    onReveal: (String) -> Unit,
    onCollapse: () -> Unit
) {
    // Close revealed item on back press
    BackHandler(enabled = revealedChatId != null) {
        onCollapse()
    }

    LazyColumn(
        contentPadding = PaddingValues(vertical = 16.dp),
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures(onTap = {
                if (revealedChatId != null) onCollapse()
            })
        }
    ) {
        items(
            items = conversations,
            key = { it.id }
        ) { conversation ->
            SwipeableChatItem(
                conversation = conversation,
                isRevealed = revealedChatId == conversation.id,
                onClick = { onChatClick(conversation.id) },
                onDelete = { onDeleteChat(conversation.id) },
                onMute = { onMuteChat(conversation.id) },
                onReveal = { onReveal(conversation.id) },
                onCollapse = onCollapse
            )
        }
    }
}

@Composable
private fun SwipeableChatItem(
    conversation: ChatConversation,
    isRevealed: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onMute: () -> Unit,
    onReveal: () -> Unit,
    onCollapse: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()

    val actionWidth = 110.dp
    val actionWidthPx = with(density) { actionWidth.toPx() }

    val offsetX = remember { Animatable(0f) }

    // Sync external state with internal animation
    LaunchedEffect(isRevealed) {
        if (isRevealed) {
            offsetX.animateTo(-actionWidthPx)
        } else {
            offsetX.animateTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Actions Background
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(actionWidth)
                .background(Color(0xFFFFE5E5)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            IconButton(
                onClick = onMute,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(LightGray)
            ) {
                Icon(
                    imageVector = if (conversation.isMuted) Icons.Default.Notifications else Icons.Outlined.Notifications,
                    contentDescription = if (conversation.isMuted) "Unmute" else "Mute",
                    tint = TextSecondary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    coroutineScope.launch {
                        offsetX.animateTo(0f)
                        onDelete()
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFE5E5))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red
                )
            }
        }

        // Foreground Content
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            val newOffset = (offsetX.value + delta).coerceIn(-actionWidthPx, 0f)
                            offsetX.snapTo(newOffset)
                        }
                    },
                    onDragStopped = {
                        if (offsetX.value < -actionWidthPx / 2) {
                            onReveal()
                        } else {
                            onCollapse()
                        }
                    }
                )
        ) {
            ChatItemContent(
                conversation = conversation,
                onClick = onClick
            )
        }
    }
}

@Composable
private fun ChatItemContent(
    conversation: ChatConversation,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(White)
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with online indicator
        Box {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(TealPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.name.take(1).uppercase(),
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50))
                        .align(Alignment.BottomEnd)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name and message
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = conversation.name,
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = conversation.lastMessage,
                color = TextSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Time and unread badge
        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = conversation.timestamp,
                color = TextSecondary,
                fontSize = 12.sp
            )

            if (conversation.unreadCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(TealPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.unreadCount.toString(),
                        color = White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = White,
        contentColor = TextSecondary
    ) {
        val items = listOf(
            BottomNavItem("Message", Icons.Default.Email, Icons.Outlined.Email),
            BottomNavItem("Calls", Icons.Default.Call, Icons.Outlined.Call),
            BottomNavItem("Contacts", Icons.Default.Person, Icons.Outlined.Person),
            BottomNavItem("Settings", Icons.Default.Settings, Icons.Outlined.Settings)
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 12.sp
                    )
                },
                selected = selectedIndex == index,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = TealPrimary,
                    selectedTextColor = TealPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = White
                )
            )
        }
    }
}

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coming soon",
                fontSize = 16.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun LoadingSpinner(
    color: Color = TealPrimary,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    strokeWidth: androidx.compose.ui.unit.Dp = 4.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    Canvas(
        modifier = Modifier
            .size(size)
            .rotate(rotation)
    ) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }
}

// ── User Search Sheet ──────────────────────────────────────────────────────────

@Composable
fun UserSearchSheet(
    query: String,
    results: List<com.example.chat_box_android.data.models.UserSearchResult>,
    isSearching: Boolean,
    onQueryChange: (String) -> Unit,
    onSendRequest: (com.example.chat_box_android.data.models.UserSearchResult) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Find People", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text("Search by username or email") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (isSearching) {
                            LoadingSpinner(size = 20.dp, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Search, contentDescription = null)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TealPrimary,
                        focusedLabelColor = TealPrimary
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (results.isEmpty() && query.isNotBlank() && !isSearching) {
                    Text(
                        "No users found",
                        color = TextSecondary,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(results) { user ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = user.displayName.ifBlank { user.username },
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "@${user.username}",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                            Button(
                                onClick = { onSendRequest(user) },
                                colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Request", fontSize = 12.sp)
                            }
                        }
                        Divider(color = LightGray)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        }
    )
}

// ── Requests Sheet ─────────────────────────────────────────────────────────────

@Composable
fun RequestsSheet(
    incoming: List<com.example.chat_box_android.data.models.ChatRequest>,
    outgoing: List<com.example.chat_box_android.data.models.ChatRequest>,
    isLoading: Boolean,
    onAccept: (String) -> Unit,
    onReject: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showIncoming by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Chat Requests", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Toggle: Incoming | Outgoing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(LightGray),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(true to "Incoming", false to "Outgoing").forEach { (isInc, label) ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (showIncoming == isInc) TealPrimary else Color.Transparent)
                                .clickable { showIncoming = isInc }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (showIncoming == isInc) White else TextSecondary,
                                fontWeight = if (showIncoming == isInc) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        LoadingSpinner()
                    }
                } else {
                    val list = if (showIncoming) incoming else outgoing
                    if (list.isEmpty()) {
                        Text(
                            "No requests",
                            color = TextSecondary,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                    LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                        items(list) { req ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                Text(
                                    text = if (showIncoming) "From: ${req.sender}" else "To: ${req.recipient}",
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Status: ${req.status}",
                                    color = when (req.status) {
                                        "pending" -> Color(0xFFFFA000)
                                        "accepted" -> Color(0xFF4CAF50)
                                        else -> Color.Red
                                    },
                                    fontSize = 13.sp
                                )
                                if (showIncoming && req.status == "pending") {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(top = 6.dp)
                                    ) {
                                        Button(
                                            onClick = { onAccept(req.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealPrimary),
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("Accept", fontSize = 12.sp) }
                                        OutlinedButton(
                                            onClick = { onReject(req.id) },
                                            contentPadding = PaddingValues(horizontal = 12.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) { Text("Reject", fontSize = 12.sp, color = Color.Red) }
                                    }
                                }
                            }
                            Divider(color = LightGray)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = TextSecondary) }
        }
    )
}

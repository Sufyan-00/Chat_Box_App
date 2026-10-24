# Chat Box

Chat Box is an Android chat application built with Kotlin and Jetpack Compose.

The app communicates with a backend using REST APIs and WebSockets. It supports authentication, conversations, messaging, and profile-related screens.

## Main Features

- User registration and login
- Email verification and password recovery
- Conversation and chat screens
- REST API communication using Retrofit
- WebSocket messaging using OkHttp
- Message acknowledgements and receipt events
- WebSocket reconnection handling
- Pending-message queue
- Local token handling
- Conversation-name caching

## Networking

The networking code is located in:

```text
app/src/main/java/com/example/chat_box_android/data/network/
```

Main networking files:

- `ApiConfig.kt`
- `RetrofitClient.kt`
- `AuthInterceptor.kt`
- `WebSocketManager.kt`
- `services/`

The REST API uses Retrofit, Gson, and OkHttp. `AuthInterceptor.kt` attaches access tokens to authenticated requests and attempts token refresh after an unauthorized response.

The current project uses backend URLs configured in `ApiConfig.kt`. These URLs should be moved to environment-specific configuration before production use.

## WebSockets

WebSocket functionality is implemented in `WebSocketManager.kt`.

The implementation supports:

- Connecting with an access token
- Tracking connection state
- Subscribing to conversations
- Sending messages
- Sending message receipts
- Receiving new-message events
- Receiving message acknowledgement events
- Receiving receipt events
- Queuing messages while disconnected
- Reconnecting after connection failures

`ChatViewModel.kt` and `MessagingRepository.kt` use the WebSocket manager for chat messaging. `PendingMessageQueue.kt` stores messages that are waiting to be sent or confirmed.

## Local Data Handling

The project does not contain a direct database layer. It uses local classes for limited data handling:

- `TokenManager.kt` — authentication token handling
- `PendingMessageQueue.kt` — pending message storage
- `ConversationNameCache.kt` — conversation-name caching

Chat and authentication data are primarily accessed through the configured backend API.

## Tech Stack

- Kotlin
- Android
- Jetpack Compose
- Material 3
- Retrofit
- OkHttp WebSockets
- Gson
- Kotlin Coroutines
- Coil

## Build Requirements

- Android Studio
- JDK 17
- Android SDK 34
- Minimum Android version: API 26

## Security Note

The current configuration uses HTTP and an unencrypted WebSocket URL. Production deployments should use HTTPS and WSS, and backend URLs should not be hard-coded in source code.

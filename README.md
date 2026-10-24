# UNYTED.CHAT

A modern Android chat application built with Kotlin and Jetpack Compose. **UNYTED.CHAT** provides user onboarding, authentication, OTP verification, password recovery, profile setup, conversations, real-time messaging, and profile management.

The application communicates with a backend through REST APIs and WebSockets. Authentication tokens are stored locally, pending messages can be queued, and conversation names are cached to improve the user experience.

## Features

- Splash screen and onboarding flow
- User login and authentication
- OTP verification
- Forgot-password and reset-password flows
- Profile setup and profile management
- Conversation list and chat screens
- Real-time messaging through WebSockets
- REST API integration for authentication and messaging
- Secure local token storage
- Pending message queue for messages that cannot be sent immediately
- Conversation-name caching
- Image loading with Coil
- Material 3 user interface built with Jetpack Compose

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android
- **UI:** Jetpack Compose and Material 3
- **Build system:** Gradle Kotlin DSL
- **Android Gradle Plugin:** 8.7.2
- **Kotlin:** 2.0.0
- **Compile SDK:** 34
- **Target SDK:** 34
- **Minimum SDK:** 26
- **Java/Kotlin JVM target:** 17

### Main Libraries

- AndroidX Core KTX
- Jetpack Compose
- Compose Material 3
- Navigation Compose
- Retrofit
- Gson Converter
- OkHttp
- OkHttp Logging Interceptor
- Kotlin Coroutines
- AndroidX ViewModel and LiveData
- AndroidX Security Crypto
- RecyclerView
- Coil Compose
- JUnit
- AndroidX Test and Espresso

## Project Structure

```text
Chat_Box_App/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/example/chat_box_android/
│       │   │   ├── MainActivity.kt
│       │   │   ├── data/
│       │   │   │   ├── local/
│       │   │   │   │   ├── ConversationNameCache.kt
│       │   │   │   │   ├── PendingMessageQueue.kt
│       │   │   │   │   └── TokenManager.kt
│       │   │   │   ├── models/
│       │   │   │   │   ├── AuthModels.kt
│       │   │   │   │   ├── ChatModels.kt
│       │   │   │   │   ├── Message.kt
│       │   │   │   │   └── MessagingModels.kt
│       │   │   │   ├── network/
│       │   │   │   │   ├── ApiConfig.kt
│       │   │   │   │   ├── AuthInterceptor.kt
│       │   │   │   │   ├── RetrofitClient.kt
│       │   │   │   │   ├── WebSocketManager.kt
│       │   │   │   │   └── services/
│       │   │   │   └── repository/
│       │   │   │       ├── AuthRepository.kt
│       │   │   │       └── MessagingRepository.kt
│       │   │   ├── di/
│       │   │   │   └── Injection.kt
│       │   │   ├── navigation/
│       │   │   │   └── Navigation.kt
│       │   │   └── ui/
│       │   │       ├── components/
│       │   │       ├── screens/
│       │   │       │   ├── SplashScreen.kt
│       │   │       │   ├── OnboardingScreen.kt
│       │   │       │   ├── LoginScreen.kt
│       │   │       │   ├── OTPScreen.kt
│       │   │       │   ├── ForgotPasswordScreen.kt
│       │   │       │   ├── ResetPasswordScreen.kt
│       │   │       │   ├── ProfileSetupScreen.kt
│       │   │       │   ├── HomeScreen.kt
│       │   │       │   ├── ChatScreen.kt
│       │   │       │   └── ProfileScreen.kt
│       │   │       ├── theme/
│       │   │       └── viewmodels/
│       │   │           ├── AuthViewModel.kt
│       │   │           ├── ChatViewModel.kt
│       │   │           ├── HomeViewModel.kt
│       │   │           ├── ProfileViewModel.kt
│       │   │           └── ViewModelFactory.kt
│       │   └── res/
│       │       ├── drawable/
│       │       ├── mipmap-*/
│       │       ├── values/
│       │       └── xml/
│       ├── test/
│       └── androidTest/
├── gradle/
│   └── libs.versions.toml
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
└── gradlew.bat
```

## Architecture Overview

The application follows a layered Android architecture:

- **UI layer:** Compose screens and reusable UI components
- **ViewModel layer:** UI state management and coordination of user actions
- **Repository layer:** Abstraction over authentication and messaging data sources
- **Network layer:** Retrofit REST clients, OkHttp interceptors, and WebSocket communication
- **Local layer:** Secure token storage, conversation caching, and pending-message handling
- **Navigation layer:** Navigation between onboarding, authentication, chat, and profile screens

### Application flow

`MainActivity` initializes the Compose UI, applies `ChatboxTheme`, and launches `ChatboxNavigation`.

```text
MainActivity
    │
    └── ChatboxNavigation
            │
            ├── SplashScreen
            ├── OnboardingScreen
            ├── LoginScreen
            ├── OTPScreen
            ├── Password Recovery Screens
            ├── ProfileSetupScreen
            └── Main Application
                    ├── HomeScreen
                    ├── ChatScreen
                    └── ProfileScreen
```

ViewModels such as `AuthViewModel`, `HomeViewModel`, `ChatViewModel`, and `ProfileViewModel` expose state to the Compose UI. They delegate data operations to `AuthRepository` and `MessagingRepository`, which use REST APIs and WebSockets to communicate with the backend.

## Requirements

Before building the application, install:

- Android Studio with Kotlin and Jetpack Compose support
- Android SDK 34
- JDK 17
- An Android emulator or physical device running Android 8.0/API 26 or newer
- Access to the configured backend server

## Getting Started

### Clone the repository

```bash
git clone https://github.com/Sufyan-00/Chat_Box_App.git
cd Chat_Box_App
```

### Open the project

Open the project directory in Android Studio and allow Gradle to synchronize the project dependencies.

### Build the debug APK

On macOS or Linux:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The generated APK is normally located at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Install on a connected device

On macOS or Linux:

```bash
./gradlew installDebug
```

On Windows:

```bat
gradlew.bat installDebug
```

You can also run the application directly from Android Studio using the `app` run configuration.

## Testing

Run local JVM unit tests:

```bash
./gradlew test
```

Run Android instrumentation tests on a connected device or emulator:

```bash
./gradlew connectedAndroidTest
```

## Backend and Network Configuration

The application declares the following permissions in `AndroidManifest.xml`:

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`

The network layer is located under:

```text
app/src/main/java/com/example/chat_box_android/data/network/
```

Important files include:

- `ApiConfig.kt` — API and backend configuration
- `RetrofitClient.kt` — Retrofit and OkHttp client setup
- `AuthInterceptor.kt` — Authentication header handling
- `WebSocketManager.kt` — Real-time messaging connection management
- `services/` — REST API service definitions

The current network security configuration permits cleartext traffic for the configured backend host. For production deployments, use HTTPS/WSS and avoid hard-coded backend addresses.

## Security Notes

- Authentication tokens are managed by `TokenManager.kt`.
- AndroidX Security Crypto is included for encrypted local storage.
- Network authentication is handled through `AuthInterceptor.kt`.
- Cleartext HTTP traffic should be disabled in production.
- Backend URLs should be provided through build-time configuration or environment-specific settings rather than committed directly to source control.

## Main Screens

### Authentication

- `SplashScreen.kt`
- `OnboardingScreen.kt`
- `LoginScreen.kt`
- `OTPScreen.kt`
- `ForgotPasswordScreen.kt`
- `ResetPasswordScreen.kt`

### User and Profile

- `ProfileSetupScreen.kt`
- `ProfileScreen.kt`
- `ProfileViewModel.kt`

### Messaging

- `HomeScreen.kt`
- `ChatScreen.kt`
- `HomeViewModel.kt`
- `ChatViewModel.kt`
- `MessagingRepository.kt`
- `WebSocketManager.kt`

## Dependency Management

Dependency and plugin versions are centralized in:

```text
gradle/libs.versions.toml
```

Repositories are configured in `settings.gradle.kts` and include Google Maven, Maven Central, and the Gradle Plugin Portal.

## Build Configuration

The Android module is configured with:

- Namespace: `com.example.chat_box_android`
- Application ID: `com.example.chat_box_android`
- Version code: `1`
- Version name: `1.0`
- Compose enabled
- View Binding enabled
- Release minification disabled by default

## Future Improvements

Potential improvements for future development include:

- Move API and WebSocket URLs into build variants or environment configuration.
- Require HTTPS and secure WebSocket connections in release builds.
- Add comprehensive unit and instrumentation test coverage.
- Add offline-first persistence for conversations and messages.
- Improve error reporting and retry behavior for failed network requests.
- Add message delivery and read-status indicators.
- Add push notifications for new messages.
- Configure release signing and CI/CD automation.
- Add screenshots and demo videos to this README.

## Contributing

1. Fork the repository.
2. Create a feature branch:

   ```bash
   git checkout -b feature/your-feature-name
   ```

3. Make your changes.
4. Run the available tests:

   ```bash
   ./gradlew test
   ```

5. Commit your changes:

   ```bash
   git commit -m "Add your feature"
   ```

6. Push the branch and open a pull request.

## License

No license has been specified for this repository yet. Until a license is added, all rights are reserved by the repository owner.

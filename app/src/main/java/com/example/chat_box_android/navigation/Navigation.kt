package com.example.chat_box_android.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chat_box_android.di.Injection
import com.example.chat_box_android.ui.screens.*
import com.example.chat_box_android.ui.viewmodels.AuthViewModel
import com.example.chat_box_android.ui.viewmodels.ViewModelFactory

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login/{isSignUp}") {
        fun createRoute(isSignUp: Boolean) = "login/$isSignUp"
    }
    object OTP : Screen("otp/{email}/{isForgotPassword}") {
        fun createRoute(email: String, isForgotPassword: Boolean) = "otp/$email/$isForgotPassword"
    }
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password/{resetToken}") {
        fun createRoute(resetToken: String) = "reset_password/$resetToken"
    }
    object ProfileSetup : Screen("profile_setup")
    object Profile : Screen("profile")
    object Home : Screen("home")
    object Chat : Screen("chat/{conversationId}/{contactName}") {
        fun createRoute(conversationId: String, contactName: String) =
            "chat/$conversationId/${java.net.URLEncoder.encode(contactName, "UTF-8")}"
    }
}

@Composable
fun ChatboxNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = { isAuthenticated ->
                    val dest = if (isAuthenticated) Screen.Home.route else Screen.Onboarding.route
                    navController.navigate(dest) { popUpTo(Screen.Splash.route) { inclusive = true } }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onLoginClick = { isSignUp -> navController.navigate(Screen.Login.createRoute(isSignUp)) }
            )
        }

        composable(
            route = Screen.Login.route,
            arguments = listOf(navArgument("isSignUp") { type = NavType.BoolType })
        ) { backStackEntry ->
            val isSignUp = backStackEntry.arguments?.getBoolean("isSignUp") ?: false
            LoginScreen(
                isSignUp = isSignUp,
                onBackClick = { navController.popBackStack() },
                onSendOTP = { email, isForgotPassword ->
                    navController.navigate(Screen.OTP.createRoute(email, isForgotPassword))
                },
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onForgotPasswordClick = { navController.navigate(Screen.ForgotPassword.route) }
            )
        }

        composable(
            route = Screen.OTP.route,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("isForgotPassword") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val email = backStackEntry.arguments?.getString("email") ?: ""
            val isForgotPassword = backStackEntry.arguments?.getBoolean("isForgotPassword") ?: false
            OTPScreen(
                email = email,
                isForgotPassword = isForgotPassword,
                onBackClick = { navController.popBackStack() },
                onVerifySuccess = { resetToken ->
                    if (resetToken != null) {
                        navController.navigate(Screen.ResetPassword.createRoute(resetToken)) {
                            popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                        }
                    } else {
                        // Email verification successful, proceed to setup profile
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            popUpTo(Screen.OTP.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = { navController.popBackStack() },
                onOTPSent = { email -> navController.navigate(Screen.OTP.createRoute(email, true)) }
            )
        }

        composable(
            route = Screen.ResetPassword.route,
            arguments = listOf(navArgument("resetToken") { type = NavType.StringType })
        ) { backStackEntry ->
            val resetToken = backStackEntry.arguments?.getString("resetToken") ?: ""
            ResetPasswordScreen(
                resetToken = resetToken,
                onBackClick = { navController.popBackStack() },
                onPasswordResetSuccess = {
                    navController.navigate(Screen.Login.createRoute(false)) {
                        popUpTo(Screen.Onboarding.route) { inclusive = false }
                    }
                }
            )
        }

        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                onProfileCompleted = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(onBackClick = { navController.popBackStack() })
        }

        composable(Screen.Home.route) {
            val context = LocalContext.current
            val authViewModel: AuthViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return AuthViewModel(context.applicationContext as android.app.Application) as T
                    }
                }
            )

            BackHandler { (context as? Activity)?.finish() }

            // We need the HomeViewModel to resolve conversation names
            val homeViewModel: com.example.chat_box_android.ui.viewmodels.HomeViewModel = viewModel(
                factory = ViewModelFactory(context)
            )

            HomeScreen(
                viewModel = homeViewModel,
                onChatClick = { conversationId ->
                    val contactName = homeViewModel.getConversationName(conversationId)
                    navController.navigate(Screen.Chat.createRoute(conversationId, contactName))
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onLogout = {
                    authViewModel.logout()
                    Injection.reset()
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("contactName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val conversationId = backStackEntry.arguments?.getString("conversationId") ?: ""
            val contactName = backStackEntry.arguments?.getString("contactName")
                ?.let { java.net.URLDecoder.decode(it, "UTF-8") } ?: ""
            ChatScreen(
                conversationId = conversationId,
                contactName = contactName,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

package com.homesajja.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homesajja.app.BuildConfig
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.ui.screens.ComponentPreviewScreen
import com.homesajja.app.ui.screens.LoginScreen
import com.homesajja.app.ui.screens.SignupScreen
import com.homesajja.app.ui.screens.SplashScreen
import com.homesajja.app.ui.screens.UserHomeScreen
import com.homesajja.app.ui.screens.VendorHomeScreen
import com.homesajja.app.ui.screens.WelcomeScreen
import com.homesajja.app.viewmodel.SplashDestination

@Composable
fun HomeSajjaNavHost(navController: NavHostController = rememberNavController()) {
    fun navigateToRoleHome(role: UserRole) {
        val target = if (role == UserRole.USER) Routes.UserHome.route else Routes.VendorHome.route
        navController.navigate(target) {
            popUpTo(Routes.Splash.route) { inclusive = true }
        }
    }

    NavHost(navController = navController, startDestination = Routes.Splash.route) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onNavigate = { destination ->
                    val target = when (destination) {
                        SplashDestination.Welcome -> Routes.Welcome.route
                        SplashDestination.UserHome -> Routes.UserHome.route
                        SplashDestination.VendorHome -> Routes.VendorHome.route
                    }
                    navController.navigate(target) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Welcome.route) {
            WelcomeScreen(
                onGetStartedClick = { navController.navigate(Routes.Signup.route) },
                onPreviewComponentsClick = if (BuildConfig.DEBUG) {
                    { navController.navigate(Routes.ComponentPreview.route) }
                } else {
                    null
                },
            )
        }
        composable(Routes.Signup.route) {
            SignupScreen(
                onSignupSuccess = ::navigateToRoleHome,
                onNavigateToLogin = {
                    navController.navigate(Routes.Login.route) {
                        popUpTo(Routes.Signup.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.Login.route) {
            LoginScreen(
                onLoginSuccess = ::navigateToRoleHome,
                onNavigateToSignup = {
                    navController.navigate(Routes.Signup.route) {
                        popUpTo(Routes.Login.route) { inclusive = true }
                    }
                },
                onBackClick = { navController.popBackStack() },
            )
        }
        composable(Routes.UserHome.route) {
            UserHomeScreen(
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.VendorHome.route) {
            VendorHomeScreen(
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        if (BuildConfig.DEBUG) {
            composable(Routes.ComponentPreview.route) { ComponentPreviewScreen() }
        }
    }
}

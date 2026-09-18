package com.homesajja.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.homesajja.app.BuildConfig
import com.homesajja.app.ui.screens.AuthScreen
import com.homesajja.app.ui.screens.ComponentPreviewScreen
import com.homesajja.app.ui.screens.RoleSelectionScreen
import com.homesajja.app.ui.screens.SplashScreen
import com.homesajja.app.ui.screens.UserHomeScreen
import com.homesajja.app.ui.screens.VendorHomeScreen
import com.homesajja.app.ui.screens.WelcomeScreen

@Composable
fun HomeSajjaNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.Splash.route) {
        composable(Routes.Splash.route) {
            SplashScreen(
                onFinished = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(Routes.Splash.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.Welcome.route) {
            WelcomeScreen(
                onGetStartedClick = { navController.navigate(Routes.Auth.route) },
                onPreviewComponentsClick = if (BuildConfig.DEBUG) {
                    { navController.navigate(Routes.ComponentPreview.route) }
                } else {
                    null
                },
            )
        }
        composable(Routes.Auth.route) { AuthScreen() }
        composable(Routes.RoleSelection.route) { RoleSelectionScreen() }
        composable(Routes.UserHome.route) { UserHomeScreen() }
        composable(Routes.VendorHome.route) { VendorHomeScreen() }

        if (BuildConfig.DEBUG) {
            composable(Routes.ComponentPreview.route) { ComponentPreviewScreen() }
        }
    }
}

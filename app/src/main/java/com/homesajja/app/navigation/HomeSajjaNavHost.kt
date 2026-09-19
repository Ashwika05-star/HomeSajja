package com.homesajja.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.homesajja.app.BuildConfig
import com.homesajja.app.data.model.UserRole
import com.homesajja.app.ui.screens.ComponentPreviewScreen
import com.homesajja.app.ui.screens.LoginScreen
import com.homesajja.app.ui.screens.exchange.ExchangeDetailScreen
import com.homesajja.app.ui.screens.exchange.ExchangeProposalScreen
import com.homesajja.app.ui.screens.listing.ListingDetailScreen
import com.homesajja.app.ui.screens.repair.RepairDetailScreen
import com.homesajja.app.ui.screens.repair.RepairRequestScreen
import com.homesajja.app.ui.screens.sell.SellFlowScreen
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
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                onSell = { navController.navigate(Routes.Sell.createRoute()) },
                onEditListing = { navController.navigate(Routes.Sell.createRoute(it)) },
                onNewExchange = { navController.navigate(Routes.ExchangeNew.createRoute()) },
                onOpenExchange = { navController.navigate(Routes.ExchangeDetail.createRoute(it)) },
                onRequestRepair = { navController.navigate(Routes.RepairNew.route) },
                onOpenRepair = { navController.navigate(Routes.RepairDetail.createRoute(it)) },
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(
            route = Routes.ListingDetail.route,
            arguments = listOf(navArgument("listingId") { type = NavType.StringType }),
        ) {
            ListingDetailScreen(
                onBackClick = { navController.popBackStack() },
                onEditListing = { navController.navigate(Routes.Sell.createRoute(it)) },
                onProposeExchange = { navController.navigate(Routes.ExchangeNew.createRoute(it)) },
            )
        }
        composable(
            route = Routes.ExchangeNew.route,
            arguments = listOf(
                navArgument("requestedListingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            ExchangeProposalScreen(
                onExit = { navController.popBackStack() },
                onSent = { requestId ->
                    // Replace the proposal flow with the request itself as confirmation.
                    navController.navigate(Routes.ExchangeDetail.createRoute(requestId)) {
                        popUpTo(Routes.UserHome.route)
                    }
                },
                onSell = { navController.navigate(Routes.Sell.createRoute()) },
            )
        }
        composable(
            route = Routes.ExchangeDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            ExchangeDetailScreen(
                onBackClick = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
            )
        }
        composable(Routes.RepairNew.route) {
            RepairRequestScreen(
                onExit = { navController.popBackStack() },
                onSent = { requestId ->
                    // Replace the flow with the request itself as confirmation.
                    navController.navigate(Routes.RepairDetail.createRoute(requestId)) {
                        popUpTo(Routes.UserHome.route)
                    }
                },
            )
        }
        composable(
            route = Routes.RepairDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            RepairDetailScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.Sell.route,
            arguments = listOf(
                navArgument("listingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SellFlowScreen(
                onExit = { navController.popBackStack() },
                onPublished = { listingId ->
                    // Replace the sell flow with the listing itself as confirmation.
                    navController.navigate(Routes.ListingDetail.createRoute(listingId)) {
                        popUpTo(Routes.UserHome.route)
                    }
                },
            )
        }
        composable(Routes.VendorHome.route) {
            VendorHomeScreen(
                onOpenRepair = { navController.navigate(Routes.RepairDetail.createRoute(it)) },
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

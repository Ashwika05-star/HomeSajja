package com.homesajja.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.homesajja.app.ui.components.AppTopBar
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
import com.homesajja.app.ui.screens.material.MaterialRequestDetailScreen
import com.homesajja.app.ui.screens.material.MaterialRequestFormScreen
import com.homesajja.app.ui.screens.vendor.VendorProfileEditScreen
import com.homesajja.app.ui.screens.vendor.VendorPublicProfileScreen
import com.homesajja.app.ui.screens.recycle.RecycleDetailScreen
import com.homesajja.app.ui.screens.recycle.RecycleRequestScreen
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
                onRecycle = { navController.navigate(Routes.RecycleNew.route) },
                onOpenRecycling = { navController.navigate(Routes.RecycleDetail.createRoute(it)) },
                onOpenMaterial = { navController.navigate(Routes.MaterialDetail.createRoute(it)) },
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
                onOpenVendor = { navController.navigate(Routes.VendorProfile.createRoute(it)) },
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
                onOpenVendor = { navController.navigate(Routes.VendorProfile.createRoute(it)) },
            )
        }
        composable(
            route = Routes.RepairDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            RepairDetailScreen(onBackClick = { navController.popBackStack() })
        }
        composable(Routes.RecycleNew.route) {
            RecycleRequestScreen(
                onExit = { navController.popBackStack() },
                onSent = { requestId ->
                    // Replace the flow with the request itself as confirmation.
                    navController.navigate(Routes.RecycleDetail.createRoute(requestId)) {
                        popUpTo(Routes.UserHome.route)
                    }
                },
                onOpenVendor = { navController.navigate(Routes.VendorProfile.createRoute(it)) },
            )
        }
        composable(
            route = Routes.RecycleDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            RecycleDetailScreen(onBackClick = { navController.popBackStack() })
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
                        popUpTo(Routes.Sell.route) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.VendorHome.route) {
            VendorHomeScreen(
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                onAddListing = { navController.navigate(Routes.Sell.createRoute()) },
                onEditListing = { navController.navigate(Routes.Sell.createRoute(it)) },
                onOpenExchange = { navController.navigate(Routes.ExchangeDetail.createRoute(it)) },
                onOpenRepair = { navController.navigate(Routes.RepairDetail.createRoute(it)) },
                onOpenRecycling = { navController.navigate(Routes.RecycleDetail.createRoute(it)) },
                onNewMaterialRequest = { navController.navigate(Routes.MaterialForm.createRoute()) },
                onOpenMaterialRequest = { navController.navigate(Routes.MaterialDetail.createRoute(it)) },
                onEditProfile = { navController.navigate(Routes.VendorProfileEdit.route) },
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.VendorProfileEdit.route) {
            VendorProfileEditScreen(onBackClick = { navController.popBackStack() })
        }
        composable(
            route = Routes.VendorProfile.route,
            arguments = listOf(navArgument("vendorId") { type = NavType.StringType }),
        ) {
            Scaffold(
                topBar = { AppTopBar(title = "Vendor", onBackClick = { navController.popBackStack() }) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                VendorPublicProfileScreen(
                    onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                    modifier = Modifier.padding(padding),
                )
            }
        }
        composable(
            route = Routes.MaterialForm.route,
            arguments = listOf(
                navArgument("requestId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            MaterialRequestFormScreen(
                onExit = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.MaterialDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            MaterialRequestDetailScreen(
                onBackClick = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.MaterialForm.createRoute(it)) },
                onOpenVendor = { navController.navigate(Routes.VendorProfile.createRoute(it)) },
            )
        }

        if (BuildConfig.DEBUG) {
            composable(Routes.ComponentPreview.route) { ComponentPreviewScreen() }
        }
    }
}

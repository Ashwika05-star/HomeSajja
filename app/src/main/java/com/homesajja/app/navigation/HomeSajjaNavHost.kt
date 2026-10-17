package com.homesajja.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.homesajja.app.di.AppContainer
import com.homesajja.app.ui.components.BottomTabBar
import com.homesajja.app.ui.theme.LocalDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
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
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.data.model.Recommendation
import com.homesajja.app.ui.components.TrustMenu
import com.homesajja.app.ui.screens.ai.SajjaChatScreen
import com.homesajja.app.ui.screens.ai.SmartDecisionScreen
import com.homesajja.app.ui.screens.profile.BlockedUsersScreen
import com.homesajja.app.ui.screens.profile.SavedScreen
import com.homesajja.app.ui.screens.profile.UserProfileScreen
import com.homesajja.app.viewmodel.HomeViewModel
import com.homesajja.app.di.ViewModelFactory
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.ui.screens.chat.ChatListScreen
import com.homesajja.app.ui.screens.chat.ChatThreadScreen
import com.homesajja.app.ui.screens.chat.NotificationsScreen
import com.homesajja.app.ui.screens.exchange.ExchangeDetailScreen
import com.homesajja.app.ui.screens.exchange.ExchangeProposalScreen
import com.homesajja.app.ui.screens.listing.ListingDetailScreen
import com.homesajja.app.ui.screens.vendor.VendorProfileEditScreen
import com.homesajja.app.ui.screens.vendor.VendorReportScreen
import com.homesajja.app.ui.screens.vendor.VendorPublicProfileScreen
import com.homesajja.app.ui.screens.repair.RepairDetailScreen
import com.homesajja.app.ui.screens.repair.RepairRequestScreen
import com.homesajja.app.ui.screens.sell.SellFlowScreen
import com.homesajja.app.ui.screens.SignupScreen
import com.homesajja.app.ui.screens.SplashScreen
import com.homesajja.app.ui.screens.UserHomeScreen
import com.homesajja.app.ui.screens.VendorHomeScreen
import androidx.compose.runtime.CompositionLocalProvider
import com.homesajja.app.ui.i18n.AppLanguage
import com.homesajja.app.ui.i18n.LocalAppLanguage
import com.homesajja.app.ui.i18n.isAppHindi
import com.homesajja.app.ui.screens.IntroScreen
import com.homesajja.app.viewmodel.SplashDestination

@Composable
fun HomeSajjaNavHost(navController: NavHostController = rememberNavController()) {
    val container = LocalAppContainer.current
    val language by container.languagePreference.languageFlow.collectAsState(initial = AppLanguage.ENGLISH)

    fun navigateToRoleHome(role: UserRole) {
        val target = if (role == UserRole.USER) Routes.UserHome.route else Routes.VendorHome.route
        container.homeTabs.reset()
        navController.navigate(target) {
            popUpTo(Routes.Splash.route) { inclusive = true }
        }
    }

    // A tapped system notification asks for a screen; open it once the person is signed in and past the entry screens.
    val pendingRoute by container.pendingRoute.collectAsState()
    val currentEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    LaunchedEffect(pendingRoute, currentEntry) {
        val route = pendingRoute ?: return@LaunchedEffect
        val current = currentEntry?.destination?.route
        val inside = current == Routes.UserHome.route || current == Routes.VendorHome.route
        if (inside && container.authRepository.isSignedIn()) {
            container.pendingRoute.value = null
            navController.navigate(route)
        } else if (current == Routes.Welcome.route || current == Routes.Login.route) {
            container.pendingRoute.value = null   // signed out: nothing to open
        }
    }

    // Every screen's top bar sits on the page colour, so the status bar icons are dark on the light theme and light on the dark one.
    val view = LocalView.current
    val darkTheme = LocalDarkTheme.current
    LaunchedEffect(currentEntry, darkTheme) {
        val window = (view.context as? android.app.Activity)?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
    }

    CompositionLocalProvider(LocalAppLanguage provides language) {
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
            IntroScreen(
                onSignIn = { navController.navigate(Routes.Login.route) },
                onSignUp = { navController.navigate(Routes.Signup.route) },
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
                onOpenChats = { navController.navigate(Routes.ChatList.route) },
                onOpenNotifications = { navController.navigate(Routes.Notifications.route) },
                onOpenSajja = { navController.navigate(Routes.SajjaChat.route) },
                onOpenProfile = { navController.navigate(Routes.Profile.route) },
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
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
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
                onOpenUser = { id, name -> navController.navigate(Routes.UserProfile.createRoute(id, name)) },
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
                navArgument("offeredListingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val reqId = backStackEntry.arguments?.getString("requestedListingId")
            ExchangeProposalScreen(
                onExit = { navController.popBackStack() },
                onSent = { requestId ->
                    // Replace the proposal flow with the request itself as confirmation.
                    navController.navigate(Routes.ExchangeDetail.createRoute(requestId)) {
                        popUpTo(Routes.UserHome.route)
                    }
                },
                onSell = {
                    navController.navigate(
                        Routes.Sell.createRoute(
                            actionType = com.homesajja.app.data.model.ListingActionType.EXCHANGE.name,
                            returnToExchange = true,
                            requestedListingId = reqId,
                        )
                    )
                },
            )
        }
        composable(
            route = Routes.ExchangeDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            ExchangeDetailScreen(
                onBackClick = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
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
                onAskAi = {
                    navController.navigate(Routes.SmartDecision.route) { popUpTo(Routes.RepairNew.route) { inclusive = true } }
                },
            )
        }
        composable(
            route = Routes.RepairDetail.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            RepairDetailScreen(
                onBackClick = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
            )
        }
        composable(
            route = Routes.Sell.route,
            arguments = listOf(
                navArgument("listingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("actionType") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("returnToExchange") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("requestedListingId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { backStackEntry ->
            val returnToExchange = backStackEntry.arguments?.getBoolean("returnToExchange") ?: false
            val requestedListingId = backStackEntry.arguments?.getString("requestedListingId")
            SellFlowScreen(
                onExit = { navController.popBackStack() },
                onPublished = { listingId ->
                    if (returnToExchange) {
                        navController.navigate(
                            Routes.ExchangeNew.createRoute(
                                requestedListingId = requestedListingId,
                                offeredListingId = listingId,
                            )
                        ) {
                            popUpTo(Routes.Sell.route) { inclusive = true }
                        }
                    } else {
                        // Replace the sell flow with the listing itself as confirmation.
                        navController.navigate(Routes.ListingDetail.createRoute(listingId)) {
                            popUpTo(Routes.Sell.route) { inclusive = true }
                        }
                    }
                },
                onAskAi = {
                    navController.navigate(Routes.SmartDecision.route) { popUpTo(Routes.Sell.route) { inclusive = true } }
                },
            )
        }
        composable(Routes.VendorHome.route) {
            VendorHomeScreen(
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                onAddListing = { navController.navigate(Routes.Sell.createRoute()) },
                onEditListing = { navController.navigate(Routes.Sell.createRoute(it)) },
                onOpenExchange = { navController.navigate(Routes.ExchangeDetail.createRoute(it)) },
                onNewExchange = { navController.navigate(Routes.ExchangeNew.createRoute()) },
                onOpenRepair = { navController.navigate(Routes.RepairDetail.createRoute(it)) },
                onEditProfile = { navController.navigate(Routes.VendorProfileEdit.route) },
                onOpenReports = { navController.navigate(Routes.VendorReport.route) },
                onOpenChats = { navController.navigate(Routes.ChatList.route) },
                onOpenNotifications = { navController.navigate(Routes.Notifications.route) },
                onOpenBlocked = { navController.navigate(Routes.Blocked.route) },
                onLoggedOut = {
                    navController.navigate(Routes.Welcome.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
            )
        }
        composable(Routes.SmartDecision.route) {
            SmartDecisionScreen(
                onBackClick = { navController.popBackStack() },
                onOpenFlow = { target ->
                    val route = when (target) {
                        Recommendation.SELL, Recommendation.EXCHANGE -> Routes.Sell.createRoute()
                        Recommendation.REPAIR, Recommendation.RECYCLE -> Routes.RepairNew.route
                    }
                    navController.navigate(route) { popUpTo(Routes.SmartDecision.route) { inclusive = true } }
                },
            )
        }
        composable(Routes.SajjaChat.route) {
            SajjaChatScreen(
                onBackClick = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
            )
        }
        composable(Routes.Profile.route) {
            val homeViewModel: HomeViewModel = viewModel(factory = ViewModelFactory(container))
            Scaffold(
                topBar = { AppTopBar(title = "Profile", onBackClick = { navController.popBackStack() }) },
                bottomBar = { HubBottomBar(navController, container) },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding ->
                UserProfileScreen(
                    modifier = Modifier.padding(padding),
                    onOpenSaved = { navController.navigate(Routes.Saved.route) },
                    onOpenBlocked = { navController.navigate(Routes.Blocked.route) },
                    onLogout = {
                        homeViewModel.logout {
                            navController.navigate(Routes.Welcome.route) { popUpTo(0) { inclusive = true } }
                        }
                    },
                    onAccountDeleted = {
                        navController.navigate(Routes.Welcome.route) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
        }
        composable(
            route = Routes.UserProfile.route,
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType },
                navArgument("name") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val userId = entry.arguments?.getString("userId")
            val name = entry.arguments?.getString("name") ?: "this person"
            Scaffold(
                topBar = {
                    AppTopBar(
                        title = name,
                        onBackClick = { navController.popBackStack() },
                        actions = { TrustMenu(userId = userId, userName = name) },
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
            ) { padding -> UserProfileScreen(modifier = Modifier.padding(padding)) }
        }
        composable(Routes.Saved.route) {
            SavedScreen(
                onBackClick = { navController.popBackStack() },
                onOpenListing = { navController.navigate(Routes.ListingDetail.createRoute(it)) },
                bottomBar = { HubBottomBar(navController, container) },
            )
        }
        composable(Routes.Blocked.route) {
            BlockedUsersScreen(
                onBackClick = { navController.popBackStack() },
                bottomBar = { HubBottomBar(navController, container) },
            )
        }
        composable(Routes.ChatList.route) {
            ChatListScreen(
                onBackClick = { navController.popBackStack() },
                onOpenChat = { navController.navigate(Routes.ChatThread.createRoute(it)) },
                bottomBar = { HubBottomBar(navController, container) },
            )
        }
        composable(
            route = Routes.ChatThread.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType }),
        ) {
            ChatThreadScreen(
                onBackClick = { navController.popBackStack() },
                onOpenRoute = { navController.navigate(it) },
            )
        }
        composable(Routes.Notifications.route) {
            NotificationsScreen(
                onBackClick = { navController.popBackStack() },
                onOpenRoute = { navController.navigate(it) },
                bottomBar = { HubBottomBar(navController, container) },
            )
        }
        composable(Routes.VendorReport.route) {
            VendorReportScreen(onBackClick = { navController.popBackStack() })
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

        if (BuildConfig.DEBUG) {
            composable(Routes.ComponentPreview.route) { ComponentPreviewScreen() }
        }
    }
    }
}

/**
 * The bottom tab bar on the hub pages reached from a home screen's top bar (Chats, Notifications, Profile, Saved, Blocked), so the main
 * navigation never disappears there. Tapping a tab selects it and goes back to that home screen. Which space's tabs to show depends on
 * which home screen is below this page in the back stack.
 *
 * Deliberately NOT shown on screens with their own bottom action (listing detail, chat thread, Sajja AI) or that are full-screen flows
 * (sell, repair, recycle, exchange proposal, forms) or before sign-in.
 */
@Composable
private fun HubBottomBar(navController: NavHostController, container: AppContainer) {
    val isVendor = remember(navController) {
        runCatching { navController.getBackStackEntry(Routes.VendorHome.route) }.isSuccess
    }
    val homeRoute = if (isVendor) Routes.VendorHome.route else Routes.UserHome.route
    val backToHome = { navController.popBackStack(homeRoute, inclusive = false) }
    if (isVendor) {
        val isHindi = isAppHindi
        BottomTabBar(
            tabs = VendorTab.entries,
            selected = null,
            label = { it.localizedLabel(isHindi) },
            icon = { it.icon },
            onSelect = { tab ->
                container.homeTabs.vendor.value = tab
                backToHome()
            },
        )
    } else {
        BottomTabBar(
            tabs = UserTab.entries,
            selected = null,
            label = { it.label },
            icon = { it.icon },
            onSelect = { tab ->
                container.homeTabs.user.value = tab
                backToHome()
            },
        )
    }
}

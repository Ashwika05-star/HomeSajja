package com.homesajja.app.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Welcome : Routes("welcome")
    data object Login : Routes("login")
    data object Signup : Routes("signup")
    data object UserHome : Routes("user_home")
    data object VendorHome : Routes("vendor_home")
    data object ComponentPreview : Routes("component_preview")
}

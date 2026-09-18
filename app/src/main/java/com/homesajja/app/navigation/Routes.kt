package com.homesajja.app.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Welcome : Routes("welcome")
    data object Auth : Routes("auth")
    data object RoleSelection : Routes("role_selection")
    data object UserHome : Routes("user_home")
    data object VendorHome : Routes("vendor_home")
    data object ComponentPreview : Routes("component_preview")
}

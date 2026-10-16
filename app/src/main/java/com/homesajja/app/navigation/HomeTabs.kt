package com.homesajja.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow

/** The bottom-bar tabs of the user's space. */
enum class UserTab(val label: String, val icon: ImageVector) {
    EXPLORE("Explore", Icons.Filled.Explore),
    EXCHANGE("Exchange", Icons.Filled.SwapHoriz),
    MY_LISTINGS("My listings", Icons.Filled.Sell),
    MY_REQUESTS("Requests", Icons.AutoMirrored.Filled.ReceiptLong),
    SERVICES("Services", Icons.Filled.Build),
}

/** The bottom-bar tabs of the vendor's space. */
enum class VendorTab(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard),
    LISTINGS("Listings", Icons.Filled.Sell),
    REQUESTS("Requests", Icons.AutoMirrored.Filled.ReceiptLong),
    EXPLORE("Explore", Icons.Filled.Explore),
    PROFILE("Profile", Icons.Filled.Person),
    ;

    fun localizedLabel(isHindi: Boolean): String = if (isHindi) when (this) {
        DASHBOARD -> "डैशबोर्ड"
        LISTINGS -> "लिस्टिंग्स"
        REQUESTS -> "अनुरोध"
        EXPLORE -> "मार्केट"
        PROFILE -> "प्रोफाइल"
    } else label
}

/**
 * Which tab each space is on. It lives outside the screens so the bottom bar can also be shown on the hub pages
 * (Chats, Notifications, Profile...): tapping a tab there selects it here and returns to the home screen on that tab.
 */
class HomeTabState {
    val user = MutableStateFlow(UserTab.EXPLORE)
    val vendor = MutableStateFlow(VendorTab.DASHBOARD)

    /** Back to the first tab of each space, e.g. when a different person logs in. */
    fun reset() {
        user.value = UserTab.EXPLORE
        vendor.value = VendorTab.DASHBOARD
    }
}

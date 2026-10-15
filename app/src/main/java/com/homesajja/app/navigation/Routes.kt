package com.homesajja.app.navigation

sealed class Routes(val route: String) {
    data object Splash : Routes("splash")
    data object Welcome : Routes("welcome")
    data object Login : Routes("login")
    data object Signup : Routes("signup")
    data object UserHome : Routes("user_home")
    data object VendorHome : Routes("vendor_home")
    data object ComponentPreview : Routes("component_preview")

    data object ListingDetail : Routes("listing/{listingId}") {
        fun createRoute(listingId: String) = "listing/$listingId"
    }

    /** Propose an exchange; with a requestedListingId the wanted item is already chosen; with offeredListingId the offered item is already chosen. */
    data object ExchangeNew : Routes("exchange_new?requestedListingId={requestedListingId}&offeredListingId={offeredListingId}") {
        fun createRoute(requestedListingId: String? = null, offeredListingId: String? = null): String {
            val params = mutableListOf<String>()
            requestedListingId?.let { params.add("requestedListingId=$it") }
            offeredListingId?.let { params.add("offeredListingId=$it") }
            return if (params.isEmpty()) "exchange_new" else "exchange_new?${params.joinToString("&")}"
        }
    }

    data object ExchangeDetail : Routes("exchange/{requestId}") {
        fun createRoute(requestId: String) = "exchange/$requestId"
    }

    data object RepairNew : Routes("repair_new")

    data object RepairDetail : Routes("repair/{requestId}") {
        fun createRoute(requestId: String) = "repair/$requestId"
    }

    data object RecycleNew : Routes("recycle_new")

    data object RecycleDetail : Routes("recycling/{requestId}") {
        fun createRoute(requestId: String) = "recycling/$requestId"
    }

    /** The signed-in person's own profile (saved furniture, blocked people, reviews about them). */
    data object Profile : Routes("profile")

    /** Someone else's profile: their name (their account is private) and the reviews about them. */
    data object UserProfile : Routes("user/{userId}?name={name}") {
        fun createRoute(userId: String, name: String) = "user/$userId?name=${android.net.Uri.encode(name)}"
    }

    data object SmartDecision : Routes("smart_decision")

    data object SajjaChat : Routes("sajja_chat")

    data object Saved : Routes("saved")

    data object Blocked : Routes("blocked")

    data object ChatList : Routes("chats")

    data object ChatThread : Routes("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }

    data object Notifications : Routes("notifications")

    /** A vendor's public page. */
    data object VendorProfile : Routes("vendor/{vendorId}") {
        fun createRoute(vendorId: String) = "vendor/$vendorId"
    }

    data object VendorProfileEdit : Routes("vendor_profile_edit")
    data object VendorReport : Routes("vendor_report")

    /** Post a material request; with a requestId it edits that request instead. */
    data object MaterialForm : Routes("material_form?requestId={requestId}") {
        fun createRoute(requestId: String? = null) = if (requestId == null) "material_form" else "material_form?requestId=$requestId"
    }

    data object MaterialDetail : Routes("material/{requestId}") {
        fun createRoute(requestId: String) = "material/$requestId"
    }

    /** Sell flow; with a listingId it edits that listing instead; with actionType/returnToExchange it tailors the flow for exchange. */
    data object Sell : Routes("sell?listingId={listingId}&actionType={actionType}&returnToExchange={returnToExchange}&requestedListingId={requestedListingId}") {
        fun createRoute(
            listingId: String? = null,
            actionType: String? = null,
            returnToExchange: Boolean = false,
            requestedListingId: String? = null,
        ): String {
            val params = mutableListOf<String>()
            listingId?.let { params.add("listingId=$it") }
            actionType?.let { params.add("actionType=$it") }
            if (returnToExchange) params.add("returnToExchange=true")
            requestedListingId?.let { params.add("requestedListingId=$it") }
            return if (params.isEmpty()) "sell" else "sell?${params.joinToString("&")}"
        }
    }
}

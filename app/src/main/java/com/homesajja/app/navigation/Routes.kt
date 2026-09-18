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

    /** Propose an exchange; with a requestedListingId the wanted item is already chosen. */
    data object ExchangeNew : Routes("exchange_new?requestedListingId={requestedListingId}") {
        fun createRoute(requestedListingId: String? = null) =
            if (requestedListingId == null) "exchange_new" else "exchange_new?requestedListingId=$requestedListingId"
    }

    data object ExchangeDetail : Routes("exchange/{requestId}") {
        fun createRoute(requestId: String) = "exchange/$requestId"
    }

    /** Sell flow; with a listingId it edits that listing instead. */
    data object Sell : Routes("sell?listingId={listingId}") {
        fun createRoute(listingId: String? = null) = if (listingId == null) "sell" else "sell?listingId=$listingId"
    }
}

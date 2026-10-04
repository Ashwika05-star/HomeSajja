package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.payment.confirmedEarnings
import com.homesajja.app.payment.receivable
import com.homesajja.app.repository.VendorInbox

/** Which request system an activity item came from. */
enum class ActivityKind(val label: String) {
    PURCHASE("Purchase request"),
    EXCHANGE("Exchange request"),
    REPAIR("Repair request"),
    RECYCLING("Recycling request"),
}

/** One line of the recent-activity feed. [requestId] is what to open when it is tapped. */
data class ActivityItem(
    val kind: ActivityKind,
    val requestId: String,
    val title: String,
    val statusLabel: String,
    val timestamp: Long,
)

data class DashboardStats(
    val activeListings: Int,
    val pendingRequests: Int,
    val completedSales: Int,
    /** Money actually earned, in rupees: only payments the vendor has confirmed as received (see [confirmedEarnings]). */
    val earned: Long,
)

/** [profileIncomplete] nudges the vendor to finish their profile (description and a shop location). */
data class DashboardData(
    val vendor: VendorProfile,
    val stats: DashboardStats,
    val activity: List<ActivityItem>,
    val profileIncomplete: Boolean,
)

const val RECENT_ACTIVITY_COUNT = 5

/**
 * Turns a vendor's listings and inbox into what the dashboard shows.
 * - Active listings: listings that are ACTIVE (reserved, sold or hidden ones don't count).
 * - Pending requests: purchase, exchange, repair and recycling requests waiting for the vendor: not answered yet, or (repair and
 *   recycling) a quote the customer declined, which needs a revised quote or closing.
 * - Completed sales: purchase requests that reached COMPLETED.
 * - Earned: the agreed amount of every request whose payment the vendor confirmed as received. A payer's "I've paid" doesn't count
 *   until the vendor confirms it, and money the vendor paid out themselves is not earnings.
 */
fun buildDashboard(vendor: VendorProfile, listings: List<FurnitureListing>, inbox: VendorInbox): DashboardData {
    val pending = inbox.purchases.count { it.status == PurchaseStatus.REQUESTED } +
        inbox.exchanges.count { it.status == ExchangeStatus.PENDING } +
        inbox.repairs.count { it.status == RepairStatus.REQUESTED || it.status == RepairStatus.DECLINED } +
        inbox.recycling.count { it.status == RecyclingStatus.REQUESTED || it.status == RecyclingStatus.DECLINED }

    val activity = buildList {
        inbox.purchases.forEach {
            add(ActivityItem(ActivityKind.PURCHASE, it.id, "${it.buyerName} · ${it.listingTitle}", it.status.displayName, it.updatedAt))
        }
        inbox.exchanges.forEach {
            add(ActivityItem(ActivityKind.EXCHANGE, it.id, "${it.senderName} offers ${it.offeredTitle}", it.status.displayName, it.updatedAt))
        }
        inbox.repairs.forEach {
            add(ActivityItem(ActivityKind.REPAIR, it.id, "${it.userName} · ${it.furnitureTitle}", it.status.displayName, it.updatedAt))
        }
        inbox.recycling.forEach {
            add(ActivityItem(ActivityKind.RECYCLING, it.id, "${it.userName} · ${it.material.displayName} furniture", it.status.displayName, it.updatedAt))
        }
    }.sortedByDescending { it.timestamp }.take(RECENT_ACTIVITY_COUNT)

    return DashboardData(
        vendor = vendor,
        stats = DashboardStats(
            activeListings = listings.count { it.status == ListingStatus.ACTIVE },
            pendingRequests = pending,
            completedSales = inbox.purchases.count { it.status == PurchaseStatus.COMPLETED },
            earned = confirmedEarnings(
                vendor.uid,
                inbox.purchases.map { it.receivable() } + inbox.repairs.map { it.receivable() } + inbox.recycling.map { it.receivable() },
            ),
        ),
        activity = activity,
        profileIncomplete = vendor.description.isBlank() || vendor.shopLatitude == null || vendor.shopLongitude == null,
    )
}

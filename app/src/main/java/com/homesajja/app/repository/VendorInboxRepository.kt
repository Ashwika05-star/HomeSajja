package com.homesajja.app.repository

import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RepairRequest
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Everything currently addressed to one vendor, across the four request systems. */
data class VendorInbox(
    val purchases: List<PurchaseRequest> = emptyList(),
    val exchanges: List<ExchangeRequest> = emptyList(),
    val repairs: List<RepairRequest> = emptyList(),
    val recycling: List<RecyclingRequest> = emptyList(),
)

/**
 * Read-only view over the four separate request repositories, so the dashboard can count and list
 * what is waiting for a vendor. It does not merge the systems: each list keeps its own type and
 * status pipeline, and acting on a request still goes through that system's own repository.
 */
class VendorInboxRepository(
    private val purchases: PurchaseRequestRepository,
    private val exchanges: ExchangeRepository,
    private val repairs: RepairRepository,
    private val recycling: RecyclingRepository,
) {
    suspend fun load(vendorId: String): VendorInbox = coroutineScope {
        val purchaseList = async { purchases.getRequestsBySeller(vendorId) }
        val exchangeList = async { exchanges.getRequestsByReceiver(vendorId) }
        val repairList = async { repairs.getRequestsByVendor(vendorId) }
        val recyclingList = async { recycling.getRequestsByVendor(vendorId) }
        VendorInbox(purchaseList.await(), exchangeList.await(), repairList.await(), recyclingList.await())
    }
}

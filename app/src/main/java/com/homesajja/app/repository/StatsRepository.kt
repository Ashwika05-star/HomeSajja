package com.homesajja.app.repository

import com.google.firebase.firestore.AggregateField
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.ExchangeStatus
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.PurchaseStatus
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.data.model.RecyclingStatus
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.data.model.RepairStatus
import com.homesajja.app.viewmodel.EarnedItem
import com.homesajja.app.viewmodel.TaskCount
import com.homesajja.app.viewmodel.UserTaskCounts
import com.homesajja.app.viewmodel.VendorTaskCounts
import com.homesajja.app.viewmodel.toEarnedItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

/** The most confirmed payments one load reads from each collection. A vendor with more sees the most recent ones (and is told so). */
const val EARNED_FETCH_LIMIT = 300

/** Confirmed payments read for one person, and whether some older ones were left out because of [EARNED_FETCH_LIMIT]. */
data class EarnedBatch(val items: List<EarnedItem>, val isPartial: Boolean)

/**
 * Read-only numbers for the vendor dashboard, the monthly report and the profile summary. It is built to be cheap on Firestore's free plan:
 *
 * - Counts use `count()` aggregation queries, which bill one read per 1,000 matching documents (at least one per query), instead of reading
 *   the documents.
 * - Money is read only from payments that are already confirmed (`payment.status == CONFIRMED`), newest first, at most [EARNED_FETCH_LIMIT]
 *   per collection; a monthly report adds a `payment.confirmedAt` range so it reads that month alone. These need the composite indexes in
 *   firestore.indexes.json.
 * - The rating is one `average` + `count` aggregation over the person's reviews.
 *
 * Every query names the signed-in person (`sellerId`, `vendorId`, `userId`, ...), which is what firestore.rules require to allow it.
 */
class StatsRepository(private val firestore: FirebaseFirestore) {

    private val purchases = firestore.collection("purchaseRequests")
    private val exchanges = firestore.collection("exchangeRequests")
    private val repairs = firestore.collection("repairRequests")
    private val recycling = firestore.collection("recyclingRequests")
    private val listings = firestore.collection("listings")
    private val reviews = firestore.collection("reviews")

    private suspend fun Query.countServer(): Int = count().get(AggregateSource.SERVER).await().count.toInt()

    private fun Query.statusIn(statuses: List<String>) = whereIn("status", statuses)

    /** Listings the vendor has on sale right now. */
    suspend fun activeListings(ownerId: String): Int =
        listings.whereEqualTo("ownerId", ownerId).whereEqualTo("status", ListingStatus.ACTIVE.name).countServer()

    /** Completed and still-open requests of every kind the vendor is part of: ten small count queries, run together. */
    suspend fun vendorTaskCounts(vendorId: String): VendorTaskCounts = coroutineScope {
        val sales = async {
            val base = purchases.whereEqualTo("sellerId", vendorId)
            TaskCount(base.whereEqualTo("status", PurchaseStatus.COMPLETED.name).countServer(), base.statusIn(OPEN_PURCHASE).countServer())
        }
        val repairCounts = async {
            val base = repairs.whereEqualTo("vendorId", vendorId)
            TaskCount(base.whereEqualTo("status", RepairStatus.COMPLETED.name).countServer(), base.statusIn(OPEN_REPAIR).countServer())
        }
        val recyclingCounts = async {
            val base = recycling.whereEqualTo("vendorId", vendorId)
            TaskCount(base.whereEqualTo("status", RecyclingStatus.COMPLETED.name).countServer(), base.statusIn(OPEN_RECYCLING).countServer())
        }
        // A vendor can be either side of an exchange.
        val exchangeCounts = async {
            val asSender = exchanges.whereEqualTo("senderId", vendorId)
            val asReceiver = exchanges.whereEqualTo("receiverId", vendorId)
            val completed = listOf(asSender, asReceiver).map { async { it.whereEqualTo("status", ExchangeStatus.COMPLETED.name).countServer() } }
            val pending = listOf(asSender, asReceiver).map { async { it.statusIn(OPEN_EXCHANGE).countServer() } }
            TaskCount(completed.sumOf { it.await() }, pending.sumOf { it.await() })
        }
        VendorTaskCounts(sales.await(), repairCounts.await(), exchangeCounts.await(), recyclingCounts.await())
    }

    /** What a person has completed, for the profile's earnings and sustainability cards. */
    suspend fun userTaskCounts(userId: String): UserTaskCounts = coroutineScope {
        val done = ExchangeStatus.COMPLETED.name
        val sold = async { purchases.whereEqualTo("sellerId", userId).whereEqualTo("status", PurchaseStatus.COMPLETED.name).countServer() }
        val bought = async { purchases.whereEqualTo("buyerId", userId).whereEqualTo("status", PurchaseStatus.COMPLETED.name).countServer() }
        val sent = async { exchanges.whereEqualTo("senderId", userId).whereEqualTo("status", done).countServer() }
        val received = async { exchanges.whereEqualTo("receiverId", userId).whereEqualTo("status", done).countServer() }
        val repaired = async { repairs.whereEqualTo("userId", userId).whereEqualTo("status", RepairStatus.COMPLETED.name).countServer() }
        val recycled = async { recycling.whereEqualTo("userId", userId).whereEqualTo("status", RecyclingStatus.COMPLETED.name).countServer() }
        UserTaskCounts(sold.await(), bought.await(), sent.await() + received.await(), repaired.await(), recycled.await())
    }

    /** The money a vendor has received (confirmed, not refunded) from sales, repairs and recycling jobs. [from] and [until] limit it to a time window. */
    suspend fun vendorEarned(vendorId: String, from: Long? = null, until: Long? = null): EarnedBatch = coroutineScope {
        val sales = async { confirmed(purchases.whereEqualTo("sellerId", vendorId), from, until).getAllAs<PurchaseRequest>() }
        val repairList = async { confirmed(repairs.whereEqualTo("vendorId", vendorId), from, until).getAllAs<RepairRequest>() }
        // A recycler is only "earning" when the customer paid them; a payout to a customer has them as the payer.
        val recyclingList = async {
            confirmed(recycling.whereEqualTo("vendorId", vendorId).whereEqualTo("payment.payeeId", vendorId), from, until).getAllAs<RecyclingRequest>()
        }
        val s = sales.await()
        val r = repairList.await()
        val c = recyclingList.await()
        EarnedBatch(
            items = (s.mapNotNull { it.toEarnedItem(vendorId) } + r.mapNotNull { it.toEarnedItem(vendorId) } + c.mapNotNull { it.toEarnedItem(vendorId) })
                .sortedByDescending { it.confirmedAt },
            isPartial = listOf(s.size, r.size, c.size).any { it >= EARNED_FETCH_LIMIT },
        )
    }

    /** The money a person has received from selling their own things. */
    suspend fun userEarned(userId: String): EarnedBatch {
        val sales = confirmed(purchases.whereEqualTo("sellerId", userId), null, null).getAllAs<PurchaseRequest>()
        return EarnedBatch(sales.mapNotNull { it.toEarnedItem(userId) }, isPartial = sales.size >= EARNED_FETCH_LIMIT)
    }

    /** The person's average rating and review count, from one aggregation query (no review documents are read). */
    suspend fun rating(personId: String): RatingSummary {
        val average = AggregateField.average("rating")
        val count = AggregateField.count()
        val snapshot = reviews.whereEqualTo("targetUserId", personId).aggregate(average, count).get(AggregateSource.SERVER).await()
        val reviewCount = snapshot.getLong(count)?.toInt() ?: 0
        return if (reviewCount == 0) RatingSummary() else RatingSummary(snapshot.getDouble(average) ?: 0.0, reviewCount)
    }

    /** Payments with status CONFIRMED, newest first, within [from, until) when given. Needs the `payment.confirmedAt` composite indexes. */
    private fun confirmed(base: Query, from: Long?, until: Long?): Query {
        var query = base.whereEqualTo("payment.status", PaymentStatus.CONFIRMED.name)
        from?.let { query = query.whereGreaterThanOrEqualTo("payment.confirmedAt", it) }
        until?.let { query = query.whereLessThan("payment.confirmedAt", it) }
        return query.orderBy("payment.confirmedAt", Query.Direction.DESCENDING).limit(EARNED_FETCH_LIMIT.toLong())
    }

    private companion object {
        val OPEN_PURCHASE = listOf(PurchaseStatus.REQUESTED, PurchaseStatus.ACCEPTED, PurchaseStatus.READY_FOR_PICKUP).map { it.name }
        val OPEN_REPAIR = listOf(
            RepairStatus.REQUESTED, RepairStatus.QUOTED, RepairStatus.DECLINED, RepairStatus.AGREED, RepairStatus.ACCEPTED,
            RepairStatus.IN_PROGRESS, RepairStatus.READY,
        ).map { it.name }
        val OPEN_RECYCLING = listOf(
            RecyclingStatus.REQUESTED, RecyclingStatus.QUOTED, RecyclingStatus.DECLINED, RecyclingStatus.ACCEPTED, RecyclingStatus.SCHEDULED,
        ).map { it.name }
        val OPEN_EXCHANGE = listOf(ExchangeStatus.PENDING, ExchangeStatus.ACCEPTED).map { it.name }
    }
}

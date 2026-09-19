package com.homesajja.app.data.model

/** Average rating and how many reviews it is based on. [count] 0 means "no ratings yet". */
data class RatingSummary(val average: Double = 0.0, val count: Int = 0) {
    val hasRatings: Boolean get() = count > 0

    companion object {
        fun of(reviews: List<Review>): RatingSummary =
            if (reviews.isEmpty()) RatingSummary() else RatingSummary(reviews.map { it.rating }.average(), reviews.size)
    }
}

/** What a report is about. */
enum class ReportTarget(val displayName: String) {
    LISTING("Listing"),
    USER("User"),
}

enum class ReportReason(val displayName: String) {
    SPAM("Spam or misleading"),
    SCAM("Looks like a scam"),
    INAPPROPRIATE("Inappropriate content"),
    WRONG_INFO("Wrong or fake details"),
    OTHER("Something else"),
}

/** Stored at `reports/{reporterId_targetType_targetId}`: one report per person per target. Nothing acts on it
 * automatically; whoever runs HomeSajja reads them in the Firebase console. */
data class Report(
    val id: String = "",
    val reporterId: String = "",
    val targetType: ReportTarget = ReportTarget.LISTING,
    val targetId: String = "",
    val targetName: String = "",
    val reason: ReportReason = ReportReason.OTHER,
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** Stored at `blocks/{blockerId_blockedId}`. While it exists, neither person can chat with or send requests to the other. */
data class Block(
    val id: String = "",
    val blockerId: String = "",
    val blockedId: String = "",
    val blockedName: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

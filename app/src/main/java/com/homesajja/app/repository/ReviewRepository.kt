package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Review
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "reviews"

/** Reviews at `reviews/{reviewerId_contextType_contextId}` — the deterministic
 * id means creating a second review for the same transaction just fails. */
class ReviewRepository(firestore: FirebaseFirestore) {

    private val reviews = firestore.collection(COLLECTION)

    suspend fun createReview(review: Review): Review {
        val id = FirestoreIds.reviewId(review.reviewerId, review.contextType, review.contextId)
        val saved = review.copy(id = id)
        reviews.document(id).set(saved).await()
        return saved
    }

    suspend fun getReview(id: String): Review? = reviews.document(id).getAs()

    /** Reviews written about a vendor or user, newest first. */
    suspend fun getReviewsForTarget(targetUserId: String, limit: Int = DEFAULT_PAGE_SIZE): List<Review> =
        reviews.whereEqualTo("targetUserId", targetUserId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    suspend fun getReviewsByReviewer(reviewerId: String, limit: Int = DEFAULT_PAGE_SIZE): List<Review> =
        reviews.whereEqualTo("reviewerId", reviewerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()

    /** Only the rating and comment can change after posting. */
    suspend fun updateReview(id: String, rating: Int, comment: String) {
        reviews.document(id)
            .update(mapOf("rating" to rating, "comment" to comment, "updatedAt" to System.currentTimeMillis()))
            .await()
    }

    suspend fun deleteReview(id: String) {
        reviews.document(id).delete().await()
    }
}

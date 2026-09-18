package com.homesajja.app.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.homesajja.app.data.model.Favourite
import kotlinx.coroutines.tasks.await

private const val COLLECTION = "favourites"

/** Saved listings at `favourites/{userId_listingId}`. A favourite has nothing
 * to edit, so it is only added, read and removed. */
class FavouriteRepository(firestore: FirebaseFirestore) {

    private val favourites = firestore.collection(COLLECTION)

    suspend fun addFavourite(userId: String, listingId: String): Favourite {
        val favourite = Favourite(id = FirestoreIds.favouriteId(userId, listingId), userId = userId, listingId = listingId)
        favourites.document(favourite.id).set(favourite).await()
        return favourite
    }

    suspend fun removeFavourite(userId: String, listingId: String) {
        favourites.document(FirestoreIds.favouriteId(userId, listingId)).delete().await()
    }

    suspend fun isFavourite(userId: String, listingId: String): Boolean =
        favourites.document(FirestoreIds.favouriteId(userId, listingId)).get().await().exists()

    /** The user's saved listings, most recently saved first. */
    suspend fun getFavourites(userId: String, limit: Int = DEFAULT_PAGE_SIZE): List<Favourite> =
        favourites.whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .getAllAs()
}

package com.homesajja.app.repository

import android.content.ContentResolver
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.net.URLDecoder
import java.util.UUID

/** Listing photos in Firebase Storage at `listings/{ownerId}/{listingId}/{random}`. */
class StorageRepository(
    private val storage: FirebaseStorage,
    private val contentResolver: ContentResolver,
) {

    /** Uploads one picked image and returns its download URL. */
    suspend fun uploadListingImage(ownerId: String, listingId: String, image: Uri): String {
        val ref = storage.reference.child("listings/$ownerId/$listingId/${UUID.randomUUID()}")
        val metadata = StorageMetadata.Builder()
            .setContentType(contentResolver.getType(image) ?: "image/jpeg")
            .build()
        ref.putFile(image, metadata).await()
        return ref.downloadUrl.await().toString()
    }

    /** Best-effort cleanup: a photo that is already gone must not fail the caller. */
    suspend fun deleteImages(urls: List<String>) {
        urls.mapNotNull(::storagePathFromDownloadUrl).forEach { path ->
            runCatching { storage.reference.child(path).delete().await() }
        }
    }
}

/** Pulls the object path out of a Storage download URL
 * (`…/v0/b/{bucket}/o/{url-encoded path}?alt=media&token=…`). Works for production and
 * emulator hosts alike, which `getReferenceFromUrl` does not. Null for non-Storage URLs. */
internal fun storagePathFromDownloadUrl(url: String): String? {
    val encodedPath = url.substringAfter("/o/", missingDelimiterValue = "").substringBefore('?')
    return encodedPath.takeIf { it.isNotEmpty() }?.let { URLDecoder.decode(it, "UTF-8") }
}

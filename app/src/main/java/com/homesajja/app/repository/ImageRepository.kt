package com.homesajja.app.repository

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/** Thrown when a photo can't be prepared or uploaded. */
class ImageUploadException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Listing photos live in Cloudinary (free tier), not Firebase Storage, which needs a paid plan.
 * Uploads use an *unsigned preset*, so no secret ships in the app. The trade-off: unsigned
 * uploads cannot be deleted from the client, so removed photos simply stop being referenced.
 */
class ImageRepository(
    private val contentResolver: ContentResolver,
    private val cloudName: String,
    private val uploadPreset: String,
) {

    suspend fun uploadListingImage(ownerId: String, listingId: String, image: Uri): String =
        upload("listings/$ownerId/$listingId", image)

    suspend fun uploadRepairImage(ownerId: String, requestId: String, image: Uri): String =
        upload("repairs/$ownerId/$requestId", image)

    suspend fun uploadRecyclingImage(ownerId: String, requestId: String, image: Uri): String =
        upload("recycling/$ownerId/$requestId", image)

    suspend fun uploadVendorImage(vendorId: String, image: Uri): String =
        upload("vendors/$vendorId", image)

    /** Downsizes the picked image, uploads it into [folder] and returns its HTTPS URL. */
    private suspend fun upload(folder: String, image: Uri): String =
        withContext(Dispatchers.IO) {
            val jpeg = compress(image)
            val boundary = "hs-${UUID.randomUUID()}"
            val connection = (URL("https://api.cloudinary.com/v1_1/$cloudName/image/upload").openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
                connection.outputStream.use { out ->
                    fun field(name: String, value: String) = out.write(
                        "--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n$value\r\n".toByteArray(),
                    )
                    field("upload_preset", uploadPreset)
                    field("folder", folder)
                    out.write(
                        ("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"photo.jpg\"\r\n" +
                            "Content-Type: image/jpeg\r\n\r\n").toByteArray(),
                    )
                    out.write(jpeg)
                    out.write("\r\n--$boundary--\r\n".toByteArray())
                }
                val code = connection.responseCode
                val body = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) throw ImageUploadException("Upload failed ($code)")
                extractSecureUrl(body) ?: throw ImageUploadException("Upload response had no image URL")
            } catch (e: ImageUploadException) {
                throw e
            } catch (e: IOException) {
                throw ImageUploadException("Upload failed. Check your connection.", e)
            } finally {
                connection.disconnect()
            }
        }

    private fun compress(image: Uri): ByteArray {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, MAX_EDGE_PX)
        }
        val bitmap = contentResolver.openInputStream(image)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw ImageUploadException("Couldn't read the selected photo")
        return ByteArrayOutputStream().also {
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it)
            bitmap.recycle()
        }.toByteArray()
    }

    private companion object {
        const val MAX_EDGE_PX = 1600
        const val JPEG_QUALITY = 85
        const val TIMEOUT_MS = 30_000
    }
}

/** Largest power-of-two divisor that keeps the longer edge at or above [maxEdge]. */
internal fun sampleSizeFor(width: Int, height: Int, maxEdge: Int): Int {
    var sample = 1
    val longest = maxOf(width, height)
    while (longest / (sample * 2) >= maxEdge) sample *= 2
    return sample
}

private val SECURE_URL = Regex("\"secure_url\"\\s*:\\s*\"([^\"]+)\"")

/** Reads `secure_url` out of Cloudinary's JSON reply (which escapes `/` as `\/`). */
internal fun extractSecureUrl(json: String): String? =
    SECURE_URL.find(json)?.groupValues?.get(1)?.replace("\\/", "/")

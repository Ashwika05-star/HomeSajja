package com.homesajja.app.ui.util

/** Widths (in pixels) images are asked for, so a small card doesn't download a full-size photo. */
object ImageWidth {
    const val THUMBNAIL = 320
    const val CARD = 560
    const val FULL = 1080
}

private val CLOUDINARY_UPLOAD = Regex("^(https://res\\.cloudinary\\.com/[^/]+/image/upload/)(v\\d+/.+)$")

/**
 * Listing photos live on Cloudinary, which can shrink and compress a picture on the fly. This asks for a version that is at
 * most [width] wide, in the best format the phone supports, so lists load fast and use little data. Anything that isn't a
 * plain Cloudinary photo URL (a local picked photo, another host) is returned unchanged.
 */
fun optimizedImage(model: Any?, width: Int): Any? {
    val url = model as? String ?: return model
    val match = CLOUDINARY_UPLOAD.matchEntire(url) ?: return url
    val (prefix, rest) = match.destructured
    return "${prefix}w_$width,c_limit,q_auto,f_auto/$rest"
}

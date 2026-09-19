package com.homesajja.app.payment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.util.Locale

/** What happened when we tried to hand the payment over to a UPI app. */
enum class UpiLaunchResult {
    /** Google Pay (or another UPI app) opened; the person finishes paying there. */
    OPENED,

    /** No app on this phone can take a UPI payment. */
    NO_UPI_APP,
}

/**
 * UPI payments without a payment SDK: HomeSajja builds a standard `upi://pay` link and opens it, and the person
 * completes and confirms the payment inside Google Pay themselves. Nothing here can tell whether the payment
 * went through, so the buyer (or seller) marks the request as paid afterwards.
 */
object UpiPayment {

    const val GPAY_PACKAGE = "com.google.android.apps.nbu.paisa.user"

    private val UPI_ID = Regex("^[A-Za-z0-9._-]{2,256}@[A-Za-z]{2,64}$")

    /** A UPI id looks like `name@bank`. This only checks the shape, not that the account exists. */
    fun isValidUpiId(value: String): Boolean = UPI_ID.matches(value.trim())

    /** `upi://pay?pa=…&pn=…&am=…&cu=INR&tn=…`, with the amount in rupees to two decimals. */
    fun buildLink(upiId: String, payeeName: String, amountRupees: Long, note: String): Uri =
        Uri.parse(buildLinkString(upiId, payeeName, amountRupees, note))

    /** The link as text (separate from [buildLink] so it can be unit-tested without Android). */
    fun buildLinkString(upiId: String, payeeName: String, amountRupees: Long, note: String): String {
        val params = listOf(
            "pa" to upiId.trim(),
            "pn" to payeeName.ifBlank { "HomeSajja seller" },
            "am" to String.format(Locale.US, "%d.00", amountRupees),
            "cu" to "INR",
            "tn" to note.take(50),
        )
        return "upi://pay?" + params.joinToString("&") { (key, value) -> "$key=${encode(value)}" }
    }

    /** UPI apps expect spaces as %20, not the "+" that URLEncoder uses. */
    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    /** Opens Google Pay if it is installed, otherwise any UPI app the person picks; reports if there is none. */
    fun launch(context: Context, link: Uri): UpiLaunchResult {
        val gpay = Intent(Intent.ACTION_VIEW, link).setPackage(GPAY_PACKAGE)
        if (tryStart(context, gpay)) return UpiLaunchResult.OPENED
        val anyUpiApp = Intent.createChooser(Intent(Intent.ACTION_VIEW, link), "Pay with")
        return if (Intent(Intent.ACTION_VIEW, link).resolveActivity(context.packageManager) != null && tryStart(context, anyUpiApp)) {
            UpiLaunchResult.OPENED
        } else {
            UpiLaunchResult.NO_UPI_APP
        }
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

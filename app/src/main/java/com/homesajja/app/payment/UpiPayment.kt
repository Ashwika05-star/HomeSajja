package com.homesajja.app.payment

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import java.util.Locale

/** What happened when we tried to hand the payment over to a UPI app. */
enum class UpiLaunchResult {
    /** The chooser of UPI apps opened; the person picks one and finishes paying there. */
    OPENED,

    /** No app on this phone can take a UPI payment. */
    NO_UPI_APP,
}

/**
 * UPI payments without a payment SDK: HomeSajja builds a standard `upi://pay` link and opens it, and the person
 * completes the payment inside whichever UPI app they pick (Google Pay, PhonePe, Paytm, BHIM, a bank app...). Nothing here can
 * tell whether the payment went through, so the payer marks it paid and the payee confirms it afterwards.
 */
object UpiPayment {

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

    /** Opens the system chooser listing every UPI app on the phone; reports [UpiLaunchResult.NO_UPI_APP] if there is none. */
    fun launch(context: Context, link: Uri): UpiLaunchResult {
        val view = Intent(Intent.ACTION_VIEW, link)
        if (context.packageManager.queryIntentActivities(view, 0).isEmpty()) return UpiLaunchResult.NO_UPI_APP
        return if (tryStart(context, Intent.createChooser(view, "Pay with a UPI app"))) UpiLaunchResult.OPENED else UpiLaunchResult.NO_UPI_APP
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }
}

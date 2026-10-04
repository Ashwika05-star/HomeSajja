package com.homesajja.app.repository

import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus

/**
 * The field updates for the two payment steps, shared by purchase, repair and recycling requests. They name nested fields
 * (`payment.status`, ...) so everything else in the payment record, and the agreed amount, stays untouched; firestore.rules
 * accepts exactly these fields from exactly the right person.
 */
object PaymentWrites {

    /** The payer says they paid. [upiRef] (the UPI transaction reference) is optional and only kept for UPI payments. */
    fun markPaid(method: PaymentMethod, upiRef: String?, now: Long): Map<String, Any?> = mapOf(
        "payment.status" to PaymentStatus.MARKED_PAID.name,
        "payment.method" to method.name,
        "payment.upiRef" to upiRef?.trim()?.takeIf { it.isNotEmpty() && method == PaymentMethod.UPI },
        "payment.markedPaidAt" to now,
        "updatedAt" to now,
    )

    /** The payee says the money arrived. Only this counts as money earned. */
    fun confirmReceived(now: Long): Map<String, Any?> = mapOf(
        "payment.status" to PaymentStatus.CONFIRMED.name,
        "payment.confirmedAt" to now,
        "updatedAt" to now,
    )
}

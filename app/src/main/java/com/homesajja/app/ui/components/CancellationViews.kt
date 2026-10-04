package com.homesajja.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.CancelContext
import com.homesajja.app.data.model.CancelReason
import com.homesajja.app.data.model.Cancellation
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.Refund
import com.homesajja.app.data.model.RefundStatus
import com.homesajja.app.payment.MAX_CANCEL_NOTE_LENGTH
import com.homesajja.app.payment.cancelError
import com.homesajja.app.payment.refundStatusFor
import com.homesajja.app.ui.util.formatPrice

/**
 * A vendor cancelling a request they had accepted, in two steps: a form (a reason from [context]'s short list, an optional note, and, when
 * [refundAmount] is set, a required "I've refunded it" tick), then a confirmation that repeats what [otherName] will be told.
 * [refundAmount] is non-null only when the vendor had confirmed receiving payment: the cancel can't go ahead until they mark the refund as done.
 */
@Composable
fun VendorCancelDialog(
    context: CancelContext,
    title: String,
    otherName: String,
    refundAmount: Long?,
    onConfirm: (reason: CancelReason, note: String, refundDone: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var reason by remember { mutableStateOf<CancelReason?>(null) }
    var note by remember { mutableStateOf("") }
    var refundDone by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        val chosen = reason ?: return
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Cancel for good?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$otherName will be told: ${chosen.displayName}.${note.trim().takeIf { it.isNotEmpty() }?.let { " \"$it\"" }.orEmpty()}")
                    if (refundAmount != null) Text("You are marking ${formatPrice(refundAmount)} as refunded to $otherName.")
                    Text("This can't be undone.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = { TextButton(onClick = { onConfirm(chosen, note, refundDone) }) { Text("Yes, cancel", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Go back") } },
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Why are you cancelling? $otherName will be told.", style = MaterialTheme.typography.bodyMedium)
                context.reasons.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = reason == option, role = Role.RadioButton, onClick = { reason = option; error = null }),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == option, onClick = null, modifier = Modifier.padding(end = 12.dp))
                        Text(option.displayName, style = MaterialTheme.typography.bodyLarge)
                    }
                }
                AppTextField(
                    value = note,
                    onValueChange = { note = it.take(MAX_CANCEL_NOTE_LENGTH); error = null },
                    label = "Note (optional)",
                    singleLine = false,
                    minLines = 2,
                )
                if (refundAmount != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { refundDone = !refundDone; error = null },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = refundDone, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
                        Text(
                            "I've refunded ${formatPrice(refundAmount)} to $otherName. You confirmed receiving their payment, so this is required.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = cancelError(reason, note, context)
                when {
                    problem != null -> error = problem
                    refundAmount != null && !refundDone -> error = "Mark the refund as done to cancel."
                    else -> confirming = true
                }
            }) { Text("Continue") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep it") } },
    )
}

/**
 * What a cancelled-by-vendor request shows: who cancelled and why, and what happened to the money. [payment] and [refund] are null for an
 * exchange (no money). [viewerIsPayer] says whose side the refund line is written from.
 */
@Composable
fun CancellationCard(
    vendorName: String,
    cancellation: Cancellation,
    agreedAmount: Long?,
    payment: Payment?,
    refund: Refund?,
    viewerIsPayer: Boolean,
    modifier: Modifier = Modifier,
    showMoney: Boolean = true,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Cancelled by $vendorName", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text("Reason: ${cancellation.reason.displayName}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            if (cancellation.note.isNotBlank()) {
                Text("\"${cancellation.note}\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
            }
            if (showMoney && payment != null) {
                Text(
                    refundLine(refundStatusFor(payment, refund), refund?.amount ?: agreedAmount, vendorName, viewerIsPayer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

/** The refund line: refunded, nothing paid, or "the payment was never confirmed", from the payer's or the vendor's point of view. */
fun refundLine(status: RefundStatus, amount: Long?, vendorName: String, viewerIsPayer: Boolean): String = when (status) {
    RefundStatus.DONE ->
        if (viewerIsPayer) "Refund: ${formatPrice(amount ?: 0)} marked as refunded by $vendorName." else "Refund: ${formatPrice(amount ?: 0)} marked as refunded."
    RefundStatus.NOT_NEEDED -> "No payment was made, so there is nothing to refund."
    RefundStatus.UNCONFIRMED ->
        if (viewerIsPayer) "You marked this as paid, but $vendorName never confirmed it. If you did pay, ask $vendorName for a refund."
        else "The payment was marked paid but never confirmed, so no refund was marked."
}

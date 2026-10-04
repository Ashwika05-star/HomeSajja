package com.homesajja.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.Quote
import com.homesajja.app.payment.MAX_QUOTE_NOTE_LENGTH
import com.homesajja.app.payment.QuoteForm
import com.homesajja.app.payment.UpiPayment
import com.homesajja.app.payment.quoteError
import com.homesajja.app.ui.util.formatPrice

/**
 * The vendor's quote as the customer (and the vendor) see it. Once the amount is agreed ([agreedAmount] set) it is shown as the
 * fixed price: it can't change any more.
 */
@Composable
fun QuoteCard(quote: Quote, repair: Boolean, agreedAmount: Long?, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    when {
                        agreedAmount != null -> "Agreed price"
                        quote.revision > 1 -> "Revised quote (#${quote.revision})"
                        else -> "Quote"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                if (agreedAmount != null) StatusBadge(status = "Agreed")
            }
            Text(formatPrice(agreedAmount ?: quote.amount), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            if (repair && quote.estimatedDays != null) {
                Text("About ${quote.estimatedDays} day${if (quote.estimatedDays == 1) "" else "s"}", style = MaterialTheme.typography.bodyMedium)
            }
            if (!repair && quote.direction != null) Text(quote.direction.displayName, style = MaterialTheme.typography.bodyMedium)
            if (quote.note.isNotBlank()) {
                Text(quote.note, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (agreedAmount != null) {
                Text(
                    "Both sides agreed on this price. It can't be changed now.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/**
 * The vendor's quote form: amount in rupees, estimated days (repairs) or who pays whom (recycling), and an optional note.
 * [vendorUpiId] is the vendor's saved UPI id; it travels with the quote so the customer can pay by UPI.
 */
@Composable
fun QuoteDialog(
    repair: Boolean,
    revised: Boolean,
    initial: Quote?,
    vendorUpiId: String?,
    onSend: (QuoteForm) -> Unit,
    onDismiss: () -> Unit,
) {
    var form by remember {
        mutableStateOf(
            QuoteForm(
                amount = initial?.amount?.toString().orEmpty(),
                days = initial?.estimatedDays?.toString().orEmpty(),
                note = initial?.note.orEmpty(),
                direction = initial?.direction,
            ),
        )
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (revised) "Send a revised quote" else if (repair) "Quote this repair" else "Quote this recycling job") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (repair) "The customer sees this and accepts or declines it. You can start work only after they accept, and the price can't change after that."
                    else "Recycling is free unless you attach an amount. The customer accepts or declines it, and the price can't change after that.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                AppTextField(
                    value = form.amount,
                    onValueChange = { form = form.copy(amount = it.filter(Char::isDigit).take(8)); error = null },
                    label = "Amount (₹)",
                    keyboardType = KeyboardType.Number,
                )
                if (repair) {
                    AppTextField(
                        value = form.days,
                        onValueChange = { form = form.copy(days = it.filter(Char::isDigit).take(3)); error = null },
                        label = "Estimated days",
                        keyboardType = KeyboardType.Number,
                    )
                } else {
                    Text("Who pays?", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = form.direction == PayDirection.USER_PAYS_VENDOR,
                            onClick = { form = form.copy(direction = PayDirection.USER_PAYS_VENDOR); error = null },
                            label = { Text("Customer pays me") },
                        )
                        FilterChip(
                            selected = form.direction == PayDirection.VENDOR_PAYS_USER,
                            onClick = { form = form.copy(direction = PayDirection.VENDOR_PAYS_USER); error = null },
                            label = { Text("I pay the customer") },
                        )
                    }
                }
                AppTextField(
                    value = form.note,
                    onValueChange = { form = form.copy(note = it.take(MAX_QUOTE_NOTE_LENGTH)); error = null },
                    label = "Note (optional)",
                    singleLine = false,
                    minLines = 2,
                )
                if (form.direction != PayDirection.VENDOR_PAYS_USER) {
                    Text(
                        if (vendorUpiId != null) "Customers will be able to pay you at $vendorUpiId (from your profile)."
                        else "You haven't saved a UPI ID, so customers will only see cash. Add one in your profile to be paid by UPI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        "The customer will be asked where to send the money when they accept.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = quoteError(form, needsDays = repair, needsDirection = !repair)
                if (problem != null) error = problem else onSend(form)
            }) { Text("Send quote") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Shown when the customer accepts a recycling quote where the recycler pays: they say where to send the money (or leave it empty for cash). */
@Composable
fun PayoutUpiDialog(amount: Long, initialUpiId: String?, onConfirm: (String?) -> Unit, onDismiss: () -> Unit) {
    var upiId by remember { mutableStateOf(initialUpiId.orEmpty()) }
    val entered = upiId.trim()
    val invalid = entered.isNotEmpty() && !UpiPayment.isValidUpiId(entered)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Accept ${formatPrice(amount)}?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("The recycler will pay you ${formatPrice(amount)}. Add your UPI ID so they can send it, or leave it empty to be paid in cash.")
                AppTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = "Your UPI ID (optional)",
                    placeholder = "name@bank",
                    isError = invalid,
                    errorMessage = if (invalid) "That doesn't look like a UPI ID (name@bank)" else null,
                )
            }
        },
        confirmButton = {
            TextButton(enabled = !invalid, onClick = { onConfirm(entered.ifEmpty { null }) }) { Text("Accept") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

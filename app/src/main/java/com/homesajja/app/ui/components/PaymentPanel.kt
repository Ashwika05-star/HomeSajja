package com.homesajja.app.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.homesajja.app.data.model.Payment
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.PaymentStatus
import com.homesajja.app.payment.MAX_UPI_REF_LENGTH
import com.homesajja.app.payment.PaymentTurn
import com.homesajja.app.payment.UpiLaunchResult
import com.homesajja.app.payment.UpiPayment
import com.homesajja.app.payment.turnFor
import com.homesajja.app.ui.util.formatPrice
import kotlinx.coroutines.delay

/**
 * A scannable UPI QR code for [content] (a `upi://pay` link). Always dark on white, whatever the app theme, so scanners can read it.
 * Made with ZXing's encoder (no camera or UI code from the library) and drawn on a Compose canvas.
 */
@Composable
fun UpiQrCode(content: String, description: String, modifier: Modifier = Modifier, size: Dp = 220.dp) {
    val matrix = remember(content) {
        runCatching {
            QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                0,
                0,
                mapOf(
                    EncodeHintType.MARGIN to 0,
                    EncodeHintType.CHARACTER_SET to "UTF-8",
                    EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
                ),
            )
        }.getOrNull()
    }
    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (matrix == null) {
            Text("Couldn't make the QR code", color = Color.Black, style = MaterialTheme.typography.bodySmall)
        } else {
            Canvas(Modifier.fillMaxSize()) {
                val modules = matrix.width
                val cell = this.size.minDimension / modules
                for (y in 0 until modules) {
                    for (x in 0 until modules) {
                        // A hair of overlap so no thin lines show between neighbouring squares.
                        if (matrix[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell + 0.6f, cell + 0.6f))
                    }
                }
            }
        }
    }
}

/** A labelled value (e.g. a UPI id) with a Copy button that says "Copied" for a moment. */
@Composable
fun CopyableValue(label: String, value: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var copied by remember(value) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        TextButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
            copied = true
        }) {
            Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(if (copied) "Copied" else "Copy", modifier = Modifier.padding(start = 6.dp))
        }
    }
}

/**
 * The payment of one agreed amount, for whoever is looking at it: the payer sees how to pay (UPI app chooser, the payee's UPI id with Copy,
 * a QR code, or cash) and taps "I've paid"; the payee sees what to expect and taps "Payment received" once the money has arrived.
 * Only that confirmation counts as money earned. [cashLabel] names the cash option, e.g. "Cash on pickup" or "Cash at service".
 */
@Composable
fun PaymentPanel(
    amount: Long,
    payment: Payment,
    viewerId: String?,
    payerName: String,
    payeeName: String,
    paymentNote: String,
    cashLabel: String,
    busy: Boolean,
    onMarkPaid: (PaymentMethod, String?) -> Unit,
    onConfirmReceived: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val turn = payment.turnFor(viewerId)
    if (turn == PaymentTurn.NONE) return
    val context = LocalContext.current
    var showQr by remember { mutableStateOf(false) }
    var noUpiApp by remember { mutableStateOf(false) }
    var askPaid by remember { mutableStateOf(false) }
    var askReceived by remember { mutableStateOf(false) }

    val upiId = payment.payeeUpiId
    val payer = if (viewerId == payment.payerId) "You" else payerName
    val payee = if (viewerId == payment.payeeId) "you" else payeeName
    val link = upiId?.let { UpiPayment.buildLinkString(it, payeeName, amount, paymentNote) }
    val methodText = payment.method?.let { if (it == PaymentMethod.CASH) cashLabel.lowercase() else "UPI" }.orEmpty()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Payment", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                StatusBadge(status = payment.status.displayName)
            }
            Text(
                "${formatPrice(amount)} · ${payer.replaceFirstChar { it.uppercase() }} ${if (payer == "You") "pay" else "pays"} $payee",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )

            when (turn) {
                PaymentTurn.PAY -> {
                    if (upiId != null) {
                        CopyableValue(label = "Pay to this UPI ID", value = upiId)
                        PrimaryButton(
                            text = "Pay ${formatPrice(amount)} with a UPI app",
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                noUpiApp = UpiPayment.launch(context, UpiPayment.buildLink(upiId, payeeName, amount, paymentNote)) == UpiLaunchResult.NO_UPI_APP
                            },
                        )
                        if (noUpiApp) {
                            Text(
                                "No UPI app was found on this phone. Scan the QR code from another phone, copy the UPI ID, or pay in cash.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        OutlinedButton(
                            text = if (showQr) "Hide QR code" else "Show QR code",
                            onClick = { showQr = !showQr },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            "$payeeName hasn't added a UPI ID. Pay in cash, or ask them for payment details in chat.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    OutlinedButton(text = "I've paid", onClick = { askPaid = true }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                }

                PaymentTurn.WAIT_FOR_PAYER -> {
                    Text("Waiting for $payerName to pay.", style = MaterialTheme.typography.bodyMedium)
                    if (upiId != null) {
                        CopyableValue(label = "Your UPI ID (shown to the payer)", value = upiId)
                        OutlinedButton(
                            text = if (showQr) "Hide QR code" else "Show QR code for them to scan",
                            onClick = { showQr = !showQr },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else {
                        Text(
                            "Tip: add your UPI ID in your profile so people can pay you by UPI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                PaymentTurn.CONFIRM -> {
                    Text(
                        "$payerName says they paid${if (methodText.isNotEmpty()) " by $methodText" else ""}." +
                            (payment.upiRef?.let { " Reference: $it." } ?: "") +
                            " Check your account or cash, then confirm. It only counts as earned once you do.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    PrimaryButton(text = "Payment received", onClick = { askReceived = true }, enabled = !busy, modifier = Modifier.fillMaxWidth())
                }

                PaymentTurn.WAIT_FOR_PAYEE -> Text(
                    "You marked this as paid${if (methodText.isNotEmpty()) " ($methodText)" else ""}. Waiting for $payeeName to confirm they received it.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                PaymentTurn.DONE -> Text(
                    "✓ ${if (viewerId == payment.payeeId) "You" else payeeName} confirmed receiving ${formatPrice(amount)}" +
                        (if (methodText.isNotEmpty()) " ($methodText)." else "."),
                    style = MaterialTheme.typography.bodyMedium,
                )

                PaymentTurn.NONE -> Unit
            }

            if (showQr && link != null && payment.status == PaymentStatus.UNPAID) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    UpiQrCode(content = link, description = "UPI QR code to pay ${formatPrice(amount)} to $payeeName")
                }
                Text(
                    "Scan with any UPI app. The amount is filled in for you.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (askPaid) {
        MarkPaidDialog(
            amount = amount,
            payeeName = payeeName,
            cashLabel = cashLabel,
            hasUpiId = upiId != null,
            onDismiss = { askPaid = false },
            onConfirm = { method, ref ->
                askPaid = false
                onMarkPaid(method, ref)
            },
        )
    }
    if (askReceived) {
        AlertDialog(
            onDismissRequest = { askReceived = false },
            title = { Text("Payment received?") },
            text = { Text("Confirm only if ${formatPrice(amount)} from $payerName has really reached you. This can't be undone, and it adds to what you've earned.") },
            confirmButton = {
                TextButton(onClick = {
                    askReceived = false
                    onConfirmReceived()
                }) { Text("Yes, received") }
            },
            dismissButton = { TextButton(onClick = { askReceived = false }) { Text("Not yet") } },
        )
    }
}

/** The payer says how they paid (UPI or cash) and may add the UPI transaction reference. */
@Composable
private fun MarkPaidDialog(
    amount: Long,
    payeeName: String,
    cashLabel: String,
    hasUpiId: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, String?) -> Unit,
) {
    var method by remember { mutableStateOf(if (hasUpiId) PaymentMethod.UPI else PaymentMethod.CASH) }
    var reference by remember { mutableStateOf("") }
    val refTooLong = reference.trim().length > MAX_UPI_REF_LENGTH

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("I've paid ${formatPrice(amount)}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("$payeeName will be asked to confirm they received it.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaymentMethod.entries.forEach { option ->
                        FilterChip(
                            selected = method == option,
                            onClick = { method = option },
                            label = { Text(if (option == PaymentMethod.CASH) cashLabel else option.displayName) },
                        )
                    }
                }
                if (method == PaymentMethod.UPI) {
                    AppTextField(
                        value = reference,
                        onValueChange = { reference = it },
                        label = "UPI transaction reference (optional)",
                        placeholder = "e.g. 412345678901",
                        isError = refTooLong,
                        errorMessage = if (refTooLong) "Keep it under $MAX_UPI_REF_LENGTH characters" else null,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !refTooLong, onClick = { onConfirm(method, reference.takeIf { method == PaymentMethod.UPI }) }) { Text("I've paid") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

package com.homesajja.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.ReportReason
import com.homesajja.app.data.model.ReportTarget
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.viewmodel.TrustActionsViewModel

/**
 * A "more" menu for reporting a listing or a person and blocking a person. Nothing shows for your own listing or
 * profile. Confirmations appear as a short toast, so screens don't need a snackbar of their own.
 */
@Composable
fun TrustMenu(
    userId: String?,
    userName: String,
    modifier: Modifier = Modifier,
    listingId: String? = null,
    listingTitle: String = "",
) {
    val viewModel: TrustActionsViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var reportTarget by remember { mutableStateOf<ReportTarget?>(null) }
    var confirmBlock by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    val otherUser = userId?.takeIf { it != viewModel.myId }
    if (otherUser == null && listingId == null) return
    val isBlocked = otherUser != null && otherUser in viewModel.blockedIds

    IconButton(onClick = { menuOpen = true }, modifier = modifier) {
        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        if (listingId != null && otherUser != null) {
            DropdownMenuItem(text = { Text("Report listing") }, onClick = { menuOpen = false; reportTarget = ReportTarget.LISTING })
        }
        if (otherUser != null) {
            DropdownMenuItem(text = { Text("Report $userName") }, onClick = { menuOpen = false; reportTarget = ReportTarget.USER })
            if (isBlocked) {
                DropdownMenuItem(text = { Text("Unblock $userName") }, onClick = { menuOpen = false; viewModel.unblock(otherUser, userName) })
            } else {
                DropdownMenuItem(text = { Text("Block $userName") }, onClick = { menuOpen = false; confirmBlock = true })
            }
        }
    }

    reportTarget?.let { target ->
        ReportDialog(
            title = if (target == ReportTarget.LISTING) "Report this listing" else "Report $userName",
            onSubmit = { reason, details ->
                reportTarget = null
                if (target == ReportTarget.LISTING) {
                    viewModel.report(target, listingId.orEmpty(), listingTitle, reason, details)
                } else {
                    viewModel.report(target, otherUser.orEmpty(), userName, reason, details)
                }
            },
            onDismiss = { reportTarget = null },
        )
    }

    if (confirmBlock && otherUser != null) {
        AlertDialog(
            onDismissRequest = { confirmBlock = false },
            title = { Text("Block $userName?") },
            text = { Text("You and $userName won't be able to chat or send each other requests. You can unblock them any time from your profile.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmBlock = false
                    viewModel.block(otherUser, userName)
                }) { Text("Block", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmBlock = false }) { Text("Cancel") } },
        )
    }
}

/** Pick a reason and optionally add details. Nothing acts on a report automatically; it is read by the HomeSajja team. */
@Composable
private fun ReportDialog(title: String, onSubmit: (ReportReason, String) -> Unit, onDismiss: () -> Unit) {
    var reason by remember { mutableStateOf<ReportReason?>(null) }
    var details by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ReportReason.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().selectable(selected = reason == option, onClick = { reason = option }, role = Role.RadioButton),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == option, onClick = null, modifier = Modifier.padding(end = 8.dp))
                        Text(option.displayName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
                AppTextField(
                    value = details,
                    onValueChange = { details = it.take(500) },
                    label = "Details (optional)",
                    singleLine = false,
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = { TextButton(onClick = { reason?.let { onSubmit(it, details) } }, enabled = reason != null) { Text("Send report") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

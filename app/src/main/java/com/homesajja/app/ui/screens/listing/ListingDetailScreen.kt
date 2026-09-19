package com.homesajja.app.ui.screens.listing

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.FurnitureListing
import com.homesajja.app.data.model.ItemState
import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.data.model.ListingStatus
import com.homesajja.app.data.model.PurchaseRequest
import com.homesajja.app.data.model.SellerType
import com.homesajja.app.data.model.itemState
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.AppTextField
import com.homesajja.app.ui.components.AppTopBar
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.ImageCarousel
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.PrimaryButton
import com.homesajja.app.ui.components.PurchaseStatusTracker
import com.homesajja.app.ui.components.SecondaryButton
import com.homesajja.app.ui.components.RatingLine
import com.homesajja.app.ui.components.StatusBadge
import com.homesajja.app.ui.components.TrustMenu
import com.homesajja.app.ui.components.VerifiedBadge
import com.homesajja.app.data.model.RatingSummary
import com.homesajja.app.ui.util.displayText
import com.homesajja.app.ui.util.formatAge
import com.homesajja.app.ui.util.formatPrice
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.ListingDetailUiState
import com.homesajja.app.viewmodel.ListingDetailViewModel
import com.homesajja.app.viewmodel.validateOffer

@Composable
fun ListingDetailScreen(
    onBackClick: () -> Unit,
    onEditListing: (String) -> Unit,
    onProposeExchange: (String) -> Unit,
    onOpenVendor: (String) -> Unit,
    onOpenChat: (String) -> Unit,
    onOpenUser: (userId: String, name: String) -> Unit,
) {
    val viewModel: ListingDetailViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(viewModel) {
        viewModel.openChat.collect { onOpenChat(it) }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshSellerBlocked()
        onPauseOrDispose {}
    }

    Scaffold(
        topBar = {
            val listing = (state as? ListingDetailUiState.Content)?.listing
            AppTopBar(
                title = "Listing",
                onBackClick = onBackClick,
                actions = {
                    if (listing != null) TrustMenu(userId = listing.ownerId, userName = listing.ownerName, listingId = listing.id, listingTitle = listing.title)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            (state as? ListingDetailUiState.Content)?.takeIf { !it.isOwner }?.let {
                ActionBar(content = it, viewModel = viewModel, onProposeExchange = onProposeExchange)
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (val current = state) {
                ListingDetailUiState.Loading -> LoadingState()
                is ListingDetailUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::load)
                is ListingDetailUiState.Content -> DetailContent(current, onEditListing, onOpenVendor, onOpenUser)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailContent(
    content: ListingDetailUiState.Content,
    onEditListing: (String) -> Unit,
    onOpenVendor: (String) -> Unit,
    onOpenUser: (String, String) -> Unit,
) {
    val listing = content.listing
    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        ImageCarousel(images = listing.images)

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    listing.priceLabel(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(listing.title, style = MaterialTheme.typography.titleLarge)
                Text(
                    listing.city,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusBadge(status = listing.condition.displayName)
                StatusBadge(status = listing.category.displayName)
                if (listing.itemState() == ItemState.REFURBISHED) StatusBadge(status = "Refurbished")
                if (listing.status != ListingStatus.ACTIVE) StatusBadge(status = listing.status.displayName)
            }

            if (content.isOwner) {
                OwnerNotice(onEdit = { onEditListing(listing.id) })
            }

            content.myRequest?.let { MyRequestCard(it) }

            Section("Description") {
                Text(listing.description, style = MaterialTheme.typography.bodyLarge)
            }

            Section("Details") { DetailsTable(listing) }

            Section("Seller") {
                SellerCard(
                    listing = listing,
                    rating = content.sellerRating,
                    verified = content.sellerVerified,
                    blocked = content.sellerBlocked,
                    onOpenSeller = if (content.isOwner) null else if (listing.sellerType == SellerType.VENDOR) {
                        { onOpenVendor(listing.ownerId) }
                    } else {
                        { onOpenUser(listing.ownerId, listing.ownerName) }
                    },
                )
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun DetailsTable(listing: FurnitureListing) {
    val rows = listOfNotNull(
        "Category" to listing.category.displayName,
        "Material" to listing.material.displayName,
        "Condition" to listing.condition.displayName,
        "Age" to formatAge(listing.ageYears),
        listing.dimensions.displayText()?.let { "Dimensions" to it },
        listing.price.takeIf { listing.actionType == ListingActionType.EXCHANGE && it > 0 }
            ?.let { "Est. value" to formatPrice(it) },
    )
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            rows.forEachIndexed { index, (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(110.dp),
                    )
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
                if (index != rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun SellerCard(
    listing: FurnitureListing,
    rating: RatingSummary?,
    verified: Boolean,
    blocked: Boolean,
    onOpenSeller: (() -> Unit)?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().then(if (onOpenSeller != null) Modifier.clickable(onClick = onOpenSeller) else Modifier),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    listing.ownerName.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(listing.ownerName, style = MaterialTheme.typography.titleSmall)
                RatingLine(rating)
                if (verified) VerifiedBadge()
                if (blocked) {
                    Text("You've blocked this seller.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            StatusBadge(status = listing.sellerType.displayName)
        }
    }
}

@Composable
private fun MyRequestCard(request: PurchaseRequest) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Your request · ${formatPrice(request.offeredPrice)}", style = MaterialTheme.typography.titleSmall)
            PurchaseStatusTracker(status = request.status)
        }
    }
}

@Composable
private fun OwnerNotice(onEdit: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "This is your listing.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEdit) { Text("Edit") }
        }
    }
}

@Composable
private fun ActionBar(
    content: ListingDetailUiState.Content,
    viewModel: ListingDetailViewModel,
    onProposeExchange: (String) -> Unit,
) {
    val context = LocalContext.current
    val listing = content.listing
    var showBuyDialog by remember { mutableStateOf(false) }
    var showOfferDialog by remember { mutableStateOf(false) }
    val enabled = !content.isBusy

    Surface(color = MaterialTheme.colorScheme.background, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (listing.actionType == ListingActionType.EXCHANGE) {
                PrimaryButton(
                    text = "Propose exchange",
                    onClick = { onProposeExchange(listing.id) },
                    enabled = enabled && !content.sellerBlocked && listing.status == ListingStatus.ACTIVE,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryButton(
                        text = "Buy",
                        onClick = { showBuyDialog = true },
                        enabled = enabled && content.canRequestPurchase,
                        modifier = Modifier.weight(1f),
                    )
                    SecondaryButton(
                        text = "Make offer",
                        onClick = { showOfferDialog = true },
                        enabled = enabled && content.canRequestPurchase,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                ActionTextButton("Chat", Icons.AutoMirrored.Filled.Chat, enabled, viewModel::chatWithSeller)
                ActionTextButton(
                    if (content.isFavourite) "Saved" else "Save",
                    if (content.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    enabled,
                    viewModel::toggleFavourite,
                )
                ActionTextButton("Share", Icons.Filled.Share, true) {
                    val text = "${listing.title} — ${formatPrice(listing.price)} in ${listing.city} on HomeSajja"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(send, "Share listing"))
                }
            }
        }
    }

    if (showBuyDialog) {
        AlertDialog(
            onDismissRequest = { showBuyDialog = false },
            title = { Text("Send buy request?") },
            text = { Text("${listing.ownerName} will be asked to accept your request to buy \"${listing.title}\" for ${formatPrice(listing.price)}.") },
            confirmButton = {
                TextButton(onClick = {
                    showBuyDialog = false
                    viewModel.buy()
                }) { Text("Send request") }
            },
            dismissButton = { TextButton(onClick = { showBuyDialog = false }) { Text("Cancel") } },
        )
    }

    if (showOfferDialog) {
        OfferDialog(
            askingPrice = listing.price,
            onDismiss = { showOfferDialog = false },
            onSend = {
                showOfferDialog = false
                viewModel.makeOffer(it)
            },
        )
    }
}

@Composable
private fun ActionTextButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, enabled = enabled, contentPadding = PaddingValues(horizontal = 12.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun OfferDialog(askingPrice: Long, onDismiss: () -> Unit, onSend: (Long) -> Unit) {
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Make an offer") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Asking price is ${formatPrice(askingPrice)}.", style = MaterialTheme.typography.bodyMedium)
                AppTextField(
                    value = amount,
                    onValueChange = {
                        amount = it.filter(Char::isDigit)
                        error = null
                    },
                    label = "Your offer (₹)",
                    isError = error != null,
                    errorMessage = error,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val problem = validateOffer(amount, askingPrice)
                if (problem == null) onSend(amount.toLong()) else error = problem
            }) { Text("Send offer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

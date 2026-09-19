package com.homesajja.app.ui.screens.vendor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.homesajja.app.data.model.VendorBusinessType
import com.homesajja.app.data.model.VendorProfile
import com.homesajja.app.di.LocalAppContainer
import com.homesajja.app.di.ViewModelFactory
import com.homesajja.app.ui.components.ErrorState
import com.homesajja.app.ui.components.FurnitureCard
import com.homesajja.app.ui.components.ImageCarousel
import com.homesajja.app.ui.components.LoadingState
import com.homesajja.app.ui.components.OutlinedButton
import com.homesajja.app.ui.components.ShopMap
import com.homesajja.app.ui.components.VerifiedBadge
import com.homesajja.app.ui.util.priceLabel
import com.homesajja.app.viewmodel.VendorProfileUiState
import com.homesajja.app.viewmodel.VendorPublicProfileViewModel

/**
 * A vendor's public page. Used full-screen when a user taps a vendor, and as the vendor's own Profile
 * tab, where [onEditProfile] adds an edit button.
 */
@Composable
fun VendorPublicProfileScreen(
    onOpenListing: (String) -> Unit,
    modifier: Modifier = Modifier,
    onEditProfile: (() -> Unit)? = null,
) {
    val viewModel: VendorPublicProfileViewModel = viewModel(factory = ViewModelFactory(LocalAppContainer.current))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshIfStale()
        onPauseOrDispose {}
    }

    when (val current = state) {
        VendorProfileUiState.Loading -> LoadingState(modifier)
        is VendorProfileUiState.Error -> ErrorState(message = current.message, onRetry = viewModel::retry, modifier = modifier)
        is VendorProfileUiState.Content -> ProfileContent(current, onOpenListing, onEditProfile, modifier)
    }
}

@Composable
private fun ProfileContent(
    content: VendorProfileUiState.Content,
    onOpenListing: (String) -> Unit,
    onEditProfile: (() -> Unit)?,
    modifier: Modifier,
) {
    val vendor = content.vendor
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (vendor.brochureImages.isNotEmpty()) ImageCarousel(images = vendor.brochureImages)

        Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Header(vendor)

            if (content.isOwner && onEditProfile != null) {
                OutlinedButton(text = "Edit profile", onClick = onEditProfile, modifier = Modifier.fillMaxWidth())
            }

            Section("About") {
                Text(
                    vendor.description.ifBlank { "This vendor hasn't added a description yet." },
                    style = MaterialTheme.typography.bodyLarge,
                )
            }

            Section("Location") {
                if (vendor.shopAddress.isNotBlank()) Text(vendor.shopAddress, style = MaterialTheme.typography.bodyLarge)
                val lat = vendor.shopLatitude
                val lng = vendor.shopLongitude
                if (lat != null && lng != null) {
                    ShopMap(lat, lng, Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)))
                } else {
                    Text(
                        "Shop location not added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Section("Reviews") {
                // Real reviews arrive with the reviews phase.
                Text("☆ No ratings yet", style = MaterialTheme.typography.bodyLarge)
                Text(
                    "Customer reviews will appear here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section("Catalogue (${content.catalogue.size})") {
                if (content.catalogue.isEmpty()) {
                    Text(
                        "No items listed right now.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content.catalogue.forEach { listing ->
                FurnitureCard(
                    title = listing.title,
                    price = listing.priceLabel(),
                    imageUrl = listing.images.firstOrNull(),
                    subtitle = listing.category.displayName,
                    onClick = { onOpenListing(listing.id) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Header(vendor: VendorProfile) {
    Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(vendor.businessName.ifBlank { vendor.name }, style = MaterialTheme.typography.headlineMedium)
        Text(
            "${VendorBusinessType.fromNameOrNull(vendor.businessType)?.displayName ?: "Vendor"} · ${vendor.city}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (vendor.verified) VerifiedBadge()
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

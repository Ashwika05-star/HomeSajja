package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository

/** Exchange: browses listings that people put up for exchange, not for sale. */
class ExchangeBrowseViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    vendorRepository: VendorRepository,
    listingRepository: ListingRepository,
    favouriteRepository: FavouriteRepository,
) : ListingBrowseViewModel(
    authRepository = authRepository,
    userRepository = userRepository,
    listingRepository = listingRepository,
    favouriteRepository = favouriteRepository,
    actionType = ListingActionType.EXCHANGE,
    includeOwnListings = false,
    vendorRepository = vendorRepository,
)

package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.UserRepository

/** Buy & Sell: browses listings that are for sale, including the person's own (badged as theirs). */
class ExploreViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    listingRepository: ListingRepository,
    favouriteRepository: FavouriteRepository,
) : ListingBrowseViewModel(authRepository, userRepository, listingRepository, favouriteRepository, ListingActionType.SELL, includeOwnListings = true)

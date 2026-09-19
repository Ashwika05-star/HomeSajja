package com.homesajja.app.viewmodel

import com.homesajja.app.data.model.ListingActionType
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import com.homesajja.app.repository.ListingRepository
import com.homesajja.app.repository.UserRepository

/** Exchange: browses listings that people put up for exchange, not for sale. */
class ExchangeBrowseViewModel(
    authRepository: AuthRepository,
    userRepository: UserRepository,
    listingRepository: ListingRepository,
    favouriteRepository: FavouriteRepository,
) : ListingBrowseViewModel(authRepository, userRepository, listingRepository, favouriteRepository, ListingActionType.EXCHANGE)

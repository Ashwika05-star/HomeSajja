package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.FavouriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val MAX_SAVED = 200

/**
 * Which listings the signed-in person has saved, so any list of cards can show a filled or empty heart.
 * Tapping the heart updates it straight away and undoes itself if saving fails.
 */
class SavedIds(
    private val authRepository: AuthRepository,
    private val favouriteRepository: FavouriteRepository,
    private val scope: CoroutineScope,
) {
    var ids by mutableStateOf<Set<String>>(emptySet())
        private set

    fun load() {
        val uid = authRepository.currentUserId ?: return
        scope.launch {
            try {
                ids = favouriteRepository.getFavourites(uid, MAX_SAVED).map { it.listingId }.toSet()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Hearts just show as empty until the next load.
            }
        }
    }

    fun toggle(listingId: String) {
        val uid = authRepository.currentUserId ?: return
        val wasSaved = listingId in ids
        ids = if (wasSaved) ids - listingId else ids + listingId
        scope.launch {
            try {
                if (wasSaved) favouriteRepository.removeFavourite(uid, listingId) else favouriteRepository.addFavourite(uid, listingId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ids = if (wasSaved) ids + listingId else ids - listingId
            }
        }
    }
}

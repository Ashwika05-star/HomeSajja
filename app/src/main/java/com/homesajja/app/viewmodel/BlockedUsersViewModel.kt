package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Block
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.BlockRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface BlockedUiState {
    data object Loading : BlockedUiState
    data class Error(val message: String) : BlockedUiState
    data class Content(val blocked: List<Block>) : BlockedUiState
}

/** The people the signed-in person has blocked, with a way to unblock each. */
class BlockedUsersViewModel(
    private val authRepository: AuthRepository,
    private val blockRepository: BlockRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<BlockedUiState>(BlockedUiState.Loading)
    val uiState: StateFlow<BlockedUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load()
    }

    fun retry() {
        _uiState.value = BlockedUiState.Loading
        load()
    }

    fun unblock(block: Block) {
        val uid = authRepository.currentUserId ?: return
        viewModelScope.launch {
            try {
                blockRepository.unblock(uid, block.blockedId)
                (_uiState.value as? BlockedUiState.Content)?.let { content ->
                    _uiState.value = BlockedUiState.Content(content.blocked.filter { it.blockedId != block.blockedId })
                }
                _messages.tryEmit("${block.blockedName.ifBlank { "This person" }} is unblocked.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't unblock this person."))
            }
        }
    }

    private fun load() {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = BlockedUiState.Error("Please log in to see who you've blocked.")
            return
        }
        viewModelScope.launch {
            try {
                _uiState.value = BlockedUiState.Content(blockRepository.getBlocked(uid))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = BlockedUiState.Error(mapError(e, "Couldn't load your blocked list."))
            }
        }
    }
}

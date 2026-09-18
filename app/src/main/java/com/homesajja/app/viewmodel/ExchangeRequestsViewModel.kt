package com.homesajja.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.ExchangeRequest
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ExchangeRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ExchangeRequestsUiState {
    data object Loading : ExchangeRequestsUiState
    data class Error(val message: String) : ExchangeRequestsUiState

    /** [incoming] = requests others sent me; [outgoing] = requests I sent. */
    data class Content(
        val incoming: List<ExchangeRequest>,
        val outgoing: List<ExchangeRequest>,
    ) : ExchangeRequestsUiState
}

/** The Incoming / Outgoing exchange requests lists. Acting on a request happens on its detail screen. */
class ExchangeRequestsViewModel(
    private val authRepository: AuthRepository,
    private val exchangeRepository: ExchangeRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExchangeRequestsUiState>(ExchangeRequestsUiState.Loading)
    val uiState: StateFlow<ExchangeRequestsUiState> = _uiState

    private val refreshGate = RefreshGate()

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Silent reload, used when returning to the screen after acting on a request. */
    fun refresh() = load(showLoading = false)

    /** Called whenever the screen resumes; reloads only if the data is old. */
    fun refreshIfStale() {
        if (refreshGate.isStale()) refresh()
    }

    private fun load(showLoading: Boolean) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = ExchangeRequestsUiState.Error("Please log in to see your exchanges.")
            return
        }
        if (showLoading) _uiState.value = ExchangeRequestsUiState.Loading
        refreshGate.markLoaded()
        viewModelScope.launch {
            try {
                _uiState.value = ExchangeRequestsUiState.Content(
                    incoming = exchangeRepository.getRequestsByReceiver(uid),
                    outgoing = exchangeRepository.getRequestsBySender(uid),
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed silent refresh keeps the old lists instead of replacing them with an error.
                if (showLoading || _uiState.value !is ExchangeRequestsUiState.Content) {
                    _uiState.value = ExchangeRequestsUiState.Error(mapError(e, "Couldn't load your exchanges."))
                }
            } finally {
                refreshGate.markLoaded()
            }
        }
    }
}

package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.PayDirection
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.RecyclingRequest
import com.homesajja.app.payment.QuoteForm
import com.homesajja.app.payment.buildQuote
import com.homesajja.app.payment.nextRevision
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.payment.quoteError
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RecyclingRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RecycleDetailUiState {
    data object Loading : RecycleDetailUiState
    data class Error(val message: String) : RecycleDetailUiState

    /** [viewerIsRecycler] decides who the "other party" is and which actions apply. */
    data class Content(
        val request: RecyclingRequest,
        val actions: List<RecycleAction>,
        val viewerIsRecycler: Boolean,
        /** The viewer's own saved UPI id: the recycler's goes into a quote, the customer's pre-fills where a recycler should send money. */
        val myUpiId: String? = null,
        val isBusy: Boolean = false,
    ) : RecycleDetailUiState
}

/** One recycling request: its tracking pipeline and the actions the viewer may take. */
class RecycleDetailViewModel(
    savedStateHandle: SavedStateHandle,
    authRepository: AuthRepository,
    private val recyclingRepository: RecyclingRepository,
    private val notificationSender: NotificationSender,
    private val vendorRepository: VendorRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<RecycleDetailUiState>(RecycleDetailUiState.Loading)
    val uiState: StateFlow<RecycleDetailUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Runs one step with the buttons disabled, then shows what is really stored (whether it worked or not). */
    private fun act(failure: String, block: suspend (RecycleDetailUiState.Content) -> Unit) {
        val content = _uiState.value as? RecycleDetailUiState.Content ?: return
        if (content.isBusy) return
        _uiState.value = content.copy(isBusy = true)
        viewModelScope.launch {
            try {
                block(content)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                load(showLoading = false)
            }
        }
    }

    /**
     * A step that needs no form. Sending a quote goes through [sendQuote]. [customerUpiId] is only used when the customer accepts a quote
     * where the recycler pays: where the money should be sent (null = cash).
     */
    fun perform(action: RecycleAction, customerUpiId: String? = null) {
        if (action.needsQuote) return
        act("Couldn't update the request.") { content ->
            val request = content.request
            if (action !in content.actions) return@act
            when (action) {
                RecycleAction.ACCEPT_QUOTE -> {
                    recyclingRepository.acceptQuote(request, customerUpiId)
                    notificationSender.send(NotificationTemplates.recyclingQuoteDecision(request, accepted = true))
                    _messages.tryEmit("Quote accepted.")
                }
                RecycleAction.DECLINE_QUOTE -> {
                    recyclingRepository.updateStatus(request.id, action.target)
                    notificationSender.send(NotificationTemplates.recyclingQuoteDecision(request, accepted = false))
                    _messages.tryEmit("Quote declined.")
                }
                else -> {
                    recyclingRepository.updateStatus(request.id, action.target)
                    myId?.let { notificationSender.send(NotificationTemplates.recyclingStatus(request, action.target, it)) }
                    _messages.tryEmit("Status updated to ${action.target.displayName}.")
                }
            }
        }
    }

    /** The recycler attaches an amount and a direction (who pays whom), or sends a revised one after the customer declined. */
    fun sendQuote(form: QuoteForm) {
        act("Couldn't send the quote.") { content ->
            val request = content.request
            if (RecycleAction.SEND_QUOTE !in content.actions && RecycleAction.REVISE_QUOTE !in content.actions) return@act
            val problem = quoteError(form, needsDays = false, needsDirection = true)
            if (problem != null) {
                _messages.tryEmit(problem)
                return@act
            }
            // The recycler's UPI id travels with the quote only when they are the one being paid.
            val upi = if (form.direction == PayDirection.USER_PAYS_VENDOR) content.myUpiId else null
            val quote = buildQuote(form.copy(days = ""), upi, nextRevision(request.quote))
            recyclingRepository.sendQuote(request.id, quote)
            notificationSender.send(NotificationTemplates.recyclingQuoteSent(request, quote))
            _messages.tryEmit("Quote sent to ${request.userName}.")
        }
    }

    /** Whoever the quote says pays says they paid (UPI or cash). */
    fun markPaid(method: PaymentMethod, upiRef: String?) {
        act("Couldn't record the payment.") { content ->
            val request = content.request
            val payment = request.payment ?: return@act
            if (!request.paymentOpen || myId != payment.payerId) return@act
            recyclingRepository.markPaid(request.id, method, upiRef)
            val payerName = if (payment.payerId == request.userId) request.userName else request.vendorName.orEmpty().ifBlank { "The recycler" }
            notificationSender.send(
                NotificationTemplates.paymentMarked(payment, request.agreedAmount ?: 0L, "the recycling job", payerName, EntityType.RECYCLING_REQUEST, request.id),
            )
            _messages.tryEmit("Marked as paid. They will confirm it.")
        }
    }

    /** Whoever is being paid says the money arrived. */
    fun confirmPayment() {
        act("Couldn't confirm the payment.") { content ->
            val request = content.request
            val payment = request.payment ?: return@act
            if (!request.paymentOpen || myId != payment.payeeId) return@act
            recyclingRepository.confirmPayment(request.id)
            val payeeName = if (payment.payeeId == request.userId) request.userName else request.vendorName.orEmpty().ifBlank { "The recycler" }
            notificationSender.send(
                NotificationTemplates.paymentConfirmed(payment, request.agreedAmount ?: 0L, "the recycling job", payeeName, EntityType.RECYCLING_REQUEST, request.id),
            )
            _messages.tryEmit("Payment confirmed.")
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = RecycleDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = recyclingRepository.getRequest(requestId)
                if (request == null) {
                    _uiState.value = RecycleDetailUiState.Error("This recycling request no longer exists.")
                    return@launch
                }
                val viewerIsRecycler = myId != null && myId == request.vendorId
                _uiState.value = RecycleDetailUiState.Content(
                    request = request,
                    actions = recycleActionsFor(request, myId),
                    viewerIsRecycler = viewerIsRecycler,
                    myUpiId = myId?.let { id ->
                        runCatching {
                            if (viewerIsRecycler) vendorRepository.getVendorProfile(id)?.upiId else userRepository.getUserProfile(id)?.upiId
                        }.getOrNull()
                    },
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is RecycleDetailUiState.Content) {
                    _uiState.value = RecycleDetailUiState.Error(mapError(e, "Couldn't load this recycling request."))
                } else {
                    (_uiState.value as? RecycleDetailUiState.Content)?.let { _uiState.value = it.copy(isBusy = false) }
                }
            }
        }
    }
}

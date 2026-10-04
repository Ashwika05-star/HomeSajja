package com.homesajja.app.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.PaymentMethod
import com.homesajja.app.data.model.RepairRequest
import com.homesajja.app.payment.QuoteForm
import com.homesajja.app.payment.buildQuote
import com.homesajja.app.payment.nextRevision
import com.homesajja.app.payment.paymentOpen
import com.homesajja.app.payment.quoteError
import com.homesajja.app.repository.VendorRepository
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.RepairRepository
import com.homesajja.app.data.model.EntityType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface RepairDetailUiState {
    data object Loading : RepairDetailUiState
    data class Error(val message: String) : RepairDetailUiState

    /** [viewerIsVendor] decides who the "other party" is and which actions apply. [vendorUpiId] is the vendor's saved UPI id
     * (only loaded for the vendor), which a quote carries so the customer can pay by UPI. */
    data class Content(
        val request: RepairRequest,
        val actions: List<RepairAction>,
        val viewerIsVendor: Boolean,
        val vendorUpiId: String? = null,
        val isBusy: Boolean = false,
    ) : RepairDetailUiState
}

/** One repair request: its tracking pipeline and the actions the viewer may take. */
class RepairDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val authRepository: AuthRepository,
    private val repairRepository: RepairRepository,
    private val chatRepository: ChatRepository,
    private val notificationSender: NotificationSender,
    private val vendorRepository: VendorRepository,
) : ViewModel() {

    private val requestId: String = checkNotNull(savedStateHandle["requestId"])
    private val myId: String? = authRepository.currentUserId

    private val _uiState = MutableStateFlow<RepairDetailUiState>(RepairDetailUiState.Loading)
    val uiState: StateFlow<RepairDetailUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    /** Emits a chat id once the thread with the other party exists, so the screen can open it. */
    private val _openChat = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openChat: SharedFlow<String> = _openChat

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)

    /** Runs one step with the buttons disabled, then shows what is really stored (whether it worked or not). */
    private fun act(failure: String, block: suspend (RepairDetailUiState.Content) -> Unit) {
        val content = _uiState.value as? RepairDetailUiState.Content ?: return
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

    /** A step that needs no form. Sending a quote goes through [sendQuote]. */
    fun perform(action: RepairAction) {
        if (action.needsQuote) return
        act("Couldn't update the request.") { content ->
            val request = content.request
            if (action !in content.actions) return@act
            when (action) {
                RepairAction.ACCEPT_QUOTE -> {
                    repairRepository.acceptQuote(request)
                    notificationSender.send(NotificationTemplates.repairQuoteDecision(request, accepted = true))
                    _messages.tryEmit("Quote accepted. ${request.vendorName} can start work now.")
                }
                RepairAction.DECLINE_QUOTE -> {
                    repairRepository.updateStatus(request.id, action.target)
                    notificationSender.send(NotificationTemplates.repairQuoteDecision(request, accepted = false))
                    _messages.tryEmit("Quote declined.")
                }
                else -> {
                    repairRepository.updateStatus(request.id, action.target)
                    myId?.let { notificationSender.send(NotificationTemplates.repairStatus(request, action.target, it)) }
                    _messages.tryEmit("Status updated to ${action.target.displayName}.")
                }
            }
        }
    }

    /** The vendor sends a quote, or a revised one after the customer declined. */
    fun sendQuote(form: QuoteForm) {
        act("Couldn't send the quote.") { content ->
            val request = content.request
            if (RepairAction.SEND_QUOTE !in content.actions && RepairAction.REVISE_QUOTE !in content.actions) return@act
            val problem = quoteError(form, needsDays = true, needsDirection = false)
            if (problem != null) {
                _messages.tryEmit(problem)
                return@act
            }
            val quote = buildQuote(form, content.vendorUpiId, nextRevision(request.quote))
            repairRepository.sendQuote(request.id, quote)
            notificationSender.send(NotificationTemplates.repairQuoteSent(request, quote))
            _messages.tryEmit("Quote sent to ${request.userName}.")
        }
    }

    /** The customer says they paid (UPI or cash). */
    fun markPaid(method: PaymentMethod, upiRef: String?) {
        act("Couldn't record the payment.") { content ->
            val request = content.request
            val payment = request.payment ?: return@act
            if (!request.paymentOpen || myId != payment.payerId) return@act
            repairRepository.markPaid(request.id, method, upiRef)
            notificationSender.send(
                NotificationTemplates.paymentMarked(payment, request.agreedAmount ?: 0L, "the repair of ${request.furnitureTitle}", request.userName, EntityType.REPAIR_REQUEST, request.id),
            )
            _messages.tryEmit("Marked as paid. ${request.vendorName} will confirm it.")
        }
    }

    /** The vendor says the money arrived. */
    fun confirmPayment() {
        act("Couldn't confirm the payment.") { content ->
            val request = content.request
            val payment = request.payment ?: return@act
            if (!request.paymentOpen || myId != payment.payeeId) return@act
            repairRepository.confirmPayment(request.id)
            notificationSender.send(
                NotificationTemplates.paymentConfirmed(payment, request.agreedAmount ?: 0L, "the repair of ${request.furnitureTitle}", request.vendorName, EntityType.REPAIR_REQUEST, request.id),
            )
            _messages.tryEmit("Payment confirmed. It now counts as earned.")
        }
    }

    /** Finds or creates the chat between the customer and the provider about this repair, then opens it. */
    fun openChat() {
        val content = _uiState.value as? RepairDetailUiState.Content ?: return
        val request = content.request
        viewModelScope.launch {
            try {
                val chat = chatRepository.getOrCreateChat(
                    contextType = EntityType.REPAIR_REQUEST,
                    contextId = request.id,
                    contextTitle = "${request.furnitureTitle} · ${request.problemType.displayName}",
                    participantNames = mapOf(request.userId to request.userName, request.vendorId to request.vendorName),
                    contextImage = request.images.firstOrNull(),
                )
                _openChat.tryEmit(chat.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't open the chat."))
            }
        }
    }

    private fun load(showLoading: Boolean) {
        if (showLoading) _uiState.value = RepairDetailUiState.Loading
        viewModelScope.launch {
            try {
                val request = repairRepository.getRequest(requestId)
                if (request == null) {
                    _uiState.value = RepairDetailUiState.Error("This repair request no longer exists.")
                    return@launch
                }
                val viewerIsVendor = myId == request.vendorId
                _uiState.value = RepairDetailUiState.Content(
                    request = request,
                    actions = repairActionsFor(request, myId),
                    viewerIsVendor = viewerIsVendor,
                    // A quote carries the vendor's saved UPI id, so only the vendor needs it.
                    vendorUpiId = if (viewerIsVendor) runCatching { vendorRepository.getVendorProfile(request.vendorId)?.upiId }.getOrNull() else null,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is RepairDetailUiState.Content) {
                    _uiState.value = RepairDetailUiState.Error(mapError(e, "Couldn't load this repair request."))
                } else {
                    (_uiState.value as? RepairDetailUiState.Content)?.let { _uiState.value = it.copy(isBusy = false) }
                }
            }
        }
    }
}

package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.EntityType
import com.homesajja.app.data.model.FurnitureCategory
import com.homesajja.app.data.model.ItemRequest
import com.homesajja.app.data.model.ItemRequestStatus
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.ChatRepository
import com.homesajja.app.repository.ItemRequestRepository
import com.homesajja.app.repository.NotificationSender
import com.homesajja.app.repository.NotificationTemplates
import com.homesajja.app.repository.UserRepository
import com.homesajja.app.repository.VendorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface ItemRequestsUiState {
    data object Loading : ItemRequestsUiState
    data class Error(val message: String) : ItemRequestsUiState
    data class Content(
        val userRequests: List<ItemRequest>,
        val openRequests: List<ItemRequest>,
        val myAcceptedRequests: List<ItemRequest>,
        val allRequests: List<ItemRequest>,
        val isVendor: Boolean,
        val userCity: String,
        val userName: String,
    ) : ItemRequestsUiState
}

class ItemRequestsViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val vendorRepository: VendorRepository,
    private val itemRequestRepository: ItemRequestRepository,
    private val chatRepository: ChatRepository,
    private val notificationSender: NotificationSender,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ItemRequestsUiState>(ItemRequestsUiState.Loading)
    val uiState: StateFlow<ItemRequestsUiState> = _uiState

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    private val _openChat = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val openChat: SharedFlow<String> = _openChat

    var busyRequestId by mutableStateOf<String?>(null)
        private set

    init {
        load(showLoading = true)
    }

    fun retry() = load(showLoading = true)
    fun refresh() = load(showLoading = false)

    fun load(showLoading: Boolean = false) {
        val uid = authRepository.currentUserId
        if (uid == null) {
            _uiState.value = ItemRequestsUiState.Error("Please log in to view requests.")
            return
        }
        if (showLoading) _uiState.value = ItemRequestsUiState.Loading

        viewModelScope.launch {
            try {
                val user = userRepository.getUserProfile(uid)
                val vendor = if (user == null) vendorRepository.getVendorProfile(uid) else null
                val isVendor = vendor != null
                val cityName = user?.city ?: vendor?.city.orEmpty()
                val profileName = user?.name ?: vendor?.businessName?.ifBlank { vendor.name }.orEmpty()

                val userRequests = itemRequestRepository.getRequestsByUser(uid)
                val allRequests = itemRequestRepository.getAllRequests()
                val openRequests = allRequests.filter { it.status == ItemRequestStatus.OPEN }
                val myAccepted = allRequests.filter { it.acceptedVendorId == uid }

                _uiState.value = ItemRequestsUiState.Content(
                    userRequests = userRequests,
                    openRequests = openRequests,
                    myAcceptedRequests = myAccepted,
                    allRequests = allRequests,
                    isVendor = isVendor,
                    userCity = cityName,
                    userName = profileName,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (showLoading || _uiState.value !is ItemRequestsUiState.Content) {
                    _uiState.value = ItemRequestsUiState.Error(mapError(e, "Couldn't load item requests."))
                }
            }
        }
    }

    fun createRequest(
        title: String,
        category: FurnitureCategory,
        targetBudget: Long,
        description: String,
        city: String,
        onSuccess: () -> Unit,
    ) {
        val uid = authRepository.currentUserId ?: return
        val currentContent = _uiState.value as? ItemRequestsUiState.Content
        val userName = currentContent?.userName ?: "HomeSajja User"

        viewModelScope.launch {
            try {
                val request = ItemRequest(
                    userId = uid,
                    userName = userName,
                    title = title.trim(),
                    category = category,
                    targetBudget = targetBudget,
                    description = description.trim(),
                    city = city.trim(),
                    status = ItemRequestStatus.OPEN,
                )
                itemRequestRepository.createRequest(request)
                _messages.tryEmit("Item request created successfully!")
                load(showLoading = false)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't create request."))
            }
        }
    }

    fun acceptRequest(request: ItemRequest, note: String? = null) {
        val uid = authRepository.currentUserId ?: return
        val currentContent = _uiState.value as? ItemRequestsUiState.Content ?: return
        val vendorName = currentContent.userName.ifBlank { "Vendor" }

        runBusy(request.id, failure = "Couldn't accept request.") {
            itemRequestRepository.acceptRequest(
                id = request.id,
                vendorId = uid,
                vendorName = vendorName,
                note = note,
            )
            // Notify user
            notificationSender.send(
                NotificationTemplates.itemRequestAccepted(
                    request = request,
                    vendorName = vendorName,
                    vendorId = uid,
                )
            )
            _messages.tryEmit("Accepted request from ${request.userName}!")
            load(showLoading = false)
        }
    }

    fun fulfillRequest(request: ItemRequest) {
        runBusy(request.id, failure = "Couldn't update request.") {
            itemRequestRepository.fulfillRequest(request.id)
            _messages.tryEmit("Request marked as fulfilled!")
            load(showLoading = false)
        }
    }

    fun cancelRequest(request: ItemRequest) {
        runBusy(request.id, failure = "Couldn't cancel request.") {
            itemRequestRepository.cancelRequest(request.id)
            _messages.tryEmit("Request cancelled.")
            load(showLoading = false)
        }
    }

    fun chatWithUser(request: ItemRequest) {
        val uid = authRepository.currentUserId ?: return
        val otherId = if (uid == request.userId) request.acceptedVendorId else request.userId
        val otherName = if (uid == request.userId) request.acceptedVendorName ?: "Vendor" else request.userName
        if (otherId == null) return

        val myName = (_uiState.value as? ItemRequestsUiState.Content)?.userName ?: "User"
        viewModelScope.launch {
            try {
                val chat = chatRepository.getOrCreateChat(
                    contextType = EntityType.ITEM_REQUEST,
                    contextId = request.id,
                    contextTitle = request.title,
                    participantNames = mapOf(uid to myName, otherId to otherName),
                )
                _openChat.tryEmit(chat.id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't start chat."))
            }
        }
    }

    private fun runBusy(requestId: String, failure: String, block: suspend () -> Unit) {
        if (busyRequestId != null) return
        busyRequestId = requestId
        viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, failure))
            } finally {
                busyRequestId = null
            }
        }
    }
}

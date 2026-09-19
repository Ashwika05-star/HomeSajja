package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.data.model.Report
import com.homesajja.app.data.model.ReportReason
import com.homesajja.app.data.model.ReportTarget
import com.homesajja.app.repository.AuthRepository
import com.homesajja.app.repository.BlockRepository
import com.homesajja.app.repository.ReportRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** Report a listing or person, and block or unblock a person. Used from listing, profile and chat screens. */
class TrustActionsViewModel(
    private val authRepository: AuthRepository,
    private val reportRepository: ReportRepository,
    private val blockRepository: BlockRepository,
) : ViewModel() {

    val myId: String? = authRepository.currentUserId

    /** Who the person has blocked, so menus can offer Block or Unblock. */
    var blockedIds by mutableStateOf<Set<String>>(emptySet())
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    init {
        val uid = myId
        if (uid != null) {
            viewModelScope.launch {
                try {
                    blockedIds = blockRepository.getBlocked(uid).map { it.blockedId }.toSet()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The menu just offers "Block" until this loads.
                }
            }
        }
    }

    fun report(target: ReportTarget, targetId: String, targetName: String, reason: ReportReason, details: String) {
        val uid = myId ?: return
        viewModelScope.launch {
            try {
                reportRepository.submit(
                    Report(reporterId = uid, targetType = target, targetId = targetId, targetName = targetName, reason = reason, details = details.trim()),
                )
                _messages.tryEmit("Thanks, your report was sent.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't send your report. Please try again."))
            }
        }
    }

    fun block(userId: String, name: String) {
        val uid = myId ?: return
        viewModelScope.launch {
            try {
                blockRepository.block(uid, userId, name)
                blockedIds = blockedIds + userId
                _messages.tryEmit("$name is blocked. You can unblock them from your profile.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't block this person."))
            }
        }
    }

    fun unblock(userId: String, name: String) {
        val uid = myId ?: return
        viewModelScope.launch {
            try {
                blockRepository.unblock(uid, userId)
                blockedIds = blockedIds - userId
                _messages.tryEmit("$name is unblocked.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _messages.tryEmit(mapError(e, "Couldn't unblock this person."))
            }
        }
    }
}

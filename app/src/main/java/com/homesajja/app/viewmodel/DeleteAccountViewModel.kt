package com.homesajja.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesajja.app.repository.AccountRepository
import com.homesajja.app.repository.DeleteAccountException
import com.homesajja.app.repository.DeleteAccountFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

sealed interface DeleteAccountState {
    data object Idle : DeleteAccountState
    data object Working : DeleteAccountState
    data class Failed(val message: String) : DeleteAccountState
    data object Deleted : DeleteAccountState
}

/** "Delete my account": asks for confirmation, then removes the account. */
class DeleteAccountViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    var state by mutableStateOf<DeleteAccountState>(DeleteAccountState.Idle)
        private set
    var dialogOpen by mutableStateOf(false)
        private set

    val needsPassword: Boolean get() = accountRepository.usesPassword

    fun open() {
        state = DeleteAccountState.Idle
        dialogOpen = true
    }

    fun close() {
        if (state != DeleteAccountState.Working) dialogOpen = false
    }

    fun delete(password: String) {
        if (state == DeleteAccountState.Working) return
        state = DeleteAccountState.Working
        viewModelScope.launch {
            state = try {
                accountRepository.deleteAccount(password.takeIf { needsPassword })
                DeleteAccountState.Deleted
            } catch (e: CancellationException) {
                throw e
            } catch (e: DeleteAccountException) {
                DeleteAccountState.Failed(
                    when (e.failure) {
                        DeleteAccountFailure.WRONG_PASSWORD -> "That password isn't right. Nothing was deleted."
                        DeleteAccountFailure.LOGIN_AGAIN -> "For your security, log out and log in again, then delete your account right away. Nothing was deleted."
                        DeleteAccountFailure.NOT_SIGNED_IN -> "You're not signed in."
                    },
                )
            } catch (e: Exception) {
                DeleteAccountState.Failed(mapError(e, "Couldn't delete your account. Please try again."))
            }
        }
    }
}

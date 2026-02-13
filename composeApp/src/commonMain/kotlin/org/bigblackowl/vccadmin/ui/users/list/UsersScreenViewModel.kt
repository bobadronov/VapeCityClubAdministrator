package org.bigblackowl.vccadmin.ui.users.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.UserRepository
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.bigblackowl.vccadmin.utils.withRetry
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_shops
import vccadministrator.composeapp.generated.resources.error_unknown_load
import vccadministrator.composeapp.generated.resources.no_internet

class UsersScreenViewModel(
    private val userRepository: UserRepository,
    private val errorManager: ErrorManager,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(UsersScreenUiState())
    val uiState: StateFlow<UsersScreenUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            networkMonitorProvider.isConnected.collect { connected ->
                if (connected) {
                    val hasData = _uiState.value.userList.isNotEmpty()
                    loadUsers(isRefresh = hasData)
                }
            }
        }
    }

    fun onIntent(intent: UsersScreenIntent) {
        when (intent) {
            UsersScreenIntent.Load -> loadUsers(isRefresh = false)
            UsersScreenIntent.Refresh -> loadUsers(isRefresh = true)
        }
    }

    private fun loadUsers(isRefresh: Boolean) {
        viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                showMessage(getString(Res.string.no_internet))
                return@launch
            }

            try {
                _uiState.update {
                    it.copy(
                        isLoading = !isRefresh && it.userList.isEmpty(), // initial
                        isRefreshing = isRefresh                         // pull-to-loadUsers
                    )
                }

                withRetry {
                    val usersDeferred = async { userRepository.getUsers() }
                    val currentUserDeferred = async { userRepository.getCurrentUser() }

                    val userList = usersDeferred.await()
                    val currentUser = currentUserDeferred.await()

                    _uiState.update {
                        it.copy(
                            userList = userList,
                            currentUser = currentUser,
                        )
                    }
                } ?: throw Exception(getString(Res.string.error_load_shops))

            } catch (e: Exception) {
                showMessage(e.message ?: getString(Res.string.error_unknown_load))
                errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.UsersScreenViewModel.LOAD_USERS)
            } finally {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }


    private fun showMessage(message: String) = onEvent(UIEvents.ShowMessage(message))

    private fun onEvent(event: UIEvents) = viewModelScope.launch { _uiEvent.emit(event) }
}
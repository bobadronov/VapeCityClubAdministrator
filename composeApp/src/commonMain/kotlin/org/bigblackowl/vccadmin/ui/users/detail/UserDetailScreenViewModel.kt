package org.bigblackowl.vccadmin.ui.users.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
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
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_user
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.user_not_found
import kotlin.time.Duration.Companion.seconds

class UserDetailScreenViewModel(
    private val userRepository: UserRepository,
    private val errorManager: ErrorManager,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _state = MutableStateFlow(UserDetailState())
    val state: StateFlow<UserDetailState> = _state.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    fun onIntent(intent: UserDetailScreenIntent) {
        when (intent) {
            is UserDetailScreenIntent.Load -> loadUserInternal(intent.id, isRefresh = false)
            is UserDetailScreenIntent.Refresh -> loadUserInternal(intent.id, isRefresh = true)
        }
    }

    private fun loadUserInternal(id: String?, isRefresh: Boolean) {
        viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                showMessage(getString(Res.string.no_internet))
                return@launch
            }

            try {
                _state.update { prev ->
                    val hasData = prev.id.isNotBlank()
                    prev.copy(
                        isLoading = !isRefresh && !hasData, // full-screen тільки якщо ще нема даних
                        isRefreshing = isRefresh
                    )
                }

                if (id.isNullOrBlank()) {
                    showMessage(getString(Res.string.user_not_found))
                    delay(3.seconds)
                    onEvent(UIEvents.NavigateBack)
                    return@launch
                }

                val supabaseUser = userRepository.getUserById(id)
                if (supabaseUser == null) {
                    showMessage(getString(Res.string.user_not_found))
                    delay(3.seconds)
                    onEvent(UIEvents.NavigateBack)
                    return@launch
                }

                // (опційно) паралелізація
                val lastModifiedBy = userRepository.getUserNameById(supabaseUser.lastModifiedUserId)

                _state.update {
                    it.copy(
                        id = supabaseUser.id,
                        firstName = supabaseUser.firstName,
                        lastName = supabaseUser.lastName,
                        email = supabaseUser.email,
                        phone = supabaseUser.phone,
                        role = supabaseUser.role,
                        createdAt = AppStringProvider.formatTimestamp(supabaseUser.createdAt),
                        lastModified = AppStringProvider.formatTimestamp(supabaseUser.lastModified),
                        lastModifiedByUser = lastModifiedBy
                    )
                }

            } catch (e: Exception) {
                showMessage(e.message ?: getString(Res.string.error_load_user))
                errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.UserDetailScreenViewModel.LOAD_USER)
            } finally {
                _state.update { it.copy(isLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun showMessage(message: String) = onEvent(UIEvents.ShowMessage(message))
    private fun onEvent(event: UIEvents) = viewModelScope.launch { _uiEvent.emit(event) }
}

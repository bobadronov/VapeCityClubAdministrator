package org.bigblackowl.vccadmin.ui.users.addEdit

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
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.UserRepository
import org.bigblackowl.vccadmin.data.state.UIEvents
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.cannot_delete_self
import vccadministrator.composeapp.generated.resources.delete_error
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.only_admins_can_delete
import vccadministrator.composeapp.generated.resources.success_added
import vccadministrator.composeapp.generated.resources.success_deleted
import vccadministrator.composeapp.generated.resources.success_updated
import vccadministrator.composeapp.generated.resources.update_error
import vccadministrator.composeapp.generated.resources.user_not_found

class AddEditUserScreenViewModel(
    private val userRepository: UserRepository,
    private val errorManager: ErrorManager,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _uiState = MutableStateFlow(AddEditUserUiState())
    val uiState: StateFlow<AddEditUserUiState> = _uiState.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    fun onIntent(intent: AddEditUserScreenIntent) {
        when (intent) {
            is AddEditUserScreenIntent.UpdateEmail -> updateEmail(intent.email)
            is AddEditUserScreenIntent.UpdatePassword -> updatePassword(intent.password)
            is AddEditUserScreenIntent.UpdateFirstName -> updateFirstName(intent.firstName)
            is AddEditUserScreenIntent.UpdateLastName -> updateLastName(intent.lastName)
            is AddEditUserScreenIntent.UpdatePhone -> updatePhone(intent.phone)
            is AddEditUserScreenIntent.UpdateRole -> updateRole(intent.role)
            is AddEditUserScreenIntent.Save -> save()
            is AddEditUserScreenIntent.DiscardAndBack -> {
                cancelChanges()
                emitEvent(UIEvents.NavigateBack)
            }

            is AddEditUserScreenIntent.GoBack -> handleGoBack()
            is AddEditUserScreenIntent.DeleteUser -> deleteUser(intent.userId)
            is AddEditUserScreenIntent.LoadUser -> loadUser(intent.userId)
        }
    }

    private fun updateEmail(email: String) {
        _uiState.update { it.copy(editableEmail = email) }
        updateUnsavedAndValidity()
    }

    private fun updatePassword(password: String) {
        _uiState.update { it.copy(editablePassword = password) }
        updateUnsavedAndValidity()
    }

    private fun updateFirstName(firstName: String) {
        _uiState.update { it.copy(editableFirstName = firstName) }
        updateUnsavedAndValidity()
    }

    private fun updateLastName(lastName: String) {
        _uiState.update { it.copy(editableLastName = lastName) }
        updateUnsavedAndValidity()
    }

    private fun updatePhone(phone: String) {
        _uiState.update { it.copy(editablePhone = phone) }
        updateUnsavedAndValidity()
    }

    private fun updateRole(role: UserRole) {
        _uiState.update { it.copy(editableRole = role) }
        updateUnsavedAndValidity()
    }

    private fun updateUnsavedAndValidity() {
        val current = _uiState.value
        val valid = validateForm(
            email = current.editableEmail,
            password = current.editablePassword,
            firstName = current.editableFirstName,
            lastName = current.editableLastName
        )
        val unsaved = current.editableEmail != current.initialEmail ||
                current.editablePassword != current.initialPassword ||
                current.editableFirstName != current.initialFirstName ||
                current.editableLastName != current.initialLastName ||
                current.editablePhone != current.initialPhone ||
                current.editableRole != current.initialRole
        _uiState.update { it.copy(isFormValid = valid) }
        _isDirty.value = unsaved
    }

    private fun validateForm(email: String, password: String, firstName: String, lastName: String): Boolean {
        return email.isNotBlank() && email.contains("@") &&
                (password.isBlank() || password.length >= 6) && // Optional in edit
                firstName.isNotBlank() && lastName.isNotBlank()
    }

    private fun save() = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        val current = _uiState.value
        if (!current.isFormValid) {
            emitMessage(getString(Res.string.update_error))
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        try {
            val success = if (current.userId == null) {
                userRepository.registerUser(
                    email = current.editableEmail,
                    phone = current.editablePhone.ifBlank { null },
                    password = current.editablePassword.ifBlank { "12345678" },
                    firstName = current.editableFirstName,
                    lastName = current.editableLastName,
                    role = current.editableRole,
                )
            } else {
                userRepository.updateUser(
                    id = current.userId,
                    phone = current.editablePhone.ifBlank { null },
                    firstName = current.editableFirstName,
                    lastName = current.editableLastName,
                    role = current.editableRole,
                )
            }
            if (success) {
                if (current.userId == null) {
                    resetForm()
                    emitMessage(getString(Res.string.success_added))
                    delay(300)
                } else {
                    emitMessage(getString(Res.string.success_updated))
                }
                _isDirty.value = false
                emitEvent(UIEvents.NavigateBack)
            } else {
                throw Throwable(getString(Res.string.update_error))
            }
        } catch (e: Exception) {
            emitMessage(e.message ?: getString(Res.string.update_error))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditUserScreenViewModel.SAVE_USER)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleGoBack() {
        if (_isDirty.value) {
            emitEvent(UIEvents.ShowUnsavedChangesDialog) // If Events supports it, else handle in Composable
        } else {
            cancelChanges()
            emitEvent(UIEvents.NavigateBack)
        }
    }

    private fun loadUser(userId: String) = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        try {
            val user = userRepository.getUserById(userId)
            if (user != null) {
                _uiState.update {
                    it.copy(
                        userId = user.id,
                        initialEmail = user.email,
                        editableEmail = user.email,
                        initialPassword = "", // Not loaded for security
                        editablePassword = "",
                        initialFirstName = user.firstName,
                        editableFirstName = user.firstName,
                        initialLastName = user.lastName,
                        editableLastName = user.lastName,
                        initialPhone = user.phone,
                        editablePhone = user.phone,
                        initialRole = user.role,
                        editableRole = user.role,
                        isLoading = false
                    )
                }
                _isDirty.value = false
            } else {
                emitMessage(getString(Res.string.user_not_found))
                emitEvent(UIEvents.NavigateBack)
            }
        } catch (e: Exception) {
            emitMessage(e.message ?: getString(Res.string.update_error))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditUserScreenViewModel.LOAD_USER)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun deleteUser(userId: String) = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        _uiState.update { it.copy(isLoading = true) }
        try {
            val success = userRepository.deleteUser(userId)
            if (success) {
                emitMessage(getString(Res.string.success_deleted))
                delay(500)
                emitEvent(UIEvents.NavigateBack)
            } else {
                handleError(Exception(getString(Res.string.delete_error)))
            }
        } catch (e: Exception) {
            handleError(e)
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.AddEditUserScreenViewModel.DELETE_USER)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun cancelChanges() {
        _uiState.update { AddEditUserUiState() }
        _isDirty.value = false
    }

    private fun resetForm() {
        _uiState.update {
            it.copy(
                initialEmail = "",
                editableEmail = "",
                initialPassword = "",
                editablePassword = "",
                initialFirstName = "",
                editableFirstName = "",
                initialLastName = "",
                editableLastName = "",
                initialPhone = "",
                editablePhone = "",
                initialRole = UserRole.USER,
                editableRole = UserRole.USER
            )
        }
        _isDirty.value = false
    }

    private fun handleError(e: Exception) = viewModelScope.launch {
        val errorMessage = when {
            e.message?.contains("only admin", ignoreCase = true) == true -> getString(Res.string.only_admins_can_delete)
            e.message?.contains("cannot delete", ignoreCase = true) == true -> getString(Res.string.cannot_delete_self)
            else -> e.message ?: getString(Res.string.delete_error)
        }
        emitMessage(errorMessage)
    }

    private fun emitMessage(msg: String) = emitEvent(UIEvents.ShowMessage(msg))

    private fun emitEvent(event: UIEvents) = viewModelScope.launch { _uiEvent.emit(event) }
}
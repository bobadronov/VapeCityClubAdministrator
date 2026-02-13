package org.bigblackowl.vccadmin.ui.users.addEdit

import org.bigblackowl.vccadmin.data.entity.UserRole

data class AddEditUserUiState(
    val isLoading: Boolean = false,
    val userId: String? = null,
    val initialEmail: String = "",
    val editableEmail: String = "",
    val initialPassword: String = "",
    val editablePassword: String = "",
    val initialFirstName: String = "",
    val editableFirstName: String = "",
    val initialLastName: String = "",
    val editableLastName: String = "",
    val initialPhone: String = "",
    val editablePhone: String = "",
    val initialRole: UserRole = UserRole.USER,
    val editableRole: UserRole = UserRole.USER,
    val isFormValid: Boolean = false,
)
package org.bigblackowl.vccadmin.ui.users.addEdit

import org.bigblackowl.vccadmin.data.entity.UserRole

sealed interface AddEditUserScreenIntent {
    data class LoadUser(val userId: String) : AddEditUserScreenIntent
    data class UpdateEmail(val email: String) : AddEditUserScreenIntent
    data class UpdatePassword(val password: String) : AddEditUserScreenIntent
    data class UpdateFirstName(val firstName: String) : AddEditUserScreenIntent
    data class UpdateLastName(val lastName: String) : AddEditUserScreenIntent
    data class UpdatePhone(val phone: String) : AddEditUserScreenIntent
    data class UpdateRole(val role: UserRole) : AddEditUserScreenIntent
    object Save : AddEditUserScreenIntent
    object DiscardAndBack : AddEditUserScreenIntent
    data class DeleteUser(val userId: String) : AddEditUserScreenIntent
    object GoBack : AddEditUserScreenIntent
}
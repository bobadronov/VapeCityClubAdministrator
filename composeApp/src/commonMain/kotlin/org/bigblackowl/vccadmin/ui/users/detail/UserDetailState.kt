package org.bigblackowl.vccadmin.ui.users.detail

import org.bigblackowl.vccadmin.data.entity.UserRole

data class UserDetailState(
    val isLoading: Boolean = false,      // initial loading / full-screen
    val isRefreshing: Boolean = false,   // pull-to-refresh
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.USER,
    val createdAt: String = "",
    val lastModified: String = "",
    val lastModifiedByUser: String = "",
)
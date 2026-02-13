package org.bigblackowl.vccadmin.ui.users.list

import org.bigblackowl.vccadmin.data.entity.User

data class UsersScreenUiState(
    val isLoading: Boolean = false,      // initial loading / full-screen
    val isRefreshing: Boolean = false,   // pull-to-refresh
    val userList: List<User> = emptyList(),
    val currentUser: User? = null,
)
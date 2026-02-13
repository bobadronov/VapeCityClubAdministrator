package org.bigblackowl.vccadmin.ui.users.detail

sealed interface UserDetailScreenIntent {
    data class Load(val id: String) : UserDetailScreenIntent
    data class Refresh(val id: String) : UserDetailScreenIntent
}
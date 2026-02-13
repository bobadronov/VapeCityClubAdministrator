package org.bigblackowl.vccadmin.ui.users.list

sealed interface UsersScreenIntent {
    object Load : UsersScreenIntent
    object Refresh : UsersScreenIntent
}
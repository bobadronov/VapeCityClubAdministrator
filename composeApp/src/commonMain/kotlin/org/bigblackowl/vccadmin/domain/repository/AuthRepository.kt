package org.bigblackowl.vccadmin.domain.repository

import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.StateFlow
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole

interface AuthRepository {

    val sessionStatus: StateFlow<SessionStatus>

    val currentUser: StateFlow<User?>

    suspend fun login(email: String, password: String): Result<Unit>

    suspend fun signOut()

    suspend fun getUserRole(): UserRole
}
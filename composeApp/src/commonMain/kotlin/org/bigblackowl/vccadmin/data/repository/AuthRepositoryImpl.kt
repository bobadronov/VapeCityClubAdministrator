package org.bigblackowl.vccadmin.data.repository

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.entity.UserRole
import org.bigblackowl.vccadmin.domain.repository.AuthRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository


class AuthRepositoryImpl(
    private val supabase: SupabaseClient,
    private val userRepository: UserRepository,
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val _sessionStatus = MutableStateFlow<SessionStatus>(SessionStatus.Initializing)
    override val sessionStatus: StateFlow<SessionStatus> = _sessionStatus.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override suspend fun getUserRole(): UserRole {
        val users = userRepository.getUsers()
        return users.find { it.id == supabase.auth.currentUserOrNull()?.id }?.role ?: UserRole.USER
    }

    init {
        // Правильно підписуємось на зміни сесії Supabase
        scope.launch {

            supabase.auth.sessionStatus
                .onStart {
//                    Napier.i { "Початок спостереження за сесією Supabase" }
                }
                .catch { e ->
//                    Napier.e(e) { "Помилка в sessionStatus flow" }
                    _sessionStatus.value = SessionStatus.NotAuthenticated()
                }
                .collectLatest { status ->
//                    Napier.d { "Оновлення статусу сесії: $status" }

                    _sessionStatus.value = status
                    _currentUser.value = when (status) {
                        is SessionStatus.Authenticated -> {
                            val users = userRepository.getUsers()
                            users.find { it.id == status.session.user?.id }
                        }

                        else -> null
                    }

                    // Автоматично відновлюємо сесію при старті додатка
                    if (status is SessionStatus.NotAuthenticated && supabase.auth.currentSessionOrNull() != null) {
                        try {
                            supabase.auth.refreshSession(supabase.auth.currentSessionOrNull()?.refreshToken ?: "")
                        } catch (e: Exception) {
                            Napier.w(e) { "Не вдалося відновити сесію" }
                        }
                    }
                }
        }

        // При старті — одразу перевіряємо поточну сесію
        scope.launch {
            delay(100) // невелика затримка, щоб UI встиг ініціалізуватись
            try {
                supabase.auth.retrieveUserForCurrentSession(updateSession = true)
            } catch (e: Exception) {
                Napier.e(e) { "Немає активної сесії або не вдалося відновити" }
            }
        }
    }

    override suspend fun login(email: String, password: String): Result<Unit> {
        return try {
//            Napier.i { "Логін користувача: $email" }
            supabase.auth.signInWith(Email) {
                this.email = email.trim()
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e(e) { "Помилка логіну" }
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
//        Napier.i { "Лог аут користувача" }
        try {
            supabase.auth.signOut()
        } catch (e: Exception) {
            Napier.e(e) { "Помилка при виході" }
        }
    }

}

package org.bigblackowl.vccadmin.data.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.jordond.connectivity.Connectivity
import io.ktor.http.HttpMethod
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Провайдер для моніторингу стану мережі.
 * Використовує HTTP-полінг для перевірки підключення до ключових URL з підтримкою CORS.
 * Експонує стан як StateFlow<Boolean> для легкої інтеграції з Compose Multiplatform.
 * Автоматично стартує моніторинг при ініціалізації та зупиняє при очищенні ViewModel.
 */
class NetworkMonitorProvider : ViewModel() {
    private companion object {
        private const val TAG = "NetworkMonitorProvider"
    }

    private val connectivity = Connectivity {
        // URL з підтримкою CORS ('Access-Control-Allow-Origin: *') для браузерних таргетів
        urls(
            "https://ngqtxspsdehzkramqxlt.supabase.co/functions/v1/ping",
            "https://api.github.com/zen"
        )
        method = HttpMethod.Head  // HEAD-запит для економії трафіку (без тіла відповіді)
        port = 443            // HTTPS порт за замовчуванням
        pollingIntervalMs = 6.seconds  // Інтервал полінгу
        timeoutMs = 5.seconds          // Таймаут запиту
        autoStart = false            // Ручний старт для контролю в WasmJS/JS

        // Колбек для логування (інтегровано з Napier)
//        onPollResult { result ->
//            when (result) {
//                is PollResult.Error -> Napier.d(tag = TAG) { "Помилка полінгу: ${result.throwable.message}" }
//                is PollResult.Response -> Napier.d(tag = TAG) { "Відповідь полінгу: ${result.response}" }
//            }
//        }
    }

    /**
     * Стан підключення до мережі: true - підключено, false - відключено.
     */
    val isConnected: StateFlow<Boolean> = connectivity.statusUpdates
        .map { status -> status is Connectivity.Status.Connected }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(10_000),
            initialValue = false  // Початкове значення до першого полінгу
        )

    init {
        // Асинхронний старт моніторингу (безпечний для WasmJS/JS)
        viewModelScope.launch {
            connectivity.start()
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Асинхронна зупинка для уникнення витоків
        viewModelScope.launch {
            connectivity.stop()
        }
    }
}
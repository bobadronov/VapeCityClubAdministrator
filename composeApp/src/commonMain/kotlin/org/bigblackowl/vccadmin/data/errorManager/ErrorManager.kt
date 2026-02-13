package org.bigblackowl.vccadmin.data.errorManager

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.annotation.ExperimentalCoilApi
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.repository.LocalRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalCoilApi::class)
class ErrorManager(
    private val json: Json,
    private val supabase: SupabaseClient,
    private val preferences: LocalRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel() {
    private companion object {
        private const val TAG = "ErrorManager"
        private const val ERROR_REPORTS_TABLE = "error_reports"
        private const val CHECK_INTERVAL_MS = 30_000L
    }

    init {
        // при створенні ErrorManager запускаємо періодичну відправку
        autoFlushLoop()
    }

    @OptIn(ExperimentalTime::class)
    fun report(message: String, errorCode: Int) {
        viewModelScope.launch {
            val report = ErrorReport.create(
                message = message,
                errorCode = errorCode,
            )
            Napier.d(tag = TAG) { report.toString() }
            saveToBuffer(report)
        }
    }

    private fun saveToBuffer(report: ErrorReport) {
        viewModelScope.launch {
            val buffer = loadBuffer().toMutableList()
            buffer.add(report)
            preferences.saveErrorsToBuffer(json.encodeToString(buffer))
        }
    }

    private fun autoFlushLoop() {
        viewModelScope.launch {
            while (true) {
                flushBuffer()
                delay(CHECK_INTERVAL_MS)
            }
        }
    }

    private fun loadBuffer(): List<ErrorReport> {
        val data = preferences.loadErrorBuffer() ?: return emptyList()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            Napier.e("loadErrorBuffer: ${e.message}", tag = TAG)
            emptyList()
        }
    }

    private suspend fun flushBuffer() {
        val buffer = loadBuffer().toMutableList()
        if (buffer.isEmpty()) return

        if (!networkMonitorProvider.isConnected.value) return

        val iterator = buffer.iterator()
        while (iterator.hasNext()) {
            val report = iterator.next()
            var attempt = 0
            var sent = false
            while (attempt < 3 && !sent) {
                try {
                    supabase.from(ERROR_REPORTS_TABLE).insert(report)
                    sent = true
                    iterator.remove()
                } catch (e: Exception) {
                    Napier.e("loadErrorBuffer: ${e.message}", tag = TAG)
                    attempt++
                    delay((1 shl attempt) * 1000L)
                }
            }
        }
        preferences.saveErrorsToBuffer(json.encodeToString(buffer))
    }

}

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect object SystemInfoProvider {
    fun systemInfo(): SystemInfo
}

data class SystemInfo(
    val device: String,          // "brand model" / "iPhone ..." / "Desktop ..." / "Browser ..."
    val model: String? = null,    // model identifier / userAgent fragment / etc
    val product: String? = null,  // Android PRODUCT / Desktop OS / etc
    val osVersion: String? = null,// Android release / iOS systemVersion / etc
    val screenSize: String? = null,// "1080x2400" / "1920x1080"
)
package org.bigblackowl.vccadmin.di

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

val networkModule = module {
    single<Json> {
        Json {
            ignoreUnknownKeys = true
        }
    }

    single<HttpClient>(createdAtStart = true) {
        HttpClient(ktorEngine) {
            install(ContentNegotiation) {
                json(get())
            }
            install(HttpTimeout) {
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 120_000
                requestTimeoutMillis = 20 * 60_000 // 20 хв, або більше
            }

            install(HttpRequestRetry) {
                retryOnExceptionIf(maxRetries = 3) { _, cause ->
                    cause is HttpRequestTimeoutException ||
                            cause is SocketTimeoutException
                }
                exponentialDelay()
            }

            followRedirects = true
        }
    }

    single<SupabaseClient>(createdAtStart = true) {
        val localRepository: LocalRepository = get()
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            httpEngine = get<HttpClient>().engine
            install(Auth.Companion) {
                autoSaveToStorage = localRepository.getAutoEnterState()
                autoLoadFromStorage = localRepository.getAutoEnterState()
            }
            install(Postgrest.Companion)
            install(Functions.Companion)
            install(Storage.Companion)
        }
    }

    single<OpenAI> {
        OpenAI(
            token = BuildConfig.OPEN_AI_KEY,
            logging = LoggingConfig(
                logLevel = LogLevel.All,
                logger = Logger.Default,
            ),
            timeout = Timeout(socket = 60.seconds),
            // additional configurations...
        )
    }

    single(createdAtStart = true) { NetworkMonitorProvider() }

    singleOf(::ErrorManager)
}
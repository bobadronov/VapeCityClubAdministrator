// shared
package org.bigblackowl.vccadmin.di

import androidx.lifecycle.SavedStateHandle
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.LoggingConfig
import com.aallam.openai.client.OpenAI
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.BuildConfig
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.AiRepository
import org.bigblackowl.vccadmin.data.repository.AiRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.AuthRepository
import org.bigblackowl.vccadmin.data.repository.AuthRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.CityRepository
import org.bigblackowl.vccadmin.data.repository.CityRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.CitySearchRepository
import org.bigblackowl.vccadmin.data.repository.CitySearchRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.FileGeneratorRepository
import org.bigblackowl.vccadmin.data.repository.FileGeneratorRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.LocalRepository
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.ShopRepository
import org.bigblackowl.vccadmin.data.repository.ShopRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.SlideRepository
import org.bigblackowl.vccadmin.data.repository.SlideRepositoryImpl
import org.bigblackowl.vccadmin.data.repository.UserRepository
import org.bigblackowl.vccadmin.data.repository.UserRepositoryImpl
import org.bigblackowl.vccadmin.navigation.NavigationViewModel
import org.bigblackowl.vccadmin.ui.addEditShop.ShopAddEditScreenViewModel
import org.bigblackowl.vccadmin.ui.addEditSlideScreen.AddEditSlideViewModel
import org.bigblackowl.vccadmin.ui.city.addEdit.AddEditCityScreenViewModel
import org.bigblackowl.vccadmin.ui.city.list.CitiesListScreenViewModel
import org.bigblackowl.vccadmin.ui.editSlidesSettings.EditSlidesSettingsScreenViewModel
import org.bigblackowl.vccadmin.ui.fileGenerator.FileGeneratorScreenViewModel
import org.bigblackowl.vccadmin.ui.login.LoginScreenViewModel
import org.bigblackowl.vccadmin.ui.main.MainScreenViewModel
import org.bigblackowl.vccadmin.ui.shopDetail.ShopDetailsScreenViewModel
import org.bigblackowl.vccadmin.ui.slideAiGeneration.SlideAiGenerationScreenViewModel
import org.bigblackowl.vccadmin.ui.slidesList.SlidesListScreenViewModel
import org.bigblackowl.vccadmin.ui.users.addEdit.AddEditUserScreenViewModel
import org.bigblackowl.vccadmin.ui.users.detail.UserDetailScreenViewModel
import org.bigblackowl.vccadmin.ui.users.list.UsersScreenViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlin.time.Duration.Companion.seconds

// Модуль Koin для залежностей


expect val platformModule: Module
expect val ktorEngine: HttpClientEngine

val networkModule = module {
    single { LocalRepository() }
    single<Json> {
        Json {
            ignoreUnknownKeys = true
        }
    }
    single<HttpClient> {
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
    single<SupabaseClient> {
        val localRepository: LocalRepository = get()
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_KEY
        ) {
            httpEngine = get<HttpClient>().engine
            install(Auth) {
                autoSaveToStorage = localRepository.getAutoEnterState()
                autoLoadFromStorage = localRepository.getAutoEnterState()
            }
            install(Postgrest)
            install(Functions)
            install(Storage)
        }
    }

    single<OpenAI> {
        OpenAI(
            token = BuildConfig.OPEN_AI_KEY,
            logging = LoggingConfig(
                logLevel = LogLevel.All,
                logger = com.aallam.openai.api.logging.Logger.Default,
            ),
            timeout = Timeout(socket = 60.seconds),
            // additional configurations...
        )
    }

    singleOf(::NetworkMonitorProvider)
    singleOf(::ErrorManager)
}

val navigationModule = module {
    viewModel<NavigationViewModel> { (handle: SavedStateHandle) ->
        NavigationViewModel(
            authRepository = get(),
            addEditSlideViewModel = get(),
            shopAddEditScreenViewModel = get(),
            addEditCityScreenViewModel = get(),
            addEditUserScreenViewModel = get(),
            editSlidesSettingsScreenViewModel = get(),
            fileGeneratorScreenViewModel = get(),
            savedStateHandle = handle,
        )
    }
}

val screensModule = module {
    singleOf(::LoginScreenViewModel)
    singleOf(::MainScreenViewModel)
    singleOf(::ShopDetailsScreenViewModel)
    singleOf(::SlidesListScreenViewModel)
    singleOf(::AddEditSlideViewModel)
    singleOf(::ShopAddEditScreenViewModel)
    singleOf(::CitiesListScreenViewModel)
    singleOf(::AddEditCityScreenViewModel)
    singleOf(::UsersScreenViewModel)
    singleOf(::UserDetailScreenViewModel)
    singleOf(::AddEditUserScreenViewModel)
    singleOf(::FileGeneratorScreenViewModel)
    singleOf(::EditSlidesSettingsScreenViewModel)
    singleOf(::SlideAiGenerationScreenViewModel)
}

val repositoryModule = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    single<ShopRepository> { ShopRepositoryImpl(get()) }
    single<CityRepository> { CityRepositoryImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<SlideRepository> { SlideRepositoryImpl(get()) }
    single<FileGeneratorRepository> { FileGeneratorRepositoryImpl(get()) }
    single<AiRepository> { AiRepositoryImpl(get(), get()) }
    single<CitySearchRepository> { CitySearchRepositoryImpl(json = get()) }
}

val logger = module {
    if (BuildConfig.IS_DEBUG_BUILD) Napier.base(DebugAntilog())
}

val coreModules = listOf(
    logger,
    networkModule,
    repositoryModule,
    navigationModule,
    screensModule,
    platformModule,
)
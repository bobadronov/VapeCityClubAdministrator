// File: src/commonMain/kotlin/org/bigblackowl/vccadmin/ui/slidesList/SlidesListViewModel.kt
package org.bigblackowl.vccadmin.ui.slidesList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.toSlides
import org.bigblackowl.vccadmin.data.entity.toUiShops
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository
import org.bigblackowl.vccadmin.domain.repository.SlideRepository
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_load_data
import vccadministrator.composeapp.generated.resources.no_internet
import kotlin.time.Duration.Companion.seconds

class SlidesListScreenViewModel(
    private val slideRepository: SlideRepository,
    private val shopRepository: ShopRepository,
    private val cityRepository: CityRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
    private val errorManager: ErrorManager,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    private val _uiState = MutableStateFlow(SlidesListUiState())
    val uiState: StateFlow<SlidesListUiState> = _uiState.asStateFlow()

    private var loadJob: kotlinx.coroutines.Job? = null

    fun onIntent(intent: SlidesListScreenIntent) {
        when (intent) {
            SlidesListScreenIntent.Load -> loadSlides(forceRefresh = false)
            SlidesListScreenIntent.Refresh -> loadSlides(forceRefresh = true)
            is SlidesListScreenIntent.ToggleSlideVisibility -> toggleSlideVisibility(intent.slideId)
        }
    }

    private fun loadSlides(forceRefresh: Boolean) {
        loadJob?.cancel()

        val hasData = _uiState.value.slides.isNotEmpty() || _uiState.value.shopList.isNotEmpty()
        _uiState.update {
            it.copy(
                isInitialLoading = !hasData && !forceRefresh,
                isRefreshing = forceRefresh || hasData
            )
        }

        loadJob = viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
                return@launch
            }

            try {
                coroutineScope {
                    val slidesDeferred = async { slideRepository.getSlides() }
                    val shopsDeferred = async { shopRepository.getStores() }
                    val citiesDeferred = async { cityRepository.getCities() }

                    val supabaseShops = shopsDeferred.await()
                    val cities = citiesDeferred.await()
                    val supabaseSlides = slidesDeferred.await()

                    _uiState.update {
                        it.copy(
                            slides = supabaseSlides.toSlides().sortedBy { slide -> slide.createdAt },
                            shopList = supabaseShops.toUiShops(cities),
                        )
                    }
                }
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_load_data)
                _uiEvent.emit(UIEvents.ShowMessage(message))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.SlidesListScreenViewModel.LOAD_SLIDES)
                Napier.e(tag = "SlidesListViewModel") { message }
            } finally {
                _uiState.update { it.copy(isInitialLoading = false, isRefreshing = false) }
            }
        }
    }

    private fun toggleSlideVisibility(slideId: String) {
        viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                _uiEvent.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            try {
                _uiState.update { it.copy(isRefreshing = true, isInitialLoading = false) }
                slideRepository.toggleSlideVisibility(slideId = slideId)
                // якщо хочеш “видимий” процес — delay, інакше прибрати
                delay(1.seconds)

                loadSlides(forceRefresh = true)
            } catch (exception: Exception) {
                val message = exception.message ?: getString(Res.string.error_load_data)
                _uiEvent.emit(UIEvents.ShowMessage(message))
                errorManager.report(message = exception.message.orEmpty(), errorCode = ErrorCode.SlidesListScreenViewModel.TOGGLE_SLIDE_VISIBILITY)
                Napier.e(tag = "SlidesListViewModel") { message }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }


}

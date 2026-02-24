package org.bigblackowl.vccadmin.ui.editSlidesSettings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.TransitionEffect
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.data.utils.NetworkMonitorProvider
import org.bigblackowl.vccadmin.domain.repository.SlideRepository
import org.bigblackowl.vccadmin.domain.repository.UserRepository
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.jetbrains.compose.resources.getString
import org.koin.core.component.KoinComponent
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_loading_settings
import vccadministrator.composeapp.generated.resources.error_saving
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.settings_saved

// ViewModel для екрану редагування налаштувань слайдів
class EditSlidesSettingsScreenViewModel(
    private val errorManager: ErrorManager,
    private val userRepository: UserRepository,
    private val slideRepository: SlideRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel(), KoinComponent {

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0, extraBufferCapacity = 1)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    companion object {
        const val TAG = "EditSlidesSettingsScreenViewModel"
    }

    private val _state = MutableStateFlow(SlidesSettingsState())
    val state: StateFlow<SlidesSettingsState> = _state.asStateFlow()

    private val _isDirty = MutableStateFlow(false)

    // Обробка інтентів
    fun onIntent(intent: SlidesSettingsIntent) {
        Napier.d(tag = "EditSlidesSettingsScreenViewModel") { "onIntent _isDirty:${_isDirty.value}" }

        when (intent) {
            is SlidesSettingsIntent.ChangeAutoReloadTime -> updateState(autoReloadTime = intent.value)
            is SlidesSettingsIntent.ChangeEffect -> updateState(transitionEffect = intent.value)
            is SlidesSettingsIntent.ChangeSlideDuration -> updateState(slideDuration = intent.value)
            is SlidesSettingsIntent.ChangeTransitionDuration -> updateState(transitionDuration = intent.value)
            is SlidesSettingsIntent.GetAppSettings -> refreshSettings()
            is SlidesSettingsIntent.SaveSettings -> saveSettings()
            is SlidesSettingsIntent.GoBack -> {
                if (_isDirty.value) {
                    onEvent(UIEvents.ShowUnsavedChangesDialog)
                } else {
                    onEvent(UIEvents.NavigateBack)
                }
            }

            is SlidesSettingsIntent.DiscardChanges -> {
                loadSettings()
                onEvent(UIEvents.NavigateBack)
            }

            is SlidesSettingsIntent.Load -> loadSettings()
        }
    }

    // Завантаження налаштувань (використовується в init)
    private fun loadSettings() = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            val settings = slideRepository.getSlidesSettings()
            val slides = slideRepository.getSlides().take(4)
            val user = userRepository.getUserById(settings.lastModifiedByUser.orEmpty())
            _state.update {
                it.copy(
                    slides = slides.map { slide ->
                        SlideOrderItem(
                            id = slide.id,
                            fileName = slide.fileName,
                            position = slide.position,
                            url = slide.publicUrl
                        )
                    },
                    settingsId = settings.id,
                    slideDuration = settings.slideDuration,
                    transitionDuration = settings.transitionDuration,
                    transitionEffect = settings.transitionEffect,
                    autoReloadTime = settings.autoReloadTime,
                    lastModified = AppStringProvider.formatTimestamp(settings.lastModified ?: 0L),
                    lastModifiedByUser = user?.fullName.orEmpty()
                )
            }
            _isDirty.value = false
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Помилка завантаження налаштувань" }
            showMessage(getString(Res.string.error_loading_settings, e.message.orEmpty()))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.EditSlidesSettingsScreenViewModel.LOAD_DATA)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    // Оновлення налаштувань (refresh)
    private fun refreshSettings() = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }
        try {
            _state.update { it.copy(isLoading = true) }
            // Оновлюємо дані аналогічно init
            val settings = slideRepository.getSlidesSettings()
            val slides = slideRepository.getSlides().take(5)
            val user = userRepository.getUserById(settings.lastModifiedByUser.orEmpty())
            _state.update {
                it.copy(
                    slides = slides.map { slide ->
                        SlideOrderItem(
                            id = slide.id,
                            fileName = slide.fileName,
                            position = slide.position,
                            url = slide.publicUrl
                        )
                    },
                    settingsId = settings.id,
                    slideDuration = settings.slideDuration,
                    transitionDuration = settings.transitionDuration,
                    transitionEffect = settings.transitionEffect,
                    autoReloadTime = settings.autoReloadTime,
                    lastModified = AppStringProvider.formatTimestamp(settings.lastModified ?: 0L),
                    lastModifiedByUser = user?.fullName.orEmpty()
                )
            }
            showMessage(getString(Res.string.settings_saved))
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Помилка оновлення налаштувань" }
            showMessage(getString(Res.string.error_loading_settings, e.message.orEmpty()))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.EditSlidesSettingsScreenViewModel.REFRESH)
        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    // Збереження налаштувань
    private fun saveSettings() = viewModelScope.launch {
        if (networkMonitorProvider.isConnected.value.not()) {
            onEvent(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return@launch
        }

        try {
            _state.update { it.copy(isLoading = true) }
            _isDirty.value = false
            slideRepository.changeSettings(
                id = _state.value.settingsId.orEmpty(),
                slideDuration = _state.value.slideDuration,
                transitionDuration = _state.value.transitionDuration,
                effect = _state.value.transitionEffect,
                autoReloadTime = _state.value.autoReloadTime,
            )
            showMessage(getString(Res.string.settings_saved))
            refreshSettings()
        } catch (e: Exception) {
            Napier.e(tag = TAG, throwable = e) { "Помилка збереження налаштувань" }
            showMessage(getString(Res.string.error_saving, e.message.orEmpty()))
            errorManager.report(message = e.message.orEmpty(), errorCode = ErrorCode.EditSlidesSettingsScreenViewModel.SAVE_DATA)

        } finally {
            _state.update { it.copy(isLoading = false) }
        }
    }

    // Оновлення стану з маркером "брудних" даних
    private fun updateState(
        slideDuration: Int? = null,
        transitionDuration: Int? = null,
        transitionEffect: TransitionEffect? = null,
        autoReloadTime: Int? = null,
    ) {
        _isDirty.value = true
        _state.update { current ->
            current.copy(
                slideDuration = slideDuration ?: current.slideDuration,
                transitionDuration = transitionDuration ?: current.transitionDuration,
                transitionEffect = transitionEffect ?: current.transitionEffect,
                autoReloadTime = autoReloadTime ?: current.autoReloadTime,
            )
        }
        Napier.d(tag = "EditSlidesSettingsScreenViewModel") { "_isDirty:${_isDirty.value}, $slideDuration, $transitionDuration, ${transitionEffect?.name}, $autoReloadTime" }
    }

    // Показ повідомлення користувачу
    private fun showMessage(message: String) = viewModelScope.launch { onEvent(UIEvents.ShowMessage(message)) }

    private fun onEvent(event: UIEvents) = viewModelScope.launch { _uiEvent.emit(event) }
}
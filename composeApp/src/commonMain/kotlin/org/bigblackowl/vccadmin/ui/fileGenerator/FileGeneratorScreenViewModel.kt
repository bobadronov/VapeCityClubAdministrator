package org.bigblackowl.vccadmin.ui.fileGenerator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.errorManager.ErrorCode
import org.bigblackowl.vccadmin.data.errorManager.ErrorManager
import org.bigblackowl.vccadmin.data.repository.CityRepository
import org.bigblackowl.vccadmin.data.repository.FileGeneratorRepository
import org.bigblackowl.vccadmin.data.repository.FileType
import org.bigblackowl.vccadmin.data.repository.FileType.Companion.GLOBAL_TYPES_WITHOUT_SHOPS
import org.bigblackowl.vccadmin.data.repository.FileType.Companion.MONTH_TYPES
import org.bigblackowl.vccadmin.data.repository.NetworkMonitorProvider
import org.bigblackowl.vccadmin.data.repository.ShopRepository
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.bigblackowl.vccadmin.utils.PlatformFileProvider
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_open_file
import vccadministrator.composeapp.generated.resources.no_internet
import vccadministrator.composeapp.generated.resources.not_specified
import vccadministrator.composeapp.generated.resources.select_month
import vccadministrator.composeapp.generated.resources.select_shops
import kotlin.time.Duration.Companion.seconds

class FileGeneratorScreenViewModel(
    private val errorManager: ErrorManager,
    private val cityRepository: CityRepository,
    private val shopRepository: ShopRepository,
    private val repository: FileGeneratorRepository,
    private val networkMonitorProvider: NetworkMonitorProvider,
) : ViewModel() {

    private companion object {
        const val TAG = "FileGeneratorScreenVM"
    }

    private val _effects = MutableSharedFlow<UIEvents>(replay = 0)
    val effects: SharedFlow<UIEvents> = _effects.asSharedFlow()

    private val _uiState = MutableStateFlow(FileGenerationUiState())
    val uiState: StateFlow<FileGenerationUiState> = _uiState.asStateFlow()

    private val gens = FileType.generators(repository)

    fun onIntent(intent: FileGenerationIntent) {
        viewModelScope.launch {
            when (intent) {
                FileGenerationIntent.GoBack -> handleBack()

                FileGenerationIntent.Refresh -> refresh()
                FileGenerationIntent.Init -> init()
                FileGenerationIntent.Exit -> clear()

                is FileGenerationIntent.NavigateTo -> updateState { it.copy(stage = intent.stage) }

                is FileGenerationIntent.ToggleFileType -> updateState { state ->
                    val nextSelected = state.selectedFileTypes.toMutableSet().apply {
                        if (intent.selected) add(intent.fileType) else remove(intent.fileType)
                    }.toSet()

                    state.copy(selectedFileTypes = nextSelected)
                }

                is FileGenerationIntent.ToggleShop -> updateState { state ->
                    val next = state.selectedShopIds.toMutableSet().apply {
                        if (intent.selected) add(intent.shopId) else remove(intent.shopId)
                    }.toSet()

                    state.copy(selectedShopIds = next)
                }

                FileGenerationIntent.SelectAllShops -> updateState { state ->
                    state.copy(selectedShopIds = state.shops.asSequence().map { it.id }.toSet())
                }

                FileGenerationIntent.DeselectAllShops -> updateState { state ->
                    state.copy(selectedShopIds = emptySet())
                }

                FileGenerationIntent.OpenMonthPicker -> updateState { it.copy(showMonthPicker = true) }
                FileGenerationIntent.CloseMonthPicker -> updateState { it.copy(showMonthPicker = false) }

                is FileGenerationIntent.SelectMonth -> updateState {
                    it.copy(selectedMonth = intent.month, showMonthPicker = false)
                }

                FileGenerationIntent.ResetGeneratedFiles -> updateState {
                    it.copy(generatedFiles = emptyList(), progress = 0f)
                }

                FileGenerationIntent.Generate -> generate()

                is FileGenerationIntent.OpenFile -> openFile(intent.fileName)

                FileGenerationIntent.ShareAllFiles -> shareAll()
            }
        }
    }

    private suspend fun handleBack() {
        val current = _uiState.value.stage
        val prev = when (current) {
            FileGenerationStage.SELECT_FILES -> null
            FileGenerationStage.SELECT_SHOPS -> FileGenerationStage.SELECT_FILES
            FileGenerationStage.GENERATED -> {
                if (_uiState.value.needsShops) FileGenerationStage.SELECT_SHOPS
                else FileGenerationStage.SELECT_FILES
            }
        }

        if (prev != null) {
            updateState { it.copy(stage = prev) }
        } else {
            _effects.emit(UIEvents.NavigateBack)
        }
    }

    /**
     * ЄДИНЕ місце, де рахується derived.
     */
    private fun FileGenerationUiState.withDerived(): FileGenerationUiState {
        val requiresMonth = selectedFileTypes.any(MONTH_TYPES::contains)
        val needsShops = selectedFileTypes.any { it !in GLOBAL_TYPES_WITHOUT_SHOPS }

        val monthOk = !requiresMonth || selectedMonth.isNotBlank()
        val fileTypesOk = selectedFileTypes.isNotEmpty()
        val shopsOk = !needsShops || selectedShopIds.isNotEmpty()

        val canGoNextFromFiles = fileTypesOk && monthOk
        val canGenerate = canGoNextFromFiles && shopsOk

        return copy(
            requiresMonth = requiresMonth,
            needsShops = needsShops,
            canGoNextFromFiles = canGoNextFromFiles,
            canGenerate = canGenerate,
        )
    }

    /**
     * Усі оновлення state йдуть через це — derived завжди актуальний.
     */
    private inline fun updateState(transform: (FileGenerationUiState) -> FileGenerationUiState) {
        _uiState.update { state -> transform(state).withDerived() }
    }

    private suspend fun refresh() {
        if (!networkMonitorProvider.isConnected.value) {
            _effects.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return
        }

        updateState { it.copy(isRefreshing = true) }

        runCatching {
            val shopList = shopRepository.getStores()
            val cities = cityRepository.getCities()

            val mappedShops = shopList.map { shop ->
                val city = cities.find { it.id == shop.cityId }
                Shop(
                    id = shop.id,
                    cityName = city?.name ?: getString(Res.string.not_specified),
                    street = shop.street,
                    houseNumber = shop.houseNumber ?: getString(Res.string.not_specified),
                    phoneNumber = shop.phoneNumber ?: getString(Res.string.not_specified),
                    status = shop.status,
                    statusComment = shop.statusComment ?: getString(Res.string.not_specified),
                    cameraCodes = shop.cameraCodes,
                    lastModified = AppStringProvider.formatTimestamp(shop.lastModified),
                    lastModifiedUser = "",
                    internetProvider = shop.internetProvider ?: getString(Res.string.not_specified),
                    internetProviderPersonalAccount = shop.internetProviderPersonalAccount,
                    internetReplenishmentDay = shop.internetReplenishmentDay ?: 0,
                    cityId = shop.cityId,
                    logoUrl = city?.logoUrl ?: getString(Res.string.not_specified),
                    remoteNumber = shop.remoteNumber ?: getString(Res.string.not_specified),
                    addressComment = shop.addressComment ?: getString(Res.string.not_specified),
                    internetReplenishmentAmount = shop.internetReplenishmentAmount.orEmpty(),
                    code = shop.code,
                    deviceType = shop.deviceType,
                )
            }.sortedWith(compareBy({ it.cityName }, { it.street }, { it.houseNumber }))

            updateState { state ->
                val validIds = mappedShops.asSequence().map { it.id }.toSet()
                state.copy(
                    shops = mappedShops,
                    cities = cities,
                    selectedShopIds = state.selectedShopIds.intersect(validIds),
                )
            }
        }.onFailure { e ->
            Napier.e(tag = TAG) { "Refresh error: ${e.message}" }
            errorManager.report(
                message = e.message.orEmpty(),
                errorCode = ErrorCode.FileGeneratorScreenViewModel.LOAD_DATA
            )
        }

        updateState { it.copy(isRefreshing = false) }
    }

    private suspend fun init() {
        // скидаємо стейт, але derived одразу перераховується
        updateState { FileGenerationUiState() }

        if (!networkMonitorProvider.isConnected.value) {
            _effects.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return
        }

        updateState { it.copy(initialLoading = true) }

        runCatching {
            val shopList = shopRepository.getStores()
            val cities = cityRepository.getCities()

            if (shopList.isEmpty() || cities.isEmpty()) {
                Napier.d(tag = TAG) { "Empty lists: shops=${shopList.size}, cities=${cities.size}" }
            }

            val mappedShops = shopList.map { shop ->
                val city = cities.find { it.id == shop.cityId }
                Shop(
                    id = shop.id,
                    cityName = city?.name ?: getString(Res.string.not_specified),
                    street = shop.street,
                    houseNumber = shop.houseNumber ?: getString(Res.string.not_specified),
                    phoneNumber = shop.phoneNumber ?: getString(Res.string.not_specified),
                    status = shop.status,
                    statusComment = shop.statusComment ?: getString(Res.string.not_specified),
                    cameraCodes = shop.cameraCodes,
                    lastModified = AppStringProvider.formatTimestamp(shop.lastModified),
                    lastModifiedUser = "",
                    internetProvider = shop.internetProvider ?: getString(Res.string.not_specified),
                    internetProviderPersonalAccount = shop.internetProviderPersonalAccount,
                    internetReplenishmentDay = shop.internetReplenishmentDay ?: 0,
                    cityId = shop.cityId,
                    logoUrl = city?.logoUrl ?: getString(Res.string.not_specified),
                    remoteNumber = shop.remoteNumber ?: getString(Res.string.not_specified),
                    addressComment = shop.addressComment ?: getString(Res.string.not_specified),
                    internetReplenishmentAmount = shop.internetReplenishmentAmount.orEmpty(),
                    code = shop.code,
                    deviceType = shop.deviceType,
                )
            }.sortedWith(compareBy({ it.cityName }, { it.street }, { it.houseNumber }))

            updateState {
                it.copy(
                    shops = mappedShops,
                    cities = cities,
                    initialLoading = false
                )
            }
        }.onFailure { e ->
            Napier.e(tag = TAG) { "Init error: ${e.message}" }
            updateState { it.copy(initialLoading = false) }
            errorManager.report(
                message = e.message.orEmpty(),
                errorCode = ErrorCode.FileGeneratorScreenViewModel.LOAD_DATA
            )
        }
    }

    private fun clear() {
        _uiState.value = FileGenerationUiState().withDerived()
    }

    private suspend fun generate() {
        val state = _uiState.value

        if (!networkMonitorProvider.isConnected.value) {
            _effects.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
            return
        }

        if (state.selectedFileTypes.isEmpty()) return
        if (state.requiresMonth && state.selectedMonth.isBlank()) {
            _effects.emit(UIEvents.ShowMessage(getString(Res.string.select_month)))
            return
        }
        if (state.needsShops && state.selectedShopIds.isEmpty()) {
            _effects.emit(UIEvents.ShowMessage(getString(Res.string.select_shops)))
            return
        }

        updateState {
            it.copy(
                stage = FileGenerationStage.GENERATED,
                isGenerating = true,
                progress = 0f,
                generatedFiles = emptyList()
            )
        }

        val shopIds = state.selectedShopIds.toList()
        val month = if (state.requiresMonth) state.selectedMonth else ""
        val fileTypes = state.selectedFileTypes.toList()

        generateFiles(shopIds, month, fileTypes)

        updateState { it.copy(isGenerating = false) }

        downloadGeneratedFiles()
    }

    private suspend fun generateFiles(shopIds: List<String>, month: String, fileTypes: List<FileType>) {
        val totalTasks = fileTypes.sumOf { type ->
            val gen = gens[type] ?: return@sumOf 0
            if (gen.isGlobal) 1 else shopIds.size
        }.coerceAtLeast(1)

        var completed = 0

        suspend fun updateProgress() {
            completed += 1
            updateState { it.copy(progress = completed / totalTasks.toFloat()) }
            delay(200)
        }

        fileTypes.forEach { type ->
            val gen = gens[type] ?: return@forEach

            if (gen.isGlobal) {
                val name = gen.getFileName(
                    shop = null,
                    shopIds = if (type == FileType.SHOP_PHONES_LIST) shopIds else null,
                    month = if (type.requiresMonth()) month else null
                )

                tryWithRetry(name, null, if (type.requiresMonth()) month else null, type) {
                    val file = gen.generate(
                        shop = null,
                        shopIds = if (type == FileType.SHOP_PHONES_LIST) shopIds else null,
                        month = if (type.requiresMonth()) month else null
                    )
                    if (file != null) addGenerated(GeneratedFile(file.name, file.content, null))
                    updateProgress()
                }
            } else {
                shopIds.forEach { id ->
                    val shop = _uiState.value.shops.find { it.id == id } ?: return@forEach
                    val name = gen.getFileName(shop, null, month)

                    tryWithRetry(name, id, month, type) {
                        val file = gen.generate(shop, null, month)
                        if (file != null) addGenerated(GeneratedFile(file.name, file.content, null))
                        updateProgress()
                    }
                }
            }
        }
    }

    private fun FileType.requiresMonth(): Boolean = this in MONTH_TYPES

    private fun addGenerated(file: GeneratedFile) {
        updateState { state ->
            state.copy(generatedFiles = state.generatedFiles + file)
        }
    }

    @Suppress("unused")
    private suspend fun tryWithRetry(
        fileName: String,
        shopId: String?,
        month: String?,
        fileType: FileType,
        block: suspend () -> Unit
    ) {
        repeat(3) { attempt ->
            try {
                block()
                return
            } catch (e: Exception) {
                Napier.e(tag = TAG) { "Generate error $fileName (attempt ${attempt + 1}): ${e.message}" }
                if (attempt == 2) {
                    addGenerated(GeneratedFile(fileName, null, e.message))
                }
                errorManager.report(
                    message = e.message.orEmpty(),
                    errorCode = ErrorCode.FileGeneratorScreenViewModel.FILE_GENERATION
                )
                delay(2.seconds)
            }
        }
    }

    private fun downloadGeneratedFiles() {
        viewModelScope.launch {
            if (!networkMonitorProvider.isConnected.value) {
                _effects.emit(UIEvents.ShowMessage(getString(Res.string.no_internet)))
                return@launch
            }

            val successful = _uiState.value.generatedFiles.filter { it.content != null }
            if (successful.isEmpty()) return@launch

            successful.forEach { file ->
                PlatformFileProvider.downloadFile(file.name, file.content!!)
                delay(2.seconds)
            }
        }
    }

    private suspend fun openFile(fileName: String) {
        runCatching { PlatformFileProvider.openFile(fileName) }
            .onFailure { e ->
                Napier.e(tag = TAG) { "Open file error: ${e.message}" }
                _effects.emit(UIEvents.ShowMessage(getString(Res.string.error_open_file)))
                errorManager.report(
                    message = e.message.orEmpty(),
                    errorCode = ErrorCode.FileGeneratorScreenViewModel.OPEN_FILE
                )
            }
    }

    private suspend fun shareAll() {
        val result = PlatformFileProvider.shareFilesAsZip(_uiState.value.generatedFiles)
        _effects.emit(UIEvents.ShowMessage(result.message))
    }
}

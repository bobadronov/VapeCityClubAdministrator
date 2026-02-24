package org.bigblackowl.vccadmin.ui.workSchedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.toByteString
import org.bigblackowl.vccadmin.data.entity.SupabaseShop
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.events.UIEvents
import org.bigblackowl.vccadmin.domain.repository.AuthRepository
import org.bigblackowl.vccadmin.domain.repository.ShopRepository

@Serializable
data class WorkScheduleDto(
    val dates: List<String>,                 // ISO: yyyy-MM-dd
    val shops: List<ShopScheduleDto>
)

@Serializable
data class ShopScheduleDto(
    val shop: String,
    val shifts: List<ShiftDto>
)

@Serializable
data class ShiftDto(
    val date: String,                        // ISO
    val employees: List<String>
)

@Serializable
data class ParseScheduleRequest(
    val fileName: String,
    val base64: String
)

data class WorkScheduleUiState(
    val isInitialLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val currentUser: User? = null,
    val schedule: WorkScheduleDto? = null,
    val rows: List<MatchedShopRow> = emptyList(),
)

@Serializable
data class MatchedShopRow(
    val rawShop: String,
    val matchedShopId: String? = null,
    val matchedCode: String? = null,
    val matchedAddress: String? = null,
    val score: Int = 0, // 0..100
    val shifts: List<ShiftDto>,
    val groupTitle: String? = null, // якщо це “група/місто”
)

class WorkScheduleScreenViewModel(
    private val json: Json,
    private val supabase: SupabaseClient,
    private val authRepository: AuthRepository,
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkScheduleUiState())
    val uiState: StateFlow<WorkScheduleUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<UIEvents>(replay = 0)
    val uiEvent: SharedFlow<UIEvents> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    fun onIntent(intent: WorkScheduleIntent) {
        when (intent) {
            WorkScheduleIntent.Load -> load()
        }
    }

    private fun load() = viewModelScope.launch {
        _uiState.update { it.copy(isInitialLoading = true, schedule = null) }

        try {
            val file = FileKit.openFilePicker(
                type = FileKitType.File(extensions = listOf("xls", "xlsx"))
            )

            if (file == null) {
                _uiState.update { it.copy(isInitialLoading = false) }
                return@launch
            }

            val bytes = file.readBytes()
            val fileName = file.name

            val schedule = parseExcel(fileName, bytes)
            val allStores = shopRepository.getStores()
            Napier.d { allStores.toString() }

            val rows = enrichSchedule(
                schedule = schedule,
                allStores = allStores,
                cityIdByTitle = emptyMap(), // або cityIdByTitle
            )

            _uiState.update {
                it.copy(
                    isInitialLoading = false,
                    schedule = schedule,
                    rows = rows
                )
            }

        } catch (t: Throwable) {
            _uiState.update { it.copy(isInitialLoading = false) }
            Napier.d { t.message ?: "Помилка парсингу Excel" }
            showMessage(t.message ?: "Помилка парсингу Excel")
        }
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace('’', '\'')
            .replace(Regex("[^a-zа-яіїєґ0-9\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val dp = IntArray(b.length + 1) { it }
        for (i in a.indices) {
            var prev = dp[0]
            dp[0] = i + 1
            for (j in b.indices) {
                val temp = dp[j + 1]
                val cost = if (a[i] == b[j]) 0 else 1
                dp[j + 1] = minOf(
                    dp[j + 1] + 1,     // deletion
                    dp[j] + 1,         // insertion
                    prev + cost        // substitution
                )
                prev = temp
            }
        }
        return dp[b.length]
    }

    private fun enrichSchedule(
        schedule: WorkScheduleDto,
        allStores: List<SupabaseShop>,
        cityIdByTitle: Map<String, Int> = emptyMap(),
    ): List<MatchedShopRow> {

        val threshold: Int = 55

        var currentGroup: String? = null
        var currentCityId: Int? = null

        return schedule.shops.map { row ->
            if (row.isGroupRow()) {
                currentGroup = row.shop.trim()
                currentCityId = cityIdByTitle[normalize(currentGroup)]
                return@map MatchedShopRow(
                    rawShop = row.shop,
                    groupTitle = currentGroup,
                    shifts = row.shifts,
                    score = 0
                )
            }

            val candidates = if (currentCityId != null) {
                allStores.filter { it.cityId == currentCityId }
            } else {
                allStores
            }

            val q = row.shop
            val best = candidates
                .asSequence()
                .map { shop ->
                    val score = totalScore(q, shop)   // <- тут
                    shop to score
                }
                .maxByOrNull { it.second }

            val (bestShop, bestScore) = best ?: (null to 0)

            if (bestShop != null && bestScore >= threshold) {
                MatchedShopRow(
                    rawShop = row.shop,
                    matchedShopId = bestShop.id,
                    matchedCode = bestShop.code,
                    matchedAddress = "${bestShop.street} ${bestShop.houseNumber.orEmpty()}",
                    score = bestScore,
                    shifts = row.shifts,
                    groupTitle = currentGroup
                )
            } else {
                MatchedShopRow(
                    rawShop = row.shop,
                    matchedShopId = null,
                    matchedCode = null,
                    matchedAddress = null,
                    score = bestScore,
                    shifts = row.shifts,
                    groupTitle = currentGroup
                )
            }
        }
    }

    private fun tokenOverlapScore(a: String, b: String): Int {
        val ta = tokenize(a)
        val tb = tokenize(b)
        if (ta.isEmpty() || tb.isEmpty()) return 0
        val inter = ta.intersect(tb).size
        val union = ta.union(tb).size
        return ((inter.toDouble() / union.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun similarityRatio(a: String, b: String): Int {
        val na = normalize(a)
        val nb = normalize(b)
        if (na.isEmpty() || nb.isEmpty()) return 0
        val dist = levenshtein(na, nb)
        val maxLen = maxOf(na.length, nb.length)
        return ((1.0 - dist.toDouble() / maxLen.toDouble()) * 100).toInt().coerceIn(0, 100)
    }

    private fun tokenize(s: String): Set<String> = normalize(s).split(" ").filter { it.isNotBlank() }.toSet()
    private fun ShopScheduleDto.isGroupRow(): Boolean {
        val allEmpty = shifts.all { it.employees.isEmpty() }
        if (!allEmpty) return false

        val s = rawShopLikeGroup(shop)
        return s
    }

    private fun rawShopLikeGroup(text: String): Boolean {
        val n = normalize(text)
        if (n.isBlank()) return false
        if (n.any { it.isDigit() }) return false
        // 1-2 слова — типово для "Бровари", "Ірпінь", "Вишневе"
        val words = n.split(" ").filter { it.isNotBlank() }
        return words.size <= 2
    }

    private fun SupabaseShop.candidateStreet(): String =
        buildString {
            append(street)
            houseNumber?.let { append(" ").append(it) }
        }

    private fun totalScore(query: String, shop: SupabaseShop): Int {
        val q = normalize(query)

        val streetScore = maxOf(
            similarityRatio(q, shop.candidateStreet()),
            tokenOverlapScore(q, shop.candidateStreet())
        )

        val codeScore = if (q.contains(shop.code.lowercase())) 100 else 0

        // якщо query дуже короткий ("Ашан", "Дарниця") — streetScore буде низький,
        // тож кодScore може допомогти лише якщо код є в query (не твій кейс)
        // Тому основа — streetScore.
        return (streetScore * 0.9 + codeScore * 0.1).toInt().coerceIn(0, 100)
    }

    private suspend fun parseExcel(
        fileName: String,
        bytes: ByteArray
    ): WorkScheduleDto {
        val body = ParseScheduleRequest(
            fileName = fileName,
            base64 = bytes.encodeBase64()
        )
        val response = invokeFunction("parse-work-schedule", body).bodyAsText()
        Napier.d { response }
        return json.decodeFromString(response)
    }

    /**
     * Викликає Supabase Edge Function з вказаним тілом запиту.
     */
    @OptIn(InternalAPI::class)
    private suspend inline fun <reified T> invokeFunction(function: String, body: T): HttpResponse {
        val response = supabase.functions.invoke(function) {
            this.body = json.encodeToString(body)
            contentType(ContentType.Application.Json)
        }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException(response.bodyAsText())
        }
        return response
    }

    private fun ByteArray.encodeBase64(): String = toByteString().base64()

    private fun showMessage(message: String) = viewModelScope.launch {
        _uiEvent.emit(UIEvents.ShowMessage(message))
    }
}

sealed interface WorkScheduleIntent {
    data object Load : WorkScheduleIntent
}

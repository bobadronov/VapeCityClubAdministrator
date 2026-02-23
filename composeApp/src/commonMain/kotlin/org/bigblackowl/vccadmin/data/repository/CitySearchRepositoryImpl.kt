package org.bigblackowl.vccadmin.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.entity.SuggestionCityDto
import org.bigblackowl.vccadmin.domain.repository.CitySearchRepository
import vccadministrator.composeapp.generated.resources.Res
import kotlin.concurrent.Volatile

class CitySearchRepositoryImpl(
    private val json: Json,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : CitySearchRepository {
    private val resourcePath: String = "files/sorted_city_list.json"

    private data class CityEntry(
        val dto: SuggestionCityDto,
        val nameNorm: String,
        val oblastNorm: String,
    )

    private val mutex = Mutex()

    @Volatile
    private var cache: List<CityEntry>? = null

    override suspend fun preload() {
        ensureLoaded()
    }

    override suspend fun search(query: String, limit: Int): List<SuggestionCityDto> = withContext(dispatcher) {
        val q = normalizeUa(query)
        val entries = ensureLoaded()

        if (q.isBlank()) return@withContext emptyList()

        // 1) prefix matches (найкращий UX)
        val prefix = ArrayList<SuggestionCityDto>(limit)
        for (e in entries) {
            if (e.nameNorm.startsWith(q)) {
                prefix.add(e.dto)
                if (prefix.size >= limit) return@withContext prefix
            }
        }

        // 2) contains matches (як fallback)
        val contains = ArrayList<SuggestionCityDto>(limit - prefix.size)
        for (e in entries) {
            if (e.nameNorm.contains(q, ignoreCase = true) || e.oblastNorm.contains(q, ignoreCase = true)) {
                // щоб не дублювати те, що вже є в prefix
                // (prefix завжди subset, але на випадок інших правил)
                if (!prefix.any { it.name == e.dto.name && it.oblast == e.dto.oblast }) {
                    contains.add(e.dto)
                    if (prefix.size + contains.size >= limit) break
                }
            }
        }

        prefix + contains
    }

    private suspend fun ensureLoaded(): List<CityEntry> {
        cache?.let { return it }

        return mutex.withLock {
            cache?.let { return it }

            val bytes = Res.readBytes(resourcePath)
            val text = bytes.decodeToString()

            val list = json.decodeFromString<List<SuggestionCityDto>>(text)

            // Важливо: кешуємо нормалізовані поля для швидкого пошуку
            val entries = list.map { dto ->
                CityEntry(
                    dto = dto,
                    nameNorm = normalizeUa(dto.name),
                    oblastNorm = normalizeUa(dto.oblast)
                )
            }

            cache = entries
            entries
        }
    }

    private fun normalizeUa(s: String): String =
        s.trim()
            .lowercase()
            .replace('’', '\'')
            .replace('ʼ', '\'')
            .replace('`', '\'')
}


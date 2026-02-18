package org.bigblackowl.vccadmin.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.repository.CityDto
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import vccadministrator.composeapp.generated.resources.Res

@Preview
@Composable
private fun PlaygroundPreview1() = PreviewDarkMaterialTheme {

    val items by produceState(initialValue = emptyList()) { value = loadCitiesSortedByName() }
    var filter by remember { mutableStateOf("") }
    val query = filter.trim()

    val mappedList: List<OblastGroup> = items
        .groupBy { it.oblast }
        .toList()
        .sortedWith(compareBy(UkrainianStringComparator) { it.first })
        .map { (oblast, cities) ->
            val sortedCities = cities.sortedWith(compareBy(UkrainianStringComparator) { it.name })
            val filteredCities = if (query.isBlank()) {
                sortedCities
            } else {
                sortedCities.filter { city -> city.name.contains(query, ignoreCase = true) }
            }
            OblastGroup(oblast = oblast, cities = filteredCities)
        }
        .filter { it.cities.isNotEmpty() } // прибираємо області без збігів

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = filter,
            onValueChange = { filter = it },
            modifier = Modifier.fillMaxWidth()
        )
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            mappedList.forEach { group ->

                stickyHeader(key = "h:${group.oblast}") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = group.oblast,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(DefaultValues.Padding.cardContentPadding)
                        )
                    }
                }

                items(
                    items = group.cities,
                    key = { city -> "${city.oblast}|${city.name}" }
                ) { city ->
                    OutlinedCard(onClick = {
                        filter = city.name
                    }, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(DefaultValues.Padding.cardContentPadding)) {
                            Text(city.name, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}

private val UA_ALPHABET = listOf(
    'а', 'б', 'в', 'г', 'ґ', 'д', 'е', 'є', 'ж', 'з', 'и', 'і', 'ї', 'й',
    'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х', 'ц', 'ч', 'ш', 'щ', 'ь', 'ю', 'я'
)

private val UA_RANK: Map<Char, Int> = buildMap {
    UA_ALPHABET.forEachIndexed { idx, ch ->
        put(ch, idx)
        put(ch.uppercaseChar(), idx)
    }
}

private fun normalizeUa(s: String): String =
    s.trim()
        .lowercase()
        .replace('’', '\'')
        .replace('ʼ', '\'')
        .replace('`', '\'')


private fun isIgnorable(ch: Char): Boolean =
    ch.isWhitespace() || ch == '-' || ch == '–' || ch == '—' || ch == '\'' || ch == '.'
private fun nextComparableChar(s: String, start: Int): Pair<Int, Char?> {
    var i = start
    while (i < s.length && isIgnorable(s[i])) i++
    return if (i >= s.length) i to null else i to s[i]
}

private fun uaCompare(a0: String, b0: String): Int {
    val a = normalizeUa(a0)
    val b = normalizeUa(b0)

    var ia = 0
    var ib = 0

    while (true) {
        val (na, ca) = nextComparableChar(a, ia)
        val (nb, cb) = nextComparableChar(b, ib)

        ia = na + 1
        ib = nb + 1

        if (ca == null && cb == null) return 0
        if (ca == null) return -1
        if (cb == null) return 1

        val ra = UA_RANK[ca]
        val rb = UA_RANK[cb]

        val rankA = when {
            ra != null -> ra
            ca.isDigit() -> 10_000 + ca.code
            else -> 20_000 + ca.code
        }
        val rankB = when {
            rb != null -> rb
            cb.isDigit() -> 10_000 + cb.code
            else -> 20_000 + cb.code
        }

        val diff = rankA - rankB
        if (diff != 0) return diff
    }
}

val UkrainianStringComparator: Comparator<String> = Comparator { a, b -> uaCompare(a, b) }


data class OblastGroup(val oblast: String, val cities: List<CityDto>)

private var json = Json { ignoreUnknownKeys = true }

private suspend fun loadCitiesSortedByName(): List<CityDto> = withContext(Dispatchers.Default) {
    val bytes = Res.readBytes("files/sorted_city_list.json")
    val jsonText = bytes.decodeToString()
    json.decodeFromString<List<CityDto>>(jsonText)
}
package org.bigblackowl.vccadmin.domain.repository

import org.bigblackowl.vccadmin.data.entity.SuggestionCityDto

interface CitySearchRepository {
    suspend fun preload()
    suspend fun search(query: String, limit: Int = 20): List<SuggestionCityDto>
}
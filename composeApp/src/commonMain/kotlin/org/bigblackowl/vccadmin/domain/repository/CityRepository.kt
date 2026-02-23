package org.bigblackowl.vccadmin.domain.repository

import io.github.vinceglb.filekit.PlatformFile
import org.bigblackowl.vccadmin.data.entity.City

interface CityRepository {
    suspend fun getCities(): List<City>
    suspend fun addCity(newCityName: String, newCityLogoFile: PlatformFile?)
    suspend fun updateCity(city: City, newCityName: String, newCityLogoFile: PlatformFile?)
    suspend fun deleteCity(city: City)
}
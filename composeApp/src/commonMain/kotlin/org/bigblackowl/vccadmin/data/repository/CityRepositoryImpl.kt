package org.bigblackowl.vccadmin.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.storage.storage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.domain.repository.CityRepository
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.failed_retrieve_city
import kotlin.time.Duration

class CityRepositoryImpl(private val supabase: SupabaseClient) : CityRepository {

    companion object {
        private const val COLUMN_NAME: String = "name"
        private const val CITIES_TABLE = "cities"
        private const val CITY_LOGOS_BUCKET = "city-logos"
        private const val COLUMN_ID = "id"
    }

    private val cityTable = supabase.postgrest.from(CITIES_TABLE)
    private val storage = supabase.storage.from(CITY_LOGOS_BUCKET)

    override suspend fun getCities(): List<City> {
        return cityTable.select().decodeList<City>()
    }

    override suspend fun addCity(newCityName: String, newCityLogoFile: PlatformFile?) {
        @Serializable
        data class NewCity(
            @SerialName("name") val name: String,
            @SerialName("last_modified") val lastModified: Long,
            @SerialName("last_modified_user_id") val lastModifiedUserId: String?, // Nullable UUID
            @SerialName("logo_url") val logoUrl: String? = null,
        )

        val newCity = NewCity(
            name = newCityName, logoUrl = null, lastModified = DefaultValues.Time.now, lastModifiedUserId = supabase.auth.currentUserOrNull()?.id
        )

        val insertedCity = cityTable.insert(newCity) {
            select {
                filter { eq(COLUMN_NAME, newCityName) }
                limit(1)
            }
        }.decodeSingleOrNull<City>() ?: throw Exception(getString(Res.string.failed_retrieve_city))
        val logoUrl = newCityLogoFile?.let {
            val fileName = "city_${insertedCity.id}.${it.extension}"
            storage.upload(fileName, it.readBytes()) { upsert = true }
            delay(200)
            storage.createSignedUrl(fileName, expiresIn = Duration.INFINITE)
        }
        val updatedCity = insertedCity.copy(
            logoUrl = logoUrl, lastModified = DefaultValues.Time.now, lastModifiedUserId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        )
        cityTable.update(updatedCity) { filter { eq(COLUMN_ID, insertedCity.id) } }
    }

    override suspend fun updateCity(city: City, newCityName: String, newCityLogoFile: PlatformFile?) {
        val currentUserId = supabase.auth.currentUserOrNull()?.id.orEmpty()
        val logoUrl = newCityLogoFile?.let {
            val fileName = "city_${city.id}.${it.extension}"
            storage.upload(fileName, it.readBytes()) { upsert = true }
            storage.createSignedUrl(fileName, expiresIn = Duration.INFINITE)
        } ?: city.logoUrl
        val updatedCity = city.copy(
            name = newCityName, logoUrl = logoUrl, lastModified = DefaultValues.Time.now, lastModifiedUserId = currentUserId
        )
        cityTable.update(updatedCity) { filter { eq(COLUMN_ID, city.id) } }
    }

    override suspend fun deleteCity(city: City) {
        cityTable.delete { filter { eq(COLUMN_ID, city.id) } }
        city.logoUrl?.substringAfterLast("/")?.substringBefore("?")?.let { fileName ->
            storage.delete(fileName)
        }
    }

}
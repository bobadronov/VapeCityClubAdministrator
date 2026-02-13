package org.bigblackowl.vccadmin.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.data.entity.SlideSettings
import org.bigblackowl.vccadmin.data.entity.SupabaseSlide
import org.bigblackowl.vccadmin.data.entity.TransitionEffect
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.positions_slides_mismatch
import kotlin.time.ExperimentalTime

interface SlideRepository {
    suspend fun getSlides(): List<SupabaseSlide>
    suspend fun getSlideById(slideId: String): SupabaseSlide
    suspend fun toggleSlideVisibility(slideId: String)
    suspend fun deleteSlide(slideId: String)
    suspend fun changeSlidePosition(positions: List<Int>)
    suspend fun downloadSlideIcon(slidesName: String): ByteArray
    suspend fun addSlide(
        fileName: String,
        data: ByteArray,
        shopCodes: List<String>,
        isActive: Boolean = true
    )

    suspend fun updateSlide(
        slideId: String,
        fileName: String,
        data: ByteArray?,
        shopCodes: List<String>,
        isActive: Boolean
    )

    suspend fun changeSettings(id: String, slideDuration: Int, transitionDuration: Int, effect: TransitionEffect, autoReloadTime: Int)
    suspend fun getSlidesSettings(): SlideSettings
    suspend fun getSlides(
        shopCode: String? = null,
        onlyActive: Boolean? = null,
        nameLike: String? = null,
        orderByPosition: Boolean = true,
    ): List<SupabaseSlide>
}

class SlideRepositoryImpl(private val supabase: SupabaseClient) : SlideRepository {
    private companion object {
        private const val FILE_BUCKET_NAME = "slides-file"
        private const val SLIDES_DB = "new_slides"
        private const val SLIDE_SETTINGS_TABLE = "slides_settings"
        private const val COLUMN_ID = "id"
    }

    private fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override suspend fun getSlides(): List<SupabaseSlide> =
        supabase.from(SLIDES_DB).select().decodeList<SupabaseSlide>()

    override suspend fun getSlideById(slideId: String): SupabaseSlide =
        supabase.from(SLIDES_DB).select {
            filter { eq(COLUMN_ID, slideId) }
        }.decodeSingle<SupabaseSlide>()

    @OptIn(ExperimentalTime::class)
    override suspend fun toggleSlideVisibility(slideId: String) {
        val current = getSlideById(slideId)
        val currentTime = DefaultValues.Time.now
        val updateData = SlideVisibilityUpdate(
            is_active = !current.isActive,
            last_modified = currentTime,
            last_modified_user_id = getCurrentUserId()
        )

        supabase.from(SLIDES_DB).update(updateData) {
            filter { eq(COLUMN_ID, slideId) }
        }
    }

    override suspend fun deleteSlide(slideId: String) {
        val slide = getSlideById(slideId)
        supabase.storage.from(FILE_BUCKET_NAME).delete(slide.fileName)
        supabase.from(SLIDES_DB).delete {
            filter { eq(COLUMN_ID, slideId) }
        }
    }

    @OptIn(ExperimentalTime::class)
    override suspend fun changeSlidePosition(positions: List<Int>) {
        val slides = getSlides().sortedBy { it.position }
        if (positions.size != slides.size) throw IllegalArgumentException(getString(Res.string.positions_slides_mismatch))

        val currentTime = DefaultValues.Time.now
        val updateData = SlidePositionUpdate(
            position = 0, // значення буде перезаписане в циклі
            last_modified = currentTime,
            last_modified_user_id = getCurrentUserId()
        )

        slides.forEachIndexed { index, slide ->
            supabase.from(SLIDES_DB).update(updateData.copy(position = positions[index])) {
                filter { eq(COLUMN_ID, slide.id) }
            }
        }

        // require(ids.size == newPositions.size) { "IDs and positions must have same size" }
        //
        //        @kotlinx.serialization.Serializable
        //        data class ReorderSlidesRequest(
        //            val p_ids: List<String>,  // List of UUIDs as String
        //            val p_positions: List<Int>,
        //        )
        //
        //        val payload = ReorderSlidesRequest(ids, newPositions)
        //        client.postgrest.rpc("reorder_slides", payload) // Supabase RPC автоматично конвертує uuid[]
    }

    override suspend fun downloadSlideIcon(slidesName: String): ByteArray {
        val bucket = supabase.storage.from(FILE_BUCKET_NAME)
       return bucket.downloadPublic(slidesName)
    }

    override suspend fun addSlide(
        fileName: String,
        data: ByteArray,
        shopCodes: List<String>,
        isActive: Boolean
    ) {
        val bucket = supabase.storage.from(FILE_BUCKET_NAME)
        bucket.upload(fileName, data) { upsert = true }

        val publicUrl = bucket.publicUrl(fileName)
        val currentTime = DefaultValues.Time.now

        val insertData = SlideInsert(
            file_name = fileName,
            public_url = publicUrl,
            shop_codes = shopCodes,
            is_active = isActive,
            last_modified = currentTime,
            last_modified_user_id = getCurrentUserId(),
        )

        supabase.from(SLIDES_DB).insert(insertData)
    }

    override suspend fun updateSlide(
        slideId: String,
        fileName: String,
        data: ByteArray?,
        shopCodes: List<String>,
        isActive: Boolean
    ) {
        val current = getSlideById(slideId)
        val bucket = supabase.storage.from(FILE_BUCKET_NAME)
        var publicUrl = current.publicUrl
        val oldFileName = current.fileName
        val currentTime = DefaultValues.Time.now

        // Логіка завантаження/перейменування файлу
        if (data != null) {
            bucket.upload(fileName, data) { upsert = true }
            publicUrl = bucket.publicUrl(fileName)
            if (fileName != oldFileName) {
                bucket.delete(oldFileName)
            }
        } else if (fileName != oldFileName) {
            val existingData = bucket.downloadPublic(oldFileName)
            bucket.upload(fileName, existingData) { upsert = true }
            publicUrl = bucket.publicUrl(fileName)
            bucket.delete(oldFileName)
        }

        val updateData = SlideUpdate(
            file_name = fileName,
            public_url = publicUrl,
            shop_codes = shopCodes,
            is_active = isActive,
            last_modified = currentTime,
            last_modified_user_id = getCurrentUserId()
        )

        supabase.from(SLIDES_DB).update(updateData) {
            filter { eq(COLUMN_ID, slideId) }
        }
    }

    override suspend fun changeSettings(
        id: String,
        slideDuration: Int,
        transitionDuration: Int,
        effect: TransitionEffect,
        autoReloadTime: Int
    ) {
        @Serializable
        data class NewSlideAppSettings(
            @SerialName("slide_duration") val slideDuration: Int,
            // час анімації переходу між слайдами (с)
            @SerialName("transition_duration") val transitionDuration: Int,
            // ефект переходу
            @SerialName("transition_effect") val transitionEffect: TransitionEffect,
            @SerialName("auto_reload_time") val autoReloadTime: Int,
            @SerialName("last_modified") val lastModified: Long,
            @SerialName("last_modified_user_id") val lastModifiedByUser: String? = null,
        )

        val currentUserId = supabase.auth.currentUserOrNull()?.id
        val update = NewSlideAppSettings(
            slideDuration = slideDuration,
            transitionDuration = transitionDuration,
            transitionEffect = effect,
            autoReloadTime = autoReloadTime,
            lastModified = DefaultValues.Time.now,
            lastModifiedByUser = currentUserId
        )

        supabase.from(SLIDE_SETTINGS_TABLE).update(update) {
            filter {
                eq(COLUMN_ID, id)
            }
        }
    }

    override suspend fun getSlides(
        shopCode: String?,
        onlyActive: Boolean?,
        nameLike: String?,
        orderByPosition: Boolean,
    ): List<SupabaseSlide> {
        return supabase.postgrest.from(SLIDES_DB).select {
            filter {
                if (shopCode != null) contains("shop_codes", listOf(shopCode))
                if (onlyActive != null) eq("is_active", onlyActive)
                if (nameLike != null) ilike("file_name", "%$nameLike%")
            }
            if (orderByPosition) order("position", Order.ASCENDING)
        }.decodeList<SupabaseSlide>()
    }

    override suspend fun getSlidesSettings(): SlideSettings = supabase.from(SLIDE_SETTINGS_TABLE).select().decodeSingle<SlideSettings>()

}

// Локальні датакласи для часткових оновлень/вставок
@Suppress("PropertyName")
@Serializable
private data class SlideVisibilityUpdate(
    val is_active: Boolean,
    val last_modified: Long,
    val last_modified_user_id: String?
)

@Suppress("PropertyName")
@Serializable
private data class SlidePositionUpdate(
    val position: Int,
    val last_modified: Long,
    val last_modified_user_id: String?
)

@Suppress("PropertyName")
@Serializable
private data class SlideInsert(
    val file_name: String,
    val public_url: String,
    val shop_codes: List<String>,
    val is_active: Boolean,
    val last_modified: Long,
    val last_modified_user_id: String?,
)

@Suppress("PropertyName")
@Serializable
private data class SlideUpdate(
    val file_name: String,
    val public_url: String,
    val shop_codes: List<String>,
    val is_active: Boolean,
    val last_modified: Long,
    val last_modified_user_id: String?
)
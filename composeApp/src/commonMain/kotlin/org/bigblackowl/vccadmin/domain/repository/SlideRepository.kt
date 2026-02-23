package org.bigblackowl.vccadmin.domain.repository

import org.bigblackowl.vccadmin.data.entity.SlideSettings
import org.bigblackowl.vccadmin.data.entity.SupabaseSlide
import org.bigblackowl.vccadmin.data.entity.TransitionEffect

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
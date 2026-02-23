package org.bigblackowl.vccadmin.domain.repository

import io.ktor.client.statement.HttpResponse

// Інтерфейс репозиторію для генерації файлів (без змін, у стилі топових проектів: чітка структура, документація)
interface FileGeneratorRepository {
    /**
     * Генерує PDF-список телефонів для вказаних магазинів.
     */
    suspend fun generatePhoneListPdf(shopIds: List<String>): HttpResponse

    /**
     * Генерує детальний PDF для магазину з вказаними значеннями.
     */
    suspend fun generateShopDetailPdf(shopId: String, value: List<List<String>>): HttpResponse

    /**
     * Генерує PDF-графік прибирання для магазину на вказаний місяць.
     */
    suspend fun generateCleaningSchedulePdf(shopId: String, month: String): HttpResponse

    /**
     * Генерує PDF-графік інкасації для магазину на вказаний місяць.
     */
    suspend fun generateCollectionSchedulePdf(shopId: String, month: String): HttpResponse

    /**
     * Отримує PDF з правилами прибирання.
     */
    suspend fun getCleaningsRules(): ByteArray

    /**
     * Отримує PDF для повернення товарів.
     */
    suspend fun getReturningOfGoods(): ByteArray

    /**
     * Отримує PDF для залишків на складі.
     */
    suspend fun getStoreLeftovers(): ByteArray
}
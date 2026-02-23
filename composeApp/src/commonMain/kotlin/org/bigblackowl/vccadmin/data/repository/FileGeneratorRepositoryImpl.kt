package org.bigblackowl.vccadmin.data.repository

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.storage.storage
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.domain.repository.FileGeneratorRepository
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.error_loading_file

// Імплементація репозиторію з використанням Supabase (вичищено дублі, додано override, логування та обробку помилок)
class FileGeneratorRepositoryImpl(
    private val supabase: SupabaseClient
) : FileGeneratorRepository {

    companion object {
        private const val TAG = "FileGeneratorRepository"

        private const val PDF_FILES_BUCKET = "pdf-files"

        private const val GENERATE_PHONE_LIST_FUNCTION = "generate_phone_list_pdf"
        private const val GENERATE_SHOP_DETAIL_FUNCTION = "generate_shop_detail_pdf"
        private const val GENERATE_CLEANING_SCHEDULE_FUNCTION = "generate_cleaning_schedule_pdf"
        private const val GENERATE_COLLECTION_SCHEDULE_FUNCTION = "generate_collection_schedule_pdf"

        private const val CLEANINGS_RULE_FILE_NAME = "cleanings_rule.pdf"
        private const val RETURNING_OF_GOODS_FILE_NAME = "application_for_return_of_goods.pdf"
        private const val STORE_LEFTOVERS_FILE_NAME = "store_leftovers.pdf"

    }

    override suspend fun generatePhoneListPdf(shopIds: List<String>): HttpResponse {
        try {
            @Serializable
            data class PhoneListRequest(val shopIds: List<String>)
            return invokeFunction(GENERATE_PHONE_LIST_FUNCTION, PhoneListRequest(shopIds))
        } catch (e: Exception) {
            Napier.e("Помилка мережі в generatePhoneListPdf: ${e.message.orEmpty()}", tag = TAG)
            throw e
        }
    }

    override suspend fun generateShopDetailPdf(shopId: String, value: List<List<String>>): HttpResponse {
        try {
            @Serializable
            data class ShopDetailRequest(val shopId: String, val value: List<List<String>>)
            return invokeFunction(GENERATE_SHOP_DETAIL_FUNCTION, ShopDetailRequest(shopId, value))
        } catch (e: Exception) {
            Napier.e("Помилка мережі в generateShopDetailPdf: ${e.message.orEmpty()}", tag = TAG)
            throw e
        }
    }

    override suspend fun generateCleaningSchedulePdf(shopId: String, month: String): HttpResponse {
        try {
            @Serializable
            data class CleaningRequest(val shopId: String, val month: String)
            return invokeFunction(GENERATE_CLEANING_SCHEDULE_FUNCTION, CleaningRequest(shopId, month))
        } catch (e: Exception) {
            Napier.e("Помилка мережі в generateCleaningSchedulePdf: ${e.message.orEmpty()}", tag = TAG)
            throw e
        }
    }

    override suspend fun generateCollectionSchedulePdf(shopId: String, month: String): HttpResponse {
        try {
            @Serializable
            data class CollectionRequest(val shopId: String, val month: String)
            return invokeFunction(GENERATE_COLLECTION_SCHEDULE_FUNCTION, CollectionRequest(shopId, month))
        } catch (e: Exception) {
            Napier.e("Помилка мережі в generateCollectionSchedulePdf: ${e.message.orEmpty()}", tag = TAG)
            throw e
        }
    }

    override suspend fun getCleaningsRules(): ByteArray {
        try {
            return supabase.storage.from(PDF_FILES_BUCKET).downloadPublic(CLEANINGS_RULE_FILE_NAME)
        } catch (e: Exception) {
            Napier.e("Помилка мережі в getCleaningsRules: ${e.message.orEmpty()}", tag = TAG)
            throw Exception(getString(Res.string.error_loading_file, CLEANINGS_RULE_FILE_NAME,e.message.orEmpty()))
        }
    }

    override suspend fun getReturningOfGoods(): ByteArray {
        try {
            return supabase.storage.from(PDF_FILES_BUCKET).downloadPublic(RETURNING_OF_GOODS_FILE_NAME)
        } catch (e: Exception) {
            Napier.e("Помилка мережі в getReturningOfGoods: ${e.message.orEmpty()}", tag = TAG)
            throw Exception(getString(Res.string.error_loading_file, RETURNING_OF_GOODS_FILE_NAME, e.message.orEmpty()))
        }
    }

    override suspend fun getStoreLeftovers(): ByteArray {
        try {
            return supabase.storage.from(PDF_FILES_BUCKET).downloadPublic(STORE_LEFTOVERS_FILE_NAME)
        } catch (e: Exception) {
            Napier.e("Помилка мережі в getStoreLeftovers: ${e.message.orEmpty()}", tag = TAG)
            throw Exception(getString(Res.string.error_loading_file, STORE_LEFTOVERS_FILE_NAME, e.message.orEmpty()))
        }
    }

    /**
     * Викликає Supabase Edge Function з вказаним тілом запиту.
     */
    @OptIn(InternalAPI::class)
    private suspend inline fun <reified T> invokeFunction(function: String, body: T): HttpResponse {
        val response = supabase.functions.invoke(function) {
            this.body = Json.encodeToString(body)
            contentType(ContentType.Application.Json)
        }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException(response.bodyAsText())
        }
        return response
    }
}
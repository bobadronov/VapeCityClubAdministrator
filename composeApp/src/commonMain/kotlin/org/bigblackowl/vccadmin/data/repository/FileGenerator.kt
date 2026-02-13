package org.bigblackowl.vccadmin.data.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.graphics.vector.ImageVector
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.application_for_return_of_goods
import vccadministrator.composeapp.generated.resources.cleaning_rules
import vccadministrator.composeapp.generated.resources.cleaning_schedule
import vccadministrator.composeapp.generated.resources.collection_schedule
import vccadministrator.composeapp.generated.resources.http_error
import vccadministrator.composeapp.generated.resources.shops_phone_list
import vccadministrator.composeapp.generated.resources.store_leftovers

// Дані структури (без змін, але додано bytes для WASM)
enum class FileType(val label: StringResource, val icon: ImageVector) {
    COLLECTION(label = Res.string.collection_schedule, icon = Icons.Default.Money),
    CLEANING(label = Res.string.cleaning_schedule, icon = Icons.Default.CleaningServices),
    SHOP_PHONES_LIST(Res.string.shops_phone_list, icon = Icons.Default.ContactPhone),
    RETURNING_OF_GOODS(Res.string.application_for_return_of_goods, icon = Icons.Default.Restore),
    CLEANING_RULES(Res.string.cleaning_rules, icon = Icons.Default.CalendarViewMonth),
    STORE_LEFTOVERS(Res.string.store_leftovers, icon = Icons.Default.Archive);


    data class FileTypeItem(val id: FileType, val label: StringResource, val icon: ImageVector)

    companion object {
        val allFileTypes = entries.map { FileTypeItem(it, it.label, it.icon) }
        val MONTH_TYPES = setOf(
            CLEANING,
            COLLECTION,
        )
        val GLOBAL_TYPES_WITHOUT_SHOPS = setOf(
            RETURNING_OF_GOODS,
            CLEANING_RULES,
            STORE_LEFTOVERS,
        )

        fun generators(repository: FileGeneratorRepository): Map<FileType, FileGenerator> = mapOf(
            COLLECTION to CollectionGenerator(repository),
            CLEANING to CleaningGenerator(repository),
            SHOP_PHONES_LIST to PhoneListGenerator(repository),
            RETURNING_OF_GOODS to ReturningOfGoodsGenerator(repository),
            CLEANING_RULES to CleaningRulesGenerator(repository),
            STORE_LEFTOVERS to StoreLeftoversGenerator(repository),
        )
    }
}


// Інтерфейс FileGenerator
interface FileGenerator {
    val type: FileType
    val isGlobal: Boolean

    suspend fun generate(
        shop: Shop? = null,
        shopIds: List<String>? = null,
        month: String? = null
    ): GeneratedFile?

    fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?): String
}

// Генератори (дороблено всі, адаптовано для WASM без folder/path)
class CollectionGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.COLLECTION
    override val isGlobal = false

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile? {
        if (shop == null || month == null) return null
        val response = repo.generateCollectionSchedulePdf(shop.id, month)
        if (response.status != HttpStatusCode.OK) throw Exception(getString(Res.string.http_error, response.status.toString(), response.bodyAsText()))
        val pdfBytes = response.bodyAsBytes()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) =
        "Бланк_інкасації_${shop?.cityName?.replace(" ", "_") ?: ""}_${shop?.street?.replace(" ", "_") ?: ""}_" +
                "${shop?.houseNumber?.replace(" ", "_")?.replace("/", "_")?.replace("\\", "_") ?: ""}_${month}.pdf"
}

class CleaningGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.CLEANING
    override val isGlobal = false

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile? {
        if (shop == null || month == null) return null
        val response = repo.generateCleaningSchedulePdf(shop.id, month)
        if (response.status != HttpStatusCode.OK) throw Exception(getString(Res.string.http_error, response.status.toString(), response.bodyAsText()))
        val pdfBytes = response.bodyAsBytes()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) =
        "Бланк_прибирання_${shop?.cityName?.replace(" ", "_") ?: ""}_${shop?.street?.replace(" ", "_") ?: ""}_" +
                "${shop?.houseNumber?.replace(" ", "_")?.replace("/", "_")?.replace("\\", "_") ?: ""}_${month}.pdf"
}

class PhoneListGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.SHOP_PHONES_LIST
    override val isGlobal = true

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile? {
        if (shopIds.isNullOrEmpty()) return null
        val response = repo.generatePhoneListPdf(shopIds)
        if (response.status != HttpStatusCode.OK) throw Exception(getString(Res.string.http_error, response.status.toString(), response.bodyAsText()))
        val pdfBytes = response.bodyAsBytes()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) = "Список_телефонів_магазинів.pdf"
}

class ReturningOfGoodsGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.RETURNING_OF_GOODS
    override val isGlobal = true

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile {
        val pdfBytes = repo.getReturningOfGoods()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) = "Заява_на_повернення_товарів.pdf"
}

class CleaningRulesGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.CLEANING_RULES
    override val isGlobal = true

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile {
        val pdfBytes = repo.getCleaningsRules()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) = "Розклад_прибирання.pdf"
}

class StoreLeftoversGenerator(private val repo: FileGeneratorRepository) : FileGenerator {
    override val type = FileType.STORE_LEFTOVERS
    override val isGlobal = true

    override suspend fun generate(shop: Shop?, shopIds: List<String>?, month: String?): GeneratedFile {
        val pdfBytes = repo.getStoreLeftovers()
        val fileName = getFileName(shop, shopIds, month)
        return GeneratedFile(fileName, pdfBytes)
    }

    override fun getFileName(shop: Shop?, shopIds: List<String>?, month: String?) = "Мінімальні_залишки_на_магазині.pdf"
}
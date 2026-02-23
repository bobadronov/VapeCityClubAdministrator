package org.bigblackowl.vccadmin.data.repository

import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.domain.repository.FileGenerator
import org.bigblackowl.vccadmin.domain.repository.FileGeneratorRepository
import org.bigblackowl.vccadmin.domain.repository.FileType
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.http_error

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
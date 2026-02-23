package org.bigblackowl.vccadmin.domain.repository

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.Restore
import androidx.compose.ui.graphics.vector.ImageVector
import org.bigblackowl.vccadmin.data.entity.Shop
import org.bigblackowl.vccadmin.data.repository.CleaningGenerator
import org.bigblackowl.vccadmin.data.repository.CleaningRulesGenerator
import org.bigblackowl.vccadmin.data.repository.CollectionGenerator
import org.bigblackowl.vccadmin.data.repository.PhoneListGenerator
import org.bigblackowl.vccadmin.data.repository.ReturningOfGoodsGenerator
import org.bigblackowl.vccadmin.data.repository.StoreLeftoversGenerator
import org.bigblackowl.vccadmin.ui.fileGenerator.GeneratedFile
import org.jetbrains.compose.resources.StringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.application_for_return_of_goods
import vccadministrator.composeapp.generated.resources.cleaning_rules
import vccadministrator.composeapp.generated.resources.cleaning_schedule
import vccadministrator.composeapp.generated.resources.collection_schedule
import vccadministrator.composeapp.generated.resources.shops_phone_list
import vccadministrator.composeapp.generated.resources.store_leftovers

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
            COLLECTION,
            CLEANING,
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
package org.bigblackowl.vccadmin.data.repository

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import org.bigblackowl.vccadmin.data.entity.NewShop
import org.bigblackowl.vccadmin.data.entity.SupabaseShop
import org.bigblackowl.vccadmin.domain.repository.ShopRepository

// Імплементації репозиторіїв: використовують Supabase Postgrest для запитів.
class ShopRepositoryImpl(
    private val supabase: SupabaseClient,
) : ShopRepository {
    companion object {
        private const val COLUMN_ID = "id"
        private const val SHOPS_TABLE = "shops"
    }

    private val shopTable = supabase.postgrest.from(SHOPS_TABLE)


    override fun getCurrentUserId(): String? = supabase.auth.currentUserOrNull()?.id

    override suspend fun updateShop(shopToUpdate: SupabaseShop) {
        println("updateShop: $shopToUpdate")
        shopTable.update(shopToUpdate) { filter { eq(COLUMN_ID, shopToUpdate.id) } }
    }

    override suspend fun getStores(): List<SupabaseShop> {
        return shopTable.select().decodeList<SupabaseShop>()
    }

    override suspend fun addShop(newShop: NewShop) {
        shopTable.insert(newShop)
    }

    override suspend fun getShopById(id: String): SupabaseShop? =
        try {
            shopTable.select {
                filter { eq(COLUMN_ID, id) }
            }
                .decodeSingleOrNull<SupabaseShop>()
        } catch (e: Exception) {
            Napier.e { e.message.orEmpty() }
            null
        }

    override suspend fun deleteShop(id: String) {
        shopTable.delete { filter { eq(COLUMN_ID, id) } }
    }
}
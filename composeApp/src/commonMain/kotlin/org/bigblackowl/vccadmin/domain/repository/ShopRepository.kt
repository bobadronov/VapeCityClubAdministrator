package org.bigblackowl.vccadmin.domain.repository

import org.bigblackowl.vccadmin.data.entity.NewShop
import org.bigblackowl.vccadmin.data.entity.SupabaseShop

// Інтерфейси репозиторіїв: для тестування та модульності.
interface ShopRepository {
    suspend fun getStores(): List<SupabaseShop>
    suspend fun addShop(newShop: NewShop)
    suspend fun getShopById(id: String): SupabaseShop?
    suspend fun deleteShop(id: String)
    fun getCurrentUserId(): String?
    suspend fun updateShop(shopToUpdate: SupabaseShop)
}
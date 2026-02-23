package org.bigblackowl.vccadmin.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import org.bigblackowl.vccadmin.data.entity.AdminAppUpdate

class OtaUpdateRepository (
    private val supabase: SupabaseClient,
) {
    private companion object {
        const val UPDATE_TABLE_NAME = "admin_app_updates"
    }

    suspend fun fetchLatestManifest(): AdminAppUpdate? {
        val rows: List<AdminAppUpdate> = supabase
            .from(UPDATE_TABLE_NAME)
            .select{
                order("published_at", order= Order.DESCENDING)
            }
            .decodeList()
        return rows.firstOrNull()
    }

}
package org.bigblackowl.vccadmin.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.data.entity.UpdateManifest
class OtaUpdateRepository (
    private val supabase: SupabaseClient,
) {
    private companion object {
        const val UPDATE_TABLE_NAME = "admin_app_updates"
    }

    suspend fun fetchLatestManifest(): UpdateManifest? {
        val rows: List<RowLatest> = supabase
            .from(UPDATE_TABLE_NAME)
            .select{
                order("published_at", order= Order.DESCENDING)
            }
            .decodeList()
        return rows.firstOrNull()?.manifest
    }

    @Serializable
    private data class RowLatest(
        val tag: String,
        val published_at: String? = null,
        val version_name: String,
        val desktop_version: String,
        val manifest: UpdateManifest
    )
}
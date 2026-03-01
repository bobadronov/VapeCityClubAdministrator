package org.bigblackowl.vccadmin.data.repository

import io.github.aakira.napier.Napier
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.LocalDate
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.encodeToJsonElement
import org.bigblackowl.vccadmin.domain.repository.WorkScheduleRepository
import org.bigblackowl.vccadmin.ui.workSchedule.WorkSchedule

class WorkScheduleRepositoryImpl(
    private val supabase: SupabaseClient,
    private val json: Json, // ← ВАЖЛИВО: прокинь той самий Json, що й в інших репо/DI
) : WorkScheduleRepository {

    companion object {
        private const val TABLE = "work_schedules"
        private const val COL_WEEK_START = "week_start"
    }

    private val table = supabase.postgrest.from(TABLE)

    override suspend fun loadWorkSchedule(weekStart: LocalDate): WorkSchedule? {
        val weekStartIso = weekStart.toString()

        return try {
            table.select {
                filter { eq(COL_WEEK_START, weekStartIso) }
                limit(1)
            }.decodeSingleOrNull<WorkScheduleRow>()?.toDomain()
        } catch (e: Exception) {
            Napier.e("loadWorkSchedule failed: ${e.message}", e)
            null
        }
    }

    override suspend fun saveWorkSchedule(schedule: WorkSchedule) {
        val userId = supabase.auth.currentUserOrNull()?.id
        val row = WorkScheduleRow.fromDomain(schedule, userId)

        val body = buildJsonArray {
            add(json.encodeToJsonElement(row)) // ← 1 row => JsonArray[ row ]
        }

        try {
            table.upsert(body) {
                onConflict = COL_WEEK_START      // "insert or update" by week_start
                ignoreDuplicates = false         // false = якщо конфлікт — оновлюємо
                // returning = ReturningRepresentation.Minimal // якщо є в твоїй версії — можна зменшити payload
            }
        } catch (e: Exception) {
            Napier.e("saveWorkSchedule failed: ${e.message}", e)
            throw e
        }
    }

    @Serializable
    private data class WorkScheduleRow(
        @SerialName("week_start")
        val weekStart: String, // yyyy-mm-dd (понеділок)

        @SerialName("shop_order")
        val shopOrder: List<String> = emptyList(),

        // { "2026-02-24": { "shopId": "userId" } }
        val assignments: Map<String, Map<String, String?>> = emptyMap(),

        @SerialName("last_modified_user_id") val lastModifiedUserId: String? = null,
    ) {
        fun toDomain(): WorkSchedule = WorkSchedule(
            weekStartIso = weekStart,
            shopOrder = shopOrder,
            assignments = assignments,
        )

        companion object {
            fun fromDomain(d: WorkSchedule, userId: String?): WorkScheduleRow = WorkScheduleRow(
                weekStart = d.weekStartIso,
                shopOrder = d.shopOrder,
                assignments = d.assignments,
                lastModifiedUserId = userId,
            )
        }
    }
}
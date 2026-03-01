package org.bigblackowl.vccadmin.ui.workSchedule

import kotlinx.serialization.Serializable

@Serializable
data class WorkSchedule(
    val weekStartIso: String,
    val shopOrder: List<String>,
    val assignments: Map<String, Map<String, String?>>,
)
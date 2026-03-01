package org.bigblackowl.vccadmin.domain.repository

import kotlinx.datetime.LocalDate
import org.bigblackowl.vccadmin.ui.workSchedule.WorkSchedule

interface WorkScheduleRepository {

    suspend fun loadWorkSchedule(weekStart: LocalDate): WorkSchedule?

    suspend fun saveWorkSchedule(schedule: WorkSchedule)
}


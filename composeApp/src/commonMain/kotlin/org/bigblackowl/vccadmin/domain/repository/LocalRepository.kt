package org.bigblackowl.vccadmin.domain.repository

import kotlinx.datetime.LocalDate
import org.bigblackowl.vccadmin.ui.workSchedule.create.WorkScheduleDraft

interface LocalRepository {

    fun getAutoEnterState(): Boolean
    fun setAutoEnterState(state: Boolean)

    fun saveErrorsToBuffer(data: String)
    fun loadErrorBuffer(): String?

    fun getThemeState(): Boolean?
    fun setThemeState(state: Boolean)

    fun setWindowClosable(state: Boolean)
    fun getWindowClosableState(): Boolean

    fun clearLocalStorage()

    fun getLanguage(): String?
    fun setLanguage(iso: String)

    suspend fun loadWorkScheduleDraft(weekStart: LocalDate): WorkScheduleDraft?
    suspend fun saveWorkScheduleDraft(draft: WorkScheduleDraft)
}

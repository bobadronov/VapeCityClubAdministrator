package org.bigblackowl.vccadmin.domain.repository

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
}

package org.bigblackowl.vccadmin.data.repository

import com.russhwolf.settings.Settings
import org.bigblackowl.vccadmin.domain.repository.LocalRepository

class LocalRepositoryImpl : LocalRepository {
    private companion object {
        private const val AUTO_ENTER_KEY = "AUTO_ENTER_KEY"
        private const val THEME_MODE_KEY = "THEME_MODE_KEY"
        const val ERROR_BUFFER_KEY = "error_buffer"
        private const val WINDOW_CLOSABLE_STATE = "WINDOW_CLOSABLE_STATE"
        private val settings: Settings = Settings()
    }

    override fun getAutoEnterState(): Boolean = settings.getBoolean(AUTO_ENTER_KEY, false)
    override fun setAutoEnterState(state: Boolean) = settings.putBoolean(AUTO_ENTER_KEY, state)

    override fun saveErrorsToBuffer(data: String) = settings.putString(ERROR_BUFFER_KEY, data)
    override fun loadErrorBuffer(): String? = settings.getStringOrNull(ERROR_BUFFER_KEY)

    override fun getThemeState(): Boolean? = settings.getBooleanOrNull(THEME_MODE_KEY)
    override fun setThemeState(state: Boolean) = settings.putBoolean(THEME_MODE_KEY, state)

    override fun setWindowClosable(state: Boolean) = settings.putBoolean(WINDOW_CLOSABLE_STATE, state)
    override fun getWindowClosableState(): Boolean = settings.getBoolean(WINDOW_CLOSABLE_STATE, false)
    override fun clearLocalStorage() = settings.clear()
}
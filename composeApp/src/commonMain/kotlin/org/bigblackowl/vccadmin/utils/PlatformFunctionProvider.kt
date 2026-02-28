@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

import androidx.compose.ui.platform.Clipboard

expect object PlatformFunctionProvider {
    fun openNetwork()
    suspend fun getCacheSize(): Long
    fun clearCache()

    /** Set plain text into Compose Clipboard (LocalClipboard.current). */
    suspend fun Clipboard.setPlainText(text: String)

    /** Read plain text from Compose Clipboard (LocalClipboard.current). */
    suspend fun Clipboard.getPlainText(): String?
}
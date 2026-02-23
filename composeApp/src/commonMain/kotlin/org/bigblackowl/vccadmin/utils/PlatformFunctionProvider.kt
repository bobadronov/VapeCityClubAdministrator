@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")

package org.bigblackowl.vccadmin.utils

expect object PlatformFunctionProvider {
    fun openNetwork()

    suspend fun getCacheSize(): Long
    fun clearCache()
}
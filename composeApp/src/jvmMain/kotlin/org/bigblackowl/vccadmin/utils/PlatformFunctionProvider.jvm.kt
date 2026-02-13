@file:Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
package org.bigblackowl.vccadmin.utils

import kotlinx.io.IOException
import java.util.Locale

internal actual object PlatformFunctionProvider {

    actual fun openNetwork() {
        val os = System.getProperty("os.name").orEmpty().lowercase(Locale.getDefault())

        when {
            os.contains("win") -> openWindowsNetworkSettings()
            os.contains("mac") || os.contains("darwin") -> openMacNetworkSettings()
            else -> openLinuxNetworkSettings()
        }
    }

    private fun openWindowsNetworkSettings() {
        if (tryStart("cmd", "/c", "start", "", "ms-settings:network")) return
        if (tryStart("cmd", "/c", "start", "", "ms-settings:network-status")) return
        if (tryStart("cmd", "/c", "start", "", "ncpa.cpl")) return
        tryStart("cmd", "/c", "start", "", "control.exe", "/name", "Microsoft.NetworkAndSharingCenter")
    }

    private fun openMacNetworkSettings() {
        if (tryStart("open", "x-apple.systempreferences:com.apple.NetworkSettings")) return
        tryStart("open", "x-apple.systempreferences:com.apple.preference.network")
    }

    private fun openLinuxNetworkSettings() {
        // GNOME / Ubuntu
        if (tryStart("sh", "-c", "gnome-control-center network")) return
        // NetworkManager UI
        if (tryStart("sh", "-c", "nm-connection-editor")) return
        // KDE Plasma
        if (tryStart("sh", "-c", "systemsettings5 kcm_networkmanagement")) return
        if (tryStart("sh", "-c", "kcmshell5 kcm_networkmanagement")) return
        // Fallback: open "network" in settings apps if present
        tryStart("sh", "-c", "xdg-open 'network:'")
    }

    private fun tryStart(vararg command: String): Boolean {
        return try {
            ProcessBuilder(*command)
                .redirectErrorStream(true)
                .start()
            true
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
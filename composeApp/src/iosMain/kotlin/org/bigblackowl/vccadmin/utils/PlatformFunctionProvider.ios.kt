package org.bigblackowl.vccadmin.utils

import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString

internal actual object PlatformFunctionProvider {
    actual fun openNetwork() {
        val app = UIApplication.sharedApplication

        // ✅ Safe, App Store friendly: відкриває налаштування саме твого застосунку.
        val settingsUrl = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
        if (settingsUrl != null && app.canOpenURL(settingsUrl)) {
            app.openURL(settingsUrl)
            return
        }
    }
}
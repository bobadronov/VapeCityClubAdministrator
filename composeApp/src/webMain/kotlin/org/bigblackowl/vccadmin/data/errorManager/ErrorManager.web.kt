package org.bigblackowl.vccadmin.data.errorManager

import kotlinx.browser.window

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object SystemInfoProvider {

    actual fun systemInfo(): SystemInfo {
        val ua = window.navigator.userAgent
        val screen = "${window.screen.width}x${window.screen.height}"

        return SystemInfo(
            device = "Browser",
            model = ua.takeIf { it.isNotBlank() },
            product = window.navigator.platform.takeIf { it.isNotBlank() },
            osVersion = null,
            screenSize = screen,
        )
    }
}
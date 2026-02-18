package org.bigblackowl.vccadmin.data.errorManager

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice
import platform.UIKit.UIScreen

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object SystemInfoProvider {
    @OptIn(ExperimentalForeignApi::class)
    actual fun systemInfo(): SystemInfo {
        val device = UIDevice.currentDevice
        val bounds = UIScreen.mainScreen.bounds
        val scale = UIScreen.mainScreen.scale

        val widthPx = (bounds.size.width * scale).toInt()
        val heightPx = (bounds.size.height * scale).toInt()

        return SystemInfo(
            device = "${device.systemName()} ${device.model()}",
            model = device.name(), // можна також model() / identifier (якщо заведеш окремо)
            product = device.model(),
            osVersion = device.systemVersion(),
            screenSize = "${widthPx}x${heightPx}",
        )
    }
}
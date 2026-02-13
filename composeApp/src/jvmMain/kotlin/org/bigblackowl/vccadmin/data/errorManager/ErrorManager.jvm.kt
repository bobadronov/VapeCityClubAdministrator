package org.bigblackowl.vccadmin.data.errorManager

import java.awt.Toolkit

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object SystemInfoProvider {
    actual fun systemInfo(): SystemInfo {
        val osName = System.getProperty("os.name") ?: "Desktop"
        val osVersion = System.getProperty("os.version")
        val arch = System.getProperty("os.arch")

        val screen = runCatching {
            val size = Toolkit.getDefaultToolkit().screenSize
            "${size.width}x${size.height}"
        }.getOrNull()

        return SystemInfo(
            device = "Desktop $osName",
            model = arch,
            product = osName,
            osVersion = osVersion,
            screenSize = screen,
        )
    }
}
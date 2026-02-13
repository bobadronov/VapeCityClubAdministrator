package org.bigblackowl.vccadmin.data.errorManager

import android.content.res.Resources
import android.os.Build

@Suppress(names = ["EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING"])
actual object SystemInfoProvider {
    actual fun systemInfo(): SystemInfo {
        val dm = Resources.getSystem().displayMetrics
        val screen = "${dm.widthPixels}x${dm.heightPixels}"

        val brand = Build.BRAND.orEmpty()
        val model = Build.MODEL.orEmpty()
        val device = listOf(brand, model).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "Android" }

        return SystemInfo(
            device = device,
            model = model.takeIf { it.isNotBlank() },
            product = Build.PRODUCT.takeIf { !it.isNullOrBlank() },
            osVersion = Build.VERSION.RELEASE.takeIf { !it.isNullOrBlank() } ?: Build.VERSION.SDK_INT.toString(),
            screenSize = screen,
        )
    }
}
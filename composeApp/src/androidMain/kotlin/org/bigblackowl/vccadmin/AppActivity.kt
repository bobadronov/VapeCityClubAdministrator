package org.bigblackowl.vccadmin

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowInsetsControllerCompat
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.init
import org.koin.core.component.KoinComponent

class AppActivity : ComponentActivity(), KoinComponent {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        init()

        setContent {
            App(onThemeChanged = { ThemeChanged(it) }) 
        }
    }
}

private fun AppActivity.init(){
//    GlobalContext.get().apply {
//        setProperty("AppActivity", this)
//    }
    FileKit.init(this)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
}

@Composable
private fun ThemeChanged(isDark: Boolean) {
    val view = LocalView.current
    LaunchedEffect(isDark) {
        val window = (view.context as Activity).window
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = isDark
            isAppearanceLightNavigationBars = isDark
        }
    }
}

//fun setMaxBrightness(enabled: Boolean) {
//    try {
//        val activity = getKoin().getProperty<AppActivity>("appActivity")
//        val window = activity?.window ?: return
//        val params: WindowManager.LayoutParams = window.attributes
//        params.screenBrightness = if (enabled) WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
//        window.attributes = params
//    } catch (e: Exception) {
//        Napier.e(tag = "QRCodeDialog") { "Failed to set brightness: ${e.message}" }
//    }
//}
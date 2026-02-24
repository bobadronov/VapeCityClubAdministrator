package org.bigblackowl.vccadmin.theme.locals

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import org.bigblackowl.vccadmin.data.entity.ThemeMode

val LocalThemeMode = compositionLocalOf {
    mutableStateOf(ThemeMode.AUTO)
}
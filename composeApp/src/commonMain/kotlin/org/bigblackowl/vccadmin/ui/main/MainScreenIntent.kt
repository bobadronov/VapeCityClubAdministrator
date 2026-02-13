package org.bigblackowl.vccadmin.ui.main

sealed class MainScreenIntent {
    data object Refresh : MainScreenIntent()

}
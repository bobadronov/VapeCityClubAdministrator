package org.bigblackowl.vccadmin.ui.slidesList

sealed interface SlidesListScreenIntent {
    object Load : SlidesListScreenIntent
    object Refresh : SlidesListScreenIntent
    data class ToggleSlideVisibility(val slideId: String) : SlidesListScreenIntent
}
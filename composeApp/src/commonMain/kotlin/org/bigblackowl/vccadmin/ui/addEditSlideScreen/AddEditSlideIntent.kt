package org.bigblackowl.vccadmin.ui.addEditSlideScreen

sealed interface AddEditSlideIntent {
    object SelectFile : AddEditSlideIntent
    object OnSave : AddEditSlideIntent
    object OnToggleAllShops : AddEditSlideIntent
    object OnToggleAllShopsWithTablet : AddEditSlideIntent
    object OnToggleAllShopsWithTv : AddEditSlideIntent

    object ClearData : AddEditSlideIntent
    object GoBack : AddEditSlideIntent
    object DiscardChanges : AddEditSlideIntent
    object DownloadIconFile : AddEditSlideIntent
    object OpenFile : AddEditSlideIntent
    data class OnFileNameChanged(val newName: String) : AddEditSlideIntent
    data class LoadSlide(val slideId: String?) : AddEditSlideIntent
    data class OnShopToggled(val code: String) : AddEditSlideIntent
    data class OnActiveChanged(val state: Boolean) : AddEditSlideIntent
    data class DeleteSlide(val slideId: String) : AddEditSlideIntent

}
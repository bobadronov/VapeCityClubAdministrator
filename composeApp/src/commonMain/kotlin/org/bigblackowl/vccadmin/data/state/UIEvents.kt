package org.bigblackowl.vccadmin.data.state

sealed interface UIEvents {

    data class ShowMessage(val message: String) : UIEvents

    object NavigateBack : UIEvents

    object ShowUnsavedChangesDialog : UIEvents

    data class NotificationAndNavigate(val message: String) : UIEvents

}
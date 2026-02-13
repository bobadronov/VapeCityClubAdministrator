package org.bigblackowl.vccadmin.ui.editSlidesSettings

// Модель елемента слайду для порядку та прев'ю
data class SlideOrderItem(
    val id: String,
    val fileName: String,
    val position: Int,
    val url: String,
)
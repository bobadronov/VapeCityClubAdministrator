package org.bigblackowl.vccadmin.ui.slideAiGeneration

sealed class GenerateMode {
    data object TextOnly : GenerateMode()
    data class EditWithPhoto(val photo: LocalImage) : GenerateMode()
    data class Variations(val photo: LocalImage) : GenerateMode()
    data class TemplateWithPhoto(val template: TemplateSpec, val photo: LocalImage) : GenerateMode()
}
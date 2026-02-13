package org.bigblackowl.vccadmin.ui.slideAiGeneration

import com.aallam.openai.api.image.ImageSize

data class ImageGenSettings(
    val modelId: String = "dall-e-3",
    val size: ImageSize = ImageSize.Companion.is1024x1024,
    val n: Int = 1,
    val quality: String? = null,
    val background: String? = null,
    val outputFormat: String? = null,
)
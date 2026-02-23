package org.bigblackowl.vccadmin.domain.repository

import com.aallam.openai.api.model.Model
import org.bigblackowl.vccadmin.ui.slideAiGeneration.GenerateMode
import org.bigblackowl.vccadmin.ui.slideAiGeneration.GeneratedImage
import org.bigblackowl.vccadmin.ui.slideAiGeneration.ImageGenSettings

/**
 * ==========================
 *   Repository + OpenAI-KT
 * ==========================
 */
interface AiRepository {
    suspend fun generate(prompt: String, mode: GenerateMode, settings: ImageGenSettings): List<GeneratedImage>
    suspend fun getModels(): List<Model>
}
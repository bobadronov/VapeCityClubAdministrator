package org.bigblackowl.vccadmin.data.repository

import com.aallam.openai.api.file.FileSource
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageEdit
import com.aallam.openai.api.image.ImageURL
import com.aallam.openai.api.image.ImageVariation
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.io.Buffer
import org.bigblackowl.vccadmin.ui.slideAiGeneration.GenerateMode
import org.bigblackowl.vccadmin.ui.slideAiGeneration.GeneratedImage
import org.bigblackowl.vccadmin.ui.slideAiGeneration.ImageGenSettings
import org.bigblackowl.vccadmin.ui.slideAiGeneration.LocalImage

/**
 * ==========================
 *   Repository + OpenAI-KT
 * ==========================
 */
interface AiRepository {
    suspend fun generate(prompt: String, mode: GenerateMode, settings: ImageGenSettings): List<GeneratedImage>
    suspend fun getModels(): List<Model>
}

class AiRepositoryImpl(
    private val openAI: OpenAI,
    private val httpClient: HttpClient,
) : AiRepository {

    companion object { private const val TAG = "AiRepositoryImpl" }

    override suspend fun getModels(): List<Model> = openAI.models()

    override suspend fun generate(
        prompt: String,
        mode: GenerateMode,
        settings: ImageGenSettings
    ): List<GeneratedImage> {
        val cleanPrompt = prompt.trim()

        return when (mode) {
            GenerateMode.TextOnly -> {
                requireModelForGeneration(settings.modelId)
                generateTextOnly(cleanPrompt, settings)
            }

            is GenerateMode.EditWithPhoto -> {
                requireModelForEdits(settings.modelId)

                // ВАЖЛИВО: openai-kotlin ImageEdit вимагає mask (не null).
                // Якщо ти НЕ маєш маски — найреалістичніший шлях:
                // 1) просити користувача обрати mask.png
                // 2) або автоматично згенерувати прозору mask того ж розміру (потрібні image-tools, див. нижче).
                val imagePng = ensurePngSquareOrThrow(mode.photo, "photo for edits")
                val maskPng  = ensureMaskPngSquareOrThrow() // <- заглушка/гачок під твою реалізацію

                editWithMask(
                    prompt = cleanPrompt.ifBlank { "Edit the image" },
                    imagePng = imagePng,
                    maskPng = maskPng,
                    settings = settings,
                )
            }

            is GenerateMode.Variations -> {
                // Variations — фактично DALL·E 2 only по OpenAI API.
                // У openai-kotlin типи дозволяють modelId, але по суті має бути dall-e-2.
                requireModelForVariations(settings.modelId)

                val imagePng = ensurePngSquareOrThrow(mode.photo, "photo for variations")
                variations(imagePng, settings)
            }

            is GenerateMode.TemplateWithPhoto -> {
                // Реальна композиція "template + photo" = або:
                // A) робиш композицію локально (canvas) -> отримуєш baseImage.png -> edits
                // B) або повністю via edits з маскою рамки.
                // Тут: припускаємо, що ти робиш локальну композицію і потім edits.
                requireModelForEdits(settings.modelId)

                val composedBase = composeTemplateOrThrow()
                val maskPng = ensureMaskForTemplateOrThrow()

                editWithMask(
                    prompt = cleanPrompt.ifBlank { "Place the person photo into the template frame" },
                    imagePng = composedBase,
                    maskPng = maskPng,
                    settings = settings,
                )
            }
        }
    }

    // ---------- TEXT-ONLY ----------
    private suspend fun generateTextOnly(prompt: String, settings: ImageGenSettings): List<GeneratedImage> {
        val resp: List<ImageURL> = openAI.imageURL(
            creation = ImageCreation(
                prompt = prompt,
                n = settings.n,
                size = settings.size,
                model = ModelId(settings.modelId),
                // quality/style існують як типи в openai-kotlin ImageCreation,
                // але ти тримаєш їх строками — тут свідомо не маплю, щоб не вигадувати enum/value class.
            )
        )
        Napier.d(tag = TAG) { "imageURL(creation) resp.size=${resp.size}" }
        return downloadAll(resp)
    }

    // ---------- EDITS ----------
    private suspend fun editWithMask(
        prompt: String,
        imagePng: LocalImage,
        maskPng: LocalImage,
        settings: ImageGenSettings,
    ): List<GeneratedImage> {
        val imageFs = imagePng.toFileSource(forceName = "image.png")
        val maskFs  = maskPng.toFileSource(forceName = "mask.png")

        val req = ImageEdit(
            image = imageFs,
            mask = maskFs,
            prompt = prompt,
            n = settings.n,
            size = settings.size,
            model = ModelId(settings.modelId),
        )

        val resp: List<ImageURL> = openAI.imageURL(req)
        Napier.d(tag = TAG) { "imageURL(edit) resp.size=${resp.size}" }
        return downloadAll(resp)
    }

    // ---------- VARIATIONS ----------
    private suspend fun variations(
        imagePng: LocalImage,
        settings: ImageGenSettings,
    ): List<GeneratedImage> {
        val imageFs = imagePng.toFileSource(forceName = "image.png")

        val req = ImageVariation(
            image = imageFs,
            n = settings.n,
            size = settings.size,
            model = ModelId(settings.modelId), // реально очікуй dall-e-2
        )

        val resp: List<ImageURL> = openAI.imageURL(req)
        Napier.d(tag = TAG) { "imageURL(variation) resp.size=${resp.size}" }
        return downloadAll(resp)
    }

    // ---------- DOWNLOAD ----------
    private suspend fun downloadAll(urls: List<ImageURL>): List<GeneratedImage> =
        urls.map { item ->
            val url = item.url
            val bytes = httpClient.get(url).bodyAsBytes()
            GeneratedImage(url = url, bytes = bytes, mime = "image/png")
        }

    // ---------- MODEL RULES ----------
    private fun requireModelForGeneration(modelId: String) {
        // openai-kotlin ImageCreation: DALL·E моделі (за документацією пакету).
        if (!modelId.startsWith("dall-e-")) {
            throw IllegalStateException(
                "Model '$modelId' не підтримується через ImageCreation у openai-kotlin. " +
                        "Обери dall-e-2/dall-e-3."
            )
        }
    }

    private fun requireModelForEdits(modelId: String) {
        // ImageEdit в openai-kotlin типізовано під DALL·E. :contentReference[oaicite:4]{index=4}
        if (!modelId.startsWith("dall-e-")) {
            throw IllegalStateException(
                "Model '$modelId' не підтримується для edits через ImageEdit у openai-kotlin. " +
                        "Обери dall-e-2 (рекомендовано для edits)."
            )
        }
    }

    private fun requireModelForVariations(modelId: String) {
        // В OpenAI API variations — DALL·E 2 only. :contentReference[oaicite:5]{index=5}
        if (modelId != "dall-e-2") {
            throw IllegalStateException("Variations підтримує тільки dall-e-2. Зараз обрано '$modelId'.")
        }
    }
}

/**
 * ByteArray -> FileSource без файлів на диску:
 * FileSource(name, source: RawSource), а Buffer = Source = RawSource. :contentReference[oaicite:6]{index=6}
 */
private fun LocalImage.toFileSource(forceName: String? = null): FileSource {
    val buffer = Buffer().apply { write(bytes) }
    return FileSource(
        name = forceName ?: fileName,
        source = buffer
    )
}

/**
 * Мінімальні гачки-валидації.
 * Реально: для edits/variations OpenAI вимагає PNG, square, <4MB.
 * Тут — строго: якщо не png — кидаємо, бо конвертація має бути platform-specific.
 */
private fun ensurePngSquareOrThrow(img: LocalImage, label: String): LocalImage {
    if (img.mime != "image/png") {
        throw IllegalArgumentException("Потрібен PNG для $label. Зараз: ${img.mime}.")
    }
    // square-validate: треба знати розміри (декодувати). Це роби в platform image-tools.
    return img
}

/**
 * Поки що заглушка: openai-kotlin ImageEdit вимагає mask FileSource. :contentReference[oaicite:7]{index=7}
 * ВАРІАНТИ:
 * 1) Додати у UI вибір mask.png (найшвидше).
 * 2) Автоматично створювати прозорий mask PNG того ж розміру (потрібна декод/енкод).
 */
private fun ensureMaskPngSquareOrThrow(): LocalImage {
    throw IllegalStateException(
        "Для edits потрібна mask.png (openai-kotlin ImageEdit вимагає mask). " +
                "Додай вибір маски у UI або реалізуй авто-генерацію прозорої маски."
    )
}

private fun composeTemplateOrThrow(): LocalImage {
    throw IllegalStateException("TODO: локальна композиція template+photo (platform-specific image tools).")
}

private fun ensureMaskForTemplateOrThrow(): LocalImage {
    throw IllegalStateException("TODO: маска для template (platform-specific image tools).")
}
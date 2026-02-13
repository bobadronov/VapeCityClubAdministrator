package org.bigblackowl.vccadmin.ui.slideAiGeneration

data class GeneratedImage(
    val url: String,
    val bytes: ByteArray,
    val mime: String = "image/png",
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as GeneratedImage
        if (!bytes.contentEquals(other.bytes)) return false
        if (mime != other.mime) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mime.hashCode()
        return result
    }
}
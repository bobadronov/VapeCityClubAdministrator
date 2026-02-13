package org.bigblackowl.vccadmin.ui.slideAiGeneration

data class LocalImage(
    val bytes: ByteArray,
    val mime: String,
    val fileName: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as LocalImage
        if (!bytes.contentEquals(other.bytes)) return false
        if (mime != other.mime) return false
        if (fileName != other.fileName) return false
        return true
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + mime.hashCode()
        result = 31 * result + fileName.hashCode()
        return result
    }
}
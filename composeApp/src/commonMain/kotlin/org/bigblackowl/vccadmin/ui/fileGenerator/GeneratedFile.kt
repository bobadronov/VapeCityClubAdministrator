package org.bigblackowl.vccadmin.ui.fileGenerator

data class GeneratedFile(
    val name: String,
    val content: ByteArray?,
    val error: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as GeneratedFile

        if (name != other.name) return false
        if (!content.contentEquals(other.content)) return false
        if (error != other.error) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + (content?.contentHashCode() ?: 0)
        result = 31 * result + (error?.hashCode() ?: 0)
        return result
    }
}
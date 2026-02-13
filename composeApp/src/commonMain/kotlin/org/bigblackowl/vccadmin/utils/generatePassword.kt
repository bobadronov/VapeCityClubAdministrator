package org.bigblackowl.vccadmin.utils

import kotlin.random.Random

fun generatePassword(length: Int = 8): String {
    require(length >= 4) { "length must be >= 4 to include all required character types." }

    val lower = ('a'..'z').toList()
    val upper = ('A'..'Z').toList()
    val digits = ('0'..'9').toList()
    val specials = listOf('!', '@', '#', '$', '%', '&', '*', '?')

    val all = lower + upper + digits + specials

    fun <T> List<T>.rand() = this[Random.nextInt(this.size)]

    val pwChars = mutableListOf<Char>()
    // Гарантуємо по одному з кожної групи
    pwChars += lower.rand()
    pwChars += upper.rand()
    pwChars += digits.rand()
    pwChars += specials.rand()

    // Додаємо решту випадкових символів
    repeat(length - 4) { pwChars += all.rand() }

    // Fisher–Yates shuffle з SecureRandom
    for (i in pwChars.size - 1 downTo 1) {
        val j = Random.nextInt(i + 1)
        val tmp = pwChars[i]
        pwChars[i] = pwChars[j]
        pwChars[j] = tmp
    }
    pwChars.shuffle(random = Random)
    return pwChars.joinToString("")
}
package org.bigblackowl.vccadmin.utils

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.BuildConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.datetime_format_full
import vccadministrator.composeapp.generated.resources.month_april
import vccadministrator.composeapp.generated.resources.month_august
import vccadministrator.composeapp.generated.resources.month_december
import vccadministrator.composeapp.generated.resources.month_february
import vccadministrator.composeapp.generated.resources.month_january
import vccadministrator.composeapp.generated.resources.month_july
import vccadministrator.composeapp.generated.resources.month_june
import vccadministrator.composeapp.generated.resources.month_march
import vccadministrator.composeapp.generated.resources.month_may
import vccadministrator.composeapp.generated.resources.month_november
import vccadministrator.composeapp.generated.resources.month_october
import vccadministrator.composeapp.generated.resources.month_september
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.Instant



object AppStringProvider {
    private const val CURRENT_VERSION: String = BuildConfig.APP_VERSION

    fun isRemoteNewer(remote: String?): Boolean {
        val r = parseVersionParts(remote)
        val l = parseVersionParts(CURRENT_VERSION)

        if (r.isEmpty() || l.isEmpty()) return false

        val n = max(r.size, l.size)
        for (i in 0 until n) {
            val a = r.getOrNull(i) ?: 0
            val b = l.getOrNull(i) ?: 0
            if (a != b) return a > b
        }
        return false
    }

    private fun parseVersionParts(v: String?): List<Int> {
        val raw = v?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()

        // 1) прибрати 'v'/'V' на початку
        val noV = raw.removePrefix("v").removePrefix("V")

        // 2) взяти тільки "число(.число)*" на початку (відрізає "1.0.812-beta", "1.0.8 (abc)")
        val core = noV.takeWhile { it.isDigit() || it == '.' }

        // 3) розбити й перетворити в Int
        return core.split('.')
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }
    fun formatBytesToString(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups)
        val factor = 10.0.pow(2)
        return "${round(value * factor) / factor} ${units[digitGroups]}"
    }

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
    /**
     * Форматує timestamp (ms від епохи) у гарний локалізований формат:
     * "18:00 25 грудня 2025"
     */
    @OptIn(ExperimentalTime::class)
    suspend fun formatTimestamp(timestampMs: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestampMs)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

        val day = dateTime.day
        val month = getString(dateTime.month.getName())
        val year = dateTime.year
        val hour = dateTime.hour.toString().padStart(2, '0')
        val minute = dateTime.minute.toString().padStart(2, '0')

        return getString(
            Res.string.datetime_format_full,
            day,
            month,
            year,
            hour,
            minute
        )
    }

    private fun Month.getName(): StringResource = when (this) {
        Month.JANUARY -> Res.string.month_january
        Month.FEBRUARY -> Res.string.month_february
        Month.MARCH -> Res.string.month_march
        Month.APRIL -> Res.string.month_april
        Month.MAY -> Res.string.month_may
        Month.JUNE -> Res.string.month_june
        Month.JULY -> Res.string.month_july
        Month.AUGUST -> Res.string.month_august
        Month.SEPTEMBER -> Res.string.month_september
        Month.OCTOBER -> Res.string.month_october
        Month.NOVEMBER -> Res.string.month_november
        Month.DECEMBER -> Res.string.month_december
    }
}
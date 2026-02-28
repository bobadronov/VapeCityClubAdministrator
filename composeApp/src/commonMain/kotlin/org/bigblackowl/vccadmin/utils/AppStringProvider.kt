package org.bigblackowl.vccadmin.utils

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bigblackowl.vccadmin.BuildConfig
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.datetime_format_full
import vccadministrator.composeapp.generated.resources.friday
import vccadministrator.composeapp.generated.resources.monday
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
import vccadministrator.composeapp.generated.resources.saturday
import vccadministrator.composeapp.generated.resources.sunday
import vccadministrator.composeapp.generated.resources.thursday
import vccadministrator.composeapp.generated.resources.tuesday
import vccadministrator.composeapp.generated.resources.wednesday
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Утилітний об'єкт для роботи з рядками, ресурсами та форматуванням у додатку.
 *
 * Містить:
 * - перевірку версій,
 * - форматування розміру файлів,
 * - генерацію безпечних паролів,
 * - локалізоване форматування дати/часу,
 * - мапінг місяців та днів тижня на Compose ресурси.
 *
 * Використовується в усьому проєкті Kotlin Multiplatform + Jetpack Compose.
 */
object AppStringProvider {

    /**
     * Поточна версія додатка з BuildConfig.
     * Використовується для порівняння з віддаленою версією.
     */
    private const val CURRENT_VERSION: String = BuildConfig.APP_VERSION

    /**
     * Перевіряє, чи віддалена версія новіша за поточну.
     *
     * @param remote рядок версії з сервера (може бути null)
     * @return true, якщо remote > CURRENT_VERSION, інакше false
     */
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

    /**
     * Розбирає рядок версії на список цілих чисел.
     * Підтримує формати: "1.2.3", "v1.2.3", "1.0.8-beta", "1.2.3 (build 456)".
     * Ігнорує все після першого не-цифрового/крапкового символу.
     *
     * @param v рядок версії
     * @return список Int (наприклад [1, 2, 3])
     */
    private fun parseVersionParts(v: String?): List<Int> {
        val raw = v?.trim().orEmpty()
        if (raw.isBlank()) return emptyList()

        // 1) прибрати 'v'/'V' на початку
        val noV = raw.removePrefix("v").removePrefix("V")

        // 2) взяти тільки "число(.число)*" на початку
        val core = noV.takeWhile { it.isDigit() || it == '.' }

        // 3) розбити й перетворити в Int
        return core.split('.')
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
    }

    /**
     * Форматує кількість байтів у читабельний вигляд з одиницями вимірювання.
     *
     * Приклади:
     * - 1024 → "1.0 KB"
     * - 1048576 → "1.0 MB"
     * - 0 → "0 B"
     *
     * @param bytes кількість байтів
     * @return рядок у форматі "123.45 MB"
     */
    fun formatBytesToString(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups)
        val factor = 10.0.pow(2)
        return "${round(value * factor) / factor} ${units[digitGroups]}"
    }

    /**
     * Генерує випадковий надійний пароль заданої довжини.
     *
     * Гарантовано містить:
     * - 1 малу літеру,
     * - 1 велику літеру,
     * - 1 цифру,
     * - 1 спеціальний символ.
     *
     * Решта символів — випадкові з усього набору.
     * Використовує Fisher-Yates shuffle для рівномірного розподілу.
     *
     * @param length довжина пароля (мінімум 4)
     * @return згенерований пароль
     * @throws IllegalArgumentException якщо length < 4
     */
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

        // Fisher–Yates shuffle
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
     * Форматує timestamp (мілісекунди від епохи) у гарний локалізований формат.
     *
     * Приклад: "18:00 25 грудня 2025"
     *
     * @param timestampMs час у мілісекундах
     * @return відформатований рядок
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

    /**
     * Повертає StringResource для назви місяця українською.
     */
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

    /**
     * Повертає StringResource для назви дня тижня українською.
     */
    fun DayOfWeek.toStringRes(): StringResource = when (this) {
        DayOfWeek.MONDAY -> Res.string.monday
        DayOfWeek.TUESDAY -> Res.string.tuesday
        DayOfWeek.WEDNESDAY -> Res.string.wednesday
        DayOfWeek.THURSDAY -> Res.string.thursday
        DayOfWeek.FRIDAY -> Res.string.friday
        DayOfWeek.SATURDAY -> Res.string.saturday
        DayOfWeek.SUNDAY -> Res.string.sunday
    }

    /**
     * Український колатор для правильного сортування (міста → вулиці).
     * Працює на всіх платформах без залежностей.
     */
    private const val ORDER = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя"
    val ukrainianCollator: Comparator<String> = Comparator { a, b ->
        val s1 = a.lowercase()
        val s2 = b.lowercase()
        val len = minOf(s1.length, s2.length)

        for (i in 0 until len) {
            val pos1 = ORDER.indexOf(s1[i])
            val pos2 = ORDER.indexOf(s2[i])

            if (pos1 != pos2) {
                return@Comparator when {
                    pos1 == -1 -> 1
                    pos2 == -1 -> -1
                    else -> pos1 - pos2
                }
            }
        }
        s1.length.compareTo(s2.length)
    }

}
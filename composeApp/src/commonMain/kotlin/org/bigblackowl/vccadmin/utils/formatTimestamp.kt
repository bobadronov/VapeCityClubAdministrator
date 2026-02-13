package org.bigblackowl.vccadmin.utils

import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Форматує timestamp (ms від епохи) у гарний український формат:
 * "18:00 25 грудня 2025"
 */
@OptIn(ExperimentalTime::class)
suspend fun formatTimestamp(timestampMs: Long): String {
    val instant = Instant.fromEpochMilliseconds(timestampMs)
    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())

    val day = dateTime.day
    val month = getString(dateTime.month.ukrainianName())
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

private fun Month.ukrainianName(): StringResource = when (this) {
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
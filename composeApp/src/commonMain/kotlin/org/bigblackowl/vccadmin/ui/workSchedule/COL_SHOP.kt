package org.bigblackowl.vccadmin.ui.workSchedule

import kotlinx.datetime.LocalDate

const val COL_SHOP = "COL_SHOP"
fun colDay(day: LocalDate) = "day:${day}" // day.toString() == yyyy-mm-dd
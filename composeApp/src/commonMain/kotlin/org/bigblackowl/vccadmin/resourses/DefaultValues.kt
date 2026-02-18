package org.bigblackowl.vccadmin.resourses

import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

object DefaultValues {
    object Color {
        const val MENU_ALPHA = 0.1f
    }

    object Time {
        val shortDelay = 4.seconds

        @OptIn(ExperimentalTime::class)
        val now = Clock.System.now().toEpochMilliseconds()
        val date = Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date.toString().replace("-", "_")
    }

    object Shape {
        val defaultShape = 20.dp
    }

    object Size {
        val iconSize = 25.dp
        val gridItemMinSize = 350.dp
        val textTitleAutoSize = TextAutoSize.StepBased(
            minFontSize = 20.sp, maxFontSize = 26.sp
        )
        val textBodyAutoSize = TextAutoSize.StepBased(
            minFontSize = 14.sp, maxFontSize = 17.sp
        )
        val textHelperAutoSize = TextAutoSize.StepBased(
            minFontSize = 11.sp, maxFontSize = 14.sp
        )
        val textSmallAutoSize = TextAutoSize.StepBased(
            minFontSize = 8.sp, maxFontSize = 11.sp
        )
    }

    object Padding {
        val mainBoxPadding = 10.dp
        val verticalListItemPadding = 10.dp
        val rowItemPadding = 10.dp
        val cardContentPadding = 10.dp
        val lazyVerticalGridContentPadding = 10.dp
        val flowRowPadding = 8.dp
    }
}
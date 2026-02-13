package org.bigblackowl.vccadmin.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.bigblackowl.vccadmin.theme.shopStatusColors
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.status_active
import vccadministrator.composeapp.generated.resources.status_closed
import vccadministrator.composeapp.generated.resources.status_inactive
import vccadministrator.composeapp.generated.resources.status_relocating
import vccadministrator.composeapp.generated.resources.status_under_repair

@Serializable
enum class ShopStatus {
    @SerialName("ACTIVE")
    ACTIVE,

    @SerialName("INACTIVE")
    INACTIVE,

    @SerialName("CLOSED")
    CLOSED,

    @SerialName("RELOCATING")
    RELOCATING,

    @SerialName("UNDER_REPAIR")
    UNDER_REPAIR;

    val title: StringResource
        get() = when (this@ShopStatus) {
            ACTIVE -> Res.string.status_active
            INACTIVE -> Res.string.status_inactive
            CLOSED -> Res.string.status_closed
            RELOCATING -> Res.string.status_relocating
            UNDER_REPAIR -> Res.string.status_under_repair
        }

    val icon: ImageVector
        get() = when (this@ShopStatus) {
            ACTIVE -> Icons.Default.Done
            INACTIVE -> Icons.Default.Pause
            CLOSED -> Icons.Default.Close
            RELOCATING -> Icons.Default.EditLocation
            UNDER_REPAIR -> Icons.Default.Build
        }

    val color: Color
        @Composable
        get() = with(MaterialTheme.shopStatusColors) {
            return when (this@ShopStatus) {
                ACTIVE -> active
                INACTIVE -> inactive
                CLOSED -> closed
                RELOCATING -> relocating
                UNDER_REPAIR -> underRepair
            }
        }

    companion object {
        @Composable
        fun all(): List<ShopStatusInfo> =
            entries.mapIndexed { index, status ->
                ShopStatusInfo(
                    index = index,
                    status = status,
                    name = stringResource(status.title),
                    icon = status.icon,
                    color = status.color
                )
            }
    }
}

@Immutable
data class ShopStatusColors(
    val active: Color,
    val inactive: Color,
    val closed: Color,
    val relocating: Color,
    val underRepair: Color,
)
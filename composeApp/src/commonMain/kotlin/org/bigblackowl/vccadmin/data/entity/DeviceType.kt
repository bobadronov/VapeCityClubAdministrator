package org.bigblackowl.vccadmin.data.entity

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Tablet
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.device_none
import vccadministrator.composeapp.generated.resources.device_tablet
import vccadministrator.composeapp.generated.resources.device_tv

@Serializable
enum class DeviceType {
    NONE,
    TV,
    TABLET;

    val title: StringResource
        get() = when (this) {
            NONE -> Res.string.device_none
            TV -> Res.string.device_tv
            TABLET -> Res.string.device_tablet
        }

    val icon: ImageVector
        get() = when (this) {
            NONE -> Icons.Default.Block
            TV -> Icons.Default.LiveTv
            TABLET -> Icons.Default.Tablet
        }
    companion object {
        @Composable
        fun all(): List<DeviceTypeInfo> =
            entries.mapIndexed { index, type ->
                DeviceTypeInfo(
                    index = index,
                    type = type,
                    name = stringResource(type.title),
                    icon = type.icon
                )
            }
    }
}

data class DeviceTypeInfo(
    val index: Int,
    val type: DeviceType,
    val name: String,
    val icon: ImageVector
)
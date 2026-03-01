package org.bigblackowl.vccadmin.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.bigblackowl.vccadmin.domain.repository.LocalRepository
import org.bigblackowl.vccadmin.theme.DefaultValues
import org.bigblackowl.vccadmin.uiComponent.container.OutlinedCardWithLabel
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.stay_online_on_close
import vccadministrator.composeapp.generated.resources.work_in_background

actual fun LazyListScope.platformSettingsItems() {
    item(key = "stay_online_on_close") {
        val localRepository: LocalRepository = koinInject()

        var minimizeToTrayOnClose by rememberSaveable { mutableStateOf(false) }

        // ініціалізація 1 раз (або при зміні repo)
        LaunchedEffect(localRepository) {
            minimizeToTrayOnClose = localRepository.getWindowClosableState()
        }

        WorkInBackgroundCard(
            checked = minimizeToTrayOnClose,
            onCheckedChange = { newValue ->
                minimizeToTrayOnClose = newValue
                localRepository.setWindowClosable(newValue)
            }
        )
    }
}

@Composable
private fun WorkInBackgroundCard(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCardWithLabel(
        label = stringResource(Res.string.work_in_background),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DefaultValues.Padding.rowItemPadding)
        ) {
            DefaultIcon(Icons.Default.ClosedCaptionDisabled)
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            BodyText(stringResource(Res.string.stay_online_on_close))
        }
    }
}
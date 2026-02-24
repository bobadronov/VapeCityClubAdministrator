package org.bigblackowl.vccadmin.uiComponent.buttons

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.uiComponent.icons.DefaultIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.utils.isWideScreen
import org.jetbrains.compose.resources.stringResource
import vccadministrator.composeapp.generated.resources.Res
import vccadministrator.composeapp.generated.resources.delete

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeleteButton(
    message: String,
    modifier: Modifier = Modifier,
    showLabel: Boolean = isWideScreen(),
    label: String = stringResource(Res.string.delete),
    onDeleteConfirmed: () -> Unit,
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    OutlinedButton(
        onClick = {
            showDeleteConfirmation = true
        },
//        colors = ButtonDefaults.buttonColors(
//            contentColor = MaterialTheme.colorScheme.error,
//            containerColor = MaterialTheme.colorScheme.errorContainer
//        ),
        modifier = modifier,
    ) {
        DefaultIcon(Icons.Default.Delete)
        if (showLabel) {
            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
            BodyText(text = label,)
        }
    }

    AnimatedVisibility(showDeleteConfirmation) {
        BasicAlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            OutlinedCard(
                modifier = Modifier.padding(8.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceBright,
                ),
                shape = MaterialTheme.shapes.medium,

            ) {
                Column(
                    modifier = Modifier.padding(DefaultValues.Padding.mainBoxPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(DefaultValues.Padding.verticalListItemPadding, Alignment.CenterVertically)
                ) {
                    BodyText(
                        text = message,
                        textAlign = TextAlign.Center,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(
                            5.dp,
                            Alignment.CenterHorizontally
                        )
                    ) {
                        OutlinedButton(
                            onClick = {
                                onDeleteConfirmed()
                                showDeleteConfirmation = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.weight(1f),
                        ) {
                            BodyText(
                                text = stringResource(Res.string.delete),
                            )
                        }
                        CancelButton(color = ButtonDefaults.outlinedButtonColors()) {
                            showDeleteConfirmation = false
                        }

                    }
                }
            }
        }
    }
}
package org.bigblackowl.vccadmin.uiComponent.listItems

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.bigblackowl.vccadmin.data.entity.User
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.resourses.DefaultValues
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.text.BodyText
import org.bigblackowl.vccadmin.uiComponent.text.SmallText
import org.bigblackowl.vccadmin.uiComponent.text.TitleText
import org.jetbrains.compose.resources.stringResource

@Composable
fun CurrentUserHeader(
    currentUser: User?,
    onClick: (String) -> Unit,
) {

    // Якщо користувач ще не завантажився — показуємо красивий placeholder, а не порожній простір
    AnimatedContent(
        targetState = currentUser,
        label = "CurrentUserHeader transition",
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        }
    ) { user ->
        Card(
            onClick = { onClick(currentUser?.id.orEmpty()) },
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(DefaultValues.Shape.defaultShape),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 2.dp
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (user == null) {
                    // Заглушка під час завантаження або після логауту
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    )
                    BodyText(
                        text = "Завантаження…",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Icon(
                        Icons.Default.VerifiedUser, null,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .border(
                                width = 1.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                    )

                    Column {
                        TitleText(
                            text = user.fullName,
                            fontWeight = FontWeight.Medium,
                        )

                        BodyText(
                            text = stringResource(user.role.getName),
                            color = user.role.color,
                        )

                        SmallText(
                            text = user.email,
                            modifier = Modifier.basicMarquee(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                }
            }
        }
    }
}

@Preview
@Composable
private fun CurrentUserHeaderPreview1() = PreviewDarkMaterialTheme {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CurrentUserHeader(currentUser = FakeBackend.singleUser, onClick = {})
        CurrentUserHeader(currentUser = FakeBackend.users[2], onClick = {})
    }
}

@Preview
@Composable
private fun CurrentUserHeaderPreview2() = PreviewLightMaterialTheme {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CurrentUserHeader(currentUser = FakeBackend.singleUser, onClick = {})
        CurrentUserHeader(currentUser = FakeBackend.users[2], onClick = {})
    }
}


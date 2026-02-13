package org.bigblackowl.vccadmin.uiComponent.listItems

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import org.bigblackowl.vccadmin.data.entity.City
import org.bigblackowl.vccadmin.data.repository.FakeBackend
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.uiComponent.icons.OnlineIcon
import org.bigblackowl.vccadmin.uiComponent.text.BodyText

@Composable
fun StickyCityHeader(city: City) {

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (city.logoUrl.isNullOrBlank().not()) {
                OnlineIcon(
                    model = city.logoUrl,
                    modifier = Modifier.size(25.dp),
                )
                Spacer(Modifier.width(12.dp))
            }

            BodyText(
                text = city.name.uppercase(),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}


@OptIn(ExperimentalCoilApi::class)
@Preview
@Composable
private fun StickyCityHeaderPreview1() = PreviewDarkMaterialTheme {
    StickyCityHeader(FakeBackend.singleCity)
}

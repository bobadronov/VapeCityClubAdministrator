package org.bigblackowl.vccadmin.playground

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.Json
import org.bigblackowl.vccadmin.data.entity.AdminAppUpdate
import org.bigblackowl.vccadmin.data.entity.AssetInfo
import org.bigblackowl.vccadmin.data.entity.UpdateInfo
import org.bigblackowl.vccadmin.theme.PreviewDarkMaterialTheme
import org.bigblackowl.vccadmin.theme.PreviewLightMaterialTheme
import org.bigblackowl.vccadmin.utils.AppStringProvider
import org.jetbrains.compose.resources.ExperimentalResourceApi
import vccadministrator.composeapp.generated.resources.Res

@Composable
private fun PlaygroundContent() {

    var update by remember { mutableStateOf<AdminAppUpdate?>(null) }

    LaunchedEffect(Unit) {
        update = loadUpdateFromResources()
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Playground", style = MaterialTheme.typography.titleLarge)
            val u = update
            if (u != null) {
                UpdateHeader(u)

                if (!u.releaseNotes.isNullOrBlank()) {
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Release notes", style = MaterialTheme.typography.titleMedium)
                            Text(u.releaseNotes)
                        }
                    }
                }

                val assets = u.assets()
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Assets (${assets.size})", style = MaterialTheme.typography.titleMedium)
                        assets.forEach { info ->
                            AssetRow(info.asset)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateHeader(u: AdminAppUpdate) {

    val publishedText by produceState(initialValue = "—", key1 = u.publishedAt) {
        value = AppStringProvider.formatTimestamp(u.publishedAt ?: 0L)
    }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            KeyValue("Version", u.version ?: "—")
            KeyValue("Published", publishedText)
        }
    }
}

@Composable
private fun AssetRow(a: AssetInfo) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(a.name, style = MaterialTheme.typography.titleSmall)
            KeyValue("URL", a.url)
            KeyValue("Size", formatBytes(a.size))
            KeyValue("SHA-256", a.sha256 ?: "—")
        }
    }
}

@Composable
private fun KeyValue(k: String, v: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(k, modifier = Modifier.widthIn(min = 90.dp), style = MaterialTheme.typography.bodyMedium)
        Text(v, style = MaterialTheme.typography.bodyMedium)
    }
}

private val UpdateJson = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}

@OptIn(ExperimentalResourceApi::class)
private suspend fun loadUpdateFromResources(): AdminAppUpdate {
    val bytes = Res.readBytes("files/update.json")
    return UpdateJson.decodeFromString<AdminAppUpdate>(bytes.decodeToString())
}

private fun AdminAppUpdate.assets(): List<UpdateInfo> = buildList {
    windows?.let { add(UpdateInfo(this@assets, it)) }
    macos?.let { add(UpdateInfo(this@assets, it)) }
    linux?.let { add(UpdateInfo(this@assets, it)) }
    android?.let { add(UpdateInfo(this@assets, it)) }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> "${((bytes / gb) * 100).toInt() / 100.0} GB"
        bytes >= mb -> "${((bytes / mb) * 100).toInt() / 100.0} MB"
        bytes >= kb -> "${((bytes / kb) * 100).toInt() / 100.0} KB"
        else -> "$bytes B"
    }
}

@Preview
@Composable
private fun PlaygroundPreviewDark() = PreviewDarkMaterialTheme {
    PlaygroundContent()
}

@Preview
@Composable
private fun PlaygroundPreviewLight() = PreviewLightMaterialTheme {
    PlaygroundContent()
}
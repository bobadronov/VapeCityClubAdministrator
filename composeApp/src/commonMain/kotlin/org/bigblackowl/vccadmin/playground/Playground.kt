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

@Composable
private fun PlaygroundContent() {

    var update by remember { mutableStateOf(loadUpdateFromResources()) }

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
private fun loadUpdateFromResources(): AdminAppUpdate {
    val data = "{\n" +
            "  \"version\": \"1.2.603\",\n" +
            "  \"published_at\": 1771845913000,\n" +
            "  \"release_notes\": \"Changes\\n\\n- Fix OTA overlay crash (a1b2c3d)\\n- Improve caching on Settings screen (d4e5f6a)\\n- Bump dependencies (1a2b3c4)\\n\",\n" +
            "  \"windows\": {\n" +
            "    \"name\": \"VCC-Admin-Setup-1.2.603.msi\",\n" +
            "    \"url\": \"https://github.com/OWNER/REPO/releases/download/v1.2.603/VCC-Admin-Setup-1.2.603.msi\",\n" +
            "    \"size\": 125829120,\n" +
            "    \"sha256\": \"9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08\"\n" +
            "  },\n" +
            "  \"macos\": {\n" +
            "    \"name\": \"VCC-Admin-1.2.603.dmg\",\n" +
            "    \"url\": \"https://github.com/OWNER/REPO/releases/download/v1.2.603/VCC-Admin-1.2.603.dmg\",\n" +
            "    \"size\": 104857600,\n" +
            "    \"sha256\": \"3a7bd3e2360a3d80d64f0c1b3d1d2f1b6a6b1f1a9fd0c3b3b3d5d3f1a2b3c4d5\"\n" +
            "  },\n" +
            "  \"linux\": {\n" +
            "    \"name\": \"vcc-admin_1.2.603_amd64.deb\",\n" +
            "    \"url\": \"https://github.com/OWNER/REPO/releases/download/v1.2.603/vcc-admin_1.2.603_amd64.deb\",\n" +
            "    \"size\": 94371840,\n" +
            "    \"sha256\": \"b2e98ad6f6eb8508dd6a14cfa704bad7f05f6fb1b2a3c4d5e6f708192a3b4c5d\"\n" +
            "  },\n" +
            "  \"android\": {\n" +
            "    \"name\": \"VCC.Administrator-1.2.603.apk\",\n" +
            "    \"url\": \"https://github.com/OWNER/REPO/releases/download/v1.2.603/VCC.Administrator-1.2.603.apk\",\n" +
            "    \"size\": 50331648,\n" +
            "    \"sha256\": \"c0535e4be2b79ffd93291305436bf889314e4a3faec05ecffcbb7df31ad9e51a\"\n" +
            "  }\n" +
            "}"
    return UpdateJson.decodeFromString<AdminAppUpdate>(data)
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
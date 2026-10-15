package com.aquagrow.ui.screens.tools

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aquagrow.BuildConfig
import com.aquagrow.R
import com.aquagrow.ui.components.AppCard
import com.aquagrow.ui.components.AppTopBar
import com.aquagrow.ui.components.LeafFishMark
import com.aquagrow.ui.preview.ThemePreviews
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens

private val sections = listOf(
    R.string.privacy_data_title to R.string.privacy_data_body,
    R.string.privacy_backup_title to R.string.privacy_backup_body,
    R.string.privacy_permissions_title to R.string.privacy_permissions_body,
    R.string.privacy_ai_title to R.string.privacy_ai_body,
    R.string.privacy_estimates_title to R.string.privacy_estimates_body,
    R.string.privacy_delete_title to R.string.privacy_delete_body,
    R.string.privacy_licenses_title to R.string.privacy_licenses_body,
)

@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = { AppTopBar(stringResource(R.string.privacy_title), onBack) }) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    LeafFishMark(size = 56.dp)
                    Column {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.privacy_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodySmall, color = AquaGrowTheme.colors.mutedText)
                    }
                }
                Text(stringResource(R.string.privacy_summary), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 16.dp))
            }
            items(sections) { (title, body) ->
                AppCard(Modifier.fillMaxWidth(), contentPadding = 16.dp) {
                    Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = AquaGrowTheme.colors.mutedText, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@ThemePreviews
@Composable
private fun PrivacyPreview() = com.aquagrow.ui.theme.AquaGrowTheme { PrivacyScreen(onBack = {}) }

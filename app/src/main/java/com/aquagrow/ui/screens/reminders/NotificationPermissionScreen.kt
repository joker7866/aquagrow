package com.aquagrow.ui.screens.reminders

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aquagrow.R
import com.aquagrow.ui.components.PrimaryButton
import com.aquagrow.ui.screens.onboarding.ONBOARDING_STEPS
import com.aquagrow.ui.screens.onboarding.ProgressDots
import com.aquagrow.ui.theme.AquaGrowTheme
import com.aquagrow.ui.theme.Dimens

/** True when reminders can actually be shown (permission granted and app notifications on). */
fun Context.notificationsAllowed(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled() &&
    (
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )

fun Context.openAppNotificationSettings() {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

/**
 * Onboarding step 2: explains reminders before the system permission prompt (Android 13+).
 * Declining is fine: tasks still show in the app, and Profile offers a way back later.
 */
@Composable
fun NotificationPermissionScreen(onDone: () -> Unit) {
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { onDone() }
    val garden = AquaGrowTheme.colors.garden
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.systemBarsPadding().padding(horizontal = Dimens.ScreenPadding + 4.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressDots(current = 2, total = ONBOARDING_STEPS, modifier = Modifier.weight(1f))
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Spacer(Modifier.height(24.dp))
                Surface(shape = MaterialTheme.shapes.large, color = garden.container, modifier = Modifier.size(88.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ic_notifications), null, tint = garden.primary, modifier = Modifier.size(44.dp)) }
                }
                Text(stringResource(R.string.perm_title), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.perm_body), style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
                listOf(R.string.perm_point_1, R.string.perm_point_2, R.string.perm_point_3).forEach { p ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(painterResource(R.drawable.ic_check_circle), null, tint = garden.primary)
                        Text(stringResource(p), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
            val context = LocalContext.current
            PrimaryButton(
                stringResource(R.string.perm_allow),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !context.notificationsAllowed()) {
                        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        onDone()
                    }
                },
                modifier = Modifier.padding(top = 16.dp),
            )
            TextButton(onClick = onDone, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = Dimens.MinTouchTarget).padding(bottom = 8.dp)) {
                Text(stringResource(R.string.perm_later), style = MaterialTheme.typography.bodyLarge, color = AquaGrowTheme.colors.mutedText)
            }
        }
    }
}

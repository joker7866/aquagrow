package com.aquagrow.notifications

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.lifecycleScope
import com.aquagrow.R
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.OccurrenceActions
import com.aquagrow.ui.theme.AquaGrowTheme
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.launch

/** Tiny dialog opened from a notification's "Snooze" action: 15 min, 1 hour or tomorrow. */
@AndroidEntryPoint
class SnoozeActivity : ComponentActivity() {
    @Inject lateinit var actions: OccurrenceActions

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var time: TimeProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra(ReminderActionReceiver.EXTRA_TASK_ID, -1)
        val date = LocalDate.ofEpochDay(intent.getLongExtra(ReminderActionReceiver.EXTRA_DATE, 0))
        if (taskId < 0) {
            finish()
            return
        }
        fun pick(block: suspend () -> Unit) {
            lifecycleScope.launch {
                block()
                notifier.cancel(taskId, date)
                finish()
            }
        }
        setContent {
            AquaGrowTheme {
                AlertDialog(
                    onDismissRequest = { finish() },
                    title = { Text(stringResource(R.string.notif_action_snooze)) },
                    text = {
                        Column {
                            TextButton({ pick { actions.snooze(taskId, date, time.now().plusSeconds(15 * 60)) } }, Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.notif_action_15))
                            }
                            TextButton({ pick { actions.snooze(taskId, date, time.now().plusSeconds(60 * 60)) } }, Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.notif_action_60))
                            }
                            TextButton({ pick { actions.reschedule(taskId, date, time.today().plusDays(1)) } }, Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.notif_action_tomorrow))
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = { TextButton({ finish() }) { Text(stringResource(R.string.action_cancel)) } },
                )
            }
        }
    }
}

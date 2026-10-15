package com.aquagrow.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.aquagrow.R
import java.time.LocalDate

/** "Move this task": tomorrow, or any future date. */
@Composable
fun RescheduleDialog(
    today: LocalDate,
    current: LocalDate,
    onMove: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    var picking by remember { mutableStateOf(false) }
    if (picking) {
        DatePickerModal(
            initial = maxOf(current, today).plusDays(1),
            minDate = today,
            onDismiss = onDismiss,
            onConfirm = onMove,
        )
        return
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reschedule_title)) },
        text = {
            Column {
                TextButton(onClick = { onMove(today.plusDays(1)) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reschedule_tomorrow))
                }
                TextButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.reschedule_pick))
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

package com.aquagrow.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aquagrow.R
import com.aquagrow.di.ApplicationScope
import com.aquagrow.domain.model.OccurrenceStatus
import com.aquagrow.domain.model.ReminderKind
import com.aquagrow.domain.model.TaskType
import com.aquagrow.domain.reminders.PlannedReminder
import com.aquagrow.domain.reminders.QuietHours
import com.aquagrow.domain.reminders.ReminderChannel
import com.aquagrow.domain.reminders.ReminderPlanner
import com.aquagrow.domain.repository.ReminderLogRepository
import com.aquagrow.domain.repository.ReminderScheduler
import com.aquagrow.domain.repository.SettingsRepository
import com.aquagrow.domain.repository.TaskRepository
import com.aquagrow.domain.time.TimeProvider
import com.aquagrow.domain.usecase.OccurrenceActions
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * WorkManager-backed scheduling. WorkManager persists work across reboots and app updates;
 * we additionally reschedule on boot / time / timezone changes (see [TimeChangeReceiver]),
 * because work delays are relative and a timezone change moves wall-clock times.
 */
@Singleton
class WorkManagerReminderScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ReminderScheduler {
    override fun requestReschedule() {
        // Debounced: bursts of edits collapse into one reschedule pass.
        WorkManager.getInstance(context).enqueueUniqueWork(
            RescheduleWorker.NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<RescheduleWorker>().setInitialDelay(2, TimeUnit.SECONDS).build(),
        )
    }

    fun ensurePeriodicRefresh() {
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            RefreshWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<RefreshWorker>(6, TimeUnit.HOURS)
                .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
                .build(),
        )
    }
}

/** Computes the next ~3 days of reminders and makes WorkManager match exactly (adds, replaces, cancels). */
@HiltWorker
class RescheduleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val today = time.today()
        val occurrences = tasks.getOccurrences(today.minusDays(1), today.plusDays(HORIZON_DAYS))
        val plan = ReminderPlanner.plan(occurrences, settings.getNotificationSettings(), time.now(), time.zone())
        val wm = WorkManager.getInstance(applicationContext)

        plan.forEach { r -> enqueueReminder(wm, r) }

        // Cancel reminders that are no longer in the plan (task done, deleted, moved, channel off...).
        val wanted = plan.map { it.key }.toSet()
        wm.getWorkInfosByTagFlow(ReminderWorker.TAG).first()
            .filter { !it.state.isFinished }
            .forEach { info ->
                val key = info.tags.firstOrNull { it.startsWith(KEY_TAG) }?.removePrefix(KEY_TAG)
                if (key != null && key !in wanted) wm.cancelUniqueWork(key)
            }
        return Result.success()
    }

    private fun enqueueReminder(wm: WorkManager, r: PlannedReminder) {
        val delay = Duration.between(time.now(), r.triggerAt).toMillis().coerceAtLeast(0)
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(ReminderWorker.KEY_TASK_ID to r.taskId, ReminderWorker.KEY_DATE to r.originalDate.toEpochDay()))
            .addTag(ReminderWorker.TAG)
            .addTag(KEY_TAG + r.key)
            .build()
        // One unique work per occurrence: REPLACE updates the time, it can never create a second copy.
        wm.enqueueUniqueWork(r.key, ExistingWorkPolicy.REPLACE, request)
    }

    companion object {
        const val NAME = "reminders-reschedule"
        const val HORIZON_DAYS = 2L
        const val KEY_TAG = "rk:"
    }
}

/** Fires for one occurrence. Re-checks everything at fire time, so stale work never notifies. */
@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val log: ReminderLogRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val taskId = inputData.getLong(KEY_TASK_ID, -1)
        val date = LocalDate.ofEpochDay(inputData.getLong(KEY_DATE, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE } ?: return Result.success())
        val task = tasks.getTask(taskId) ?: return Result.success()
        val state = tasks.getState(taskId, date)
        if (state != null && state.status != OccurrenceStatus.PENDING) return Result.success()
        val s = settings.getNotificationSettings()
        val channel = ReminderChannel.forTask(task.type, task.maintenanceType)
        if (!s.remindersEnabled || channel.name in s.disabledChannels) return Result.success()

        // Quiet hours may have changed since scheduling: if we are inside them now, defer.
        val nowLocal = time.now().atZone(time.zone()).toLocalTime()
        if (s.quietHoursEnabled && QuietHours.contains(nowLocal, s.quietHoursStart, s.quietHoursEnd)) {
            scheduler.requestReschedule()
            return Result.success()
        }

        val title = listOfNotNull(task.title, task.subjectName).joinToString(" · ")
        val text = when (task.type) {
            TaskType.WATERING -> applicationContext.getString(R.string.notif_due_watering)
            else -> listOfNotNull(task.itemName, task.amountText).joinToString(" · ").ifEmpty { applicationContext.getString(R.string.notif_due_generic) }
        }
        // Dedupe key includes the snooze moment, so a snoozed reminder may fire again exactly once.
        val dedupe = "due-$taskId-$date-${state?.snoozedUntil?.epochSecond ?: 0}"
        if (log.recordIfNew(ReminderKind.DUE, taskId, date, null, title, text, dedupe)) {
            notifier.showDue(task, date, title, text, channel)
        }
        return Result.success()
    }

    companion object {
        const val TAG = "reminder"
        const val KEY_TASK_ID = "taskId"
        const val KEY_DATE = "date"
    }
}

/**
 * Periodic safety net (every ~6 h): extends the scheduling horizon, and posts one
 * "missed" follow-up for watering/feeding that was not marked done.
 */
@HiltWorker
class RefreshWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val tasks: TaskRepository,
    private val settings: SettingsRepository,
    private val log: ReminderLogRepository,
    private val notifier: ReminderNotifier,
    private val scheduler: ReminderScheduler,
    private val inventoryCheck: InventoryReminderCheck,
    private val time: TimeProvider,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        scheduler.requestReschedule()
        val s = settings.getNotificationSettings()
        val nowLocal = time.now().atZone(time.zone()).toLocalTime()
        val quiet = s.quietHoursEnabled && QuietHours.contains(nowLocal, s.quietHoursStart, s.quietHoursEnd)
        if (!s.remindersEnabled || quiet) return Result.success() // next run will catch up

        if (s.missedRemindersEnabled) {
            val df = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            tasks.getOverdue(time.today())
                .filter { it.task.type == TaskType.WATERING || it.task.type == TaskType.FEEDING }
                .forEach { occ ->
                    val title = applicationContext.getString(R.string.notif_missed_title, listOfNotNull(occ.task.title, occ.task.subjectName).joinToString(" · "))
                    val text = applicationContext.getString(R.string.notif_missed_text, df.format(occ.date))
                    if (log.recordIfNew(ReminderKind.MISSED, occ.task.id, occ.originalDate, null, title, text, "missed-${occ.task.id}-${occ.originalDate}")) {
                        notifier.showMissed(occ.task, occ.originalDate, title, text)
                    }
                }
        }
        if (s.inventoryRemindersEnabled) inventoryCheck.run()
        return Result.success()
    }

    companion object {
        const val NAME = "reminders-refresh"
    }
}

/** Notification actions: Mark done / Snooze. Runs the use case off the main thread with goAsync. */
@AndroidEntryPoint
class ReminderActionReceiver : BroadcastReceiver() {
    @Inject lateinit var actions: OccurrenceActions

    @Inject lateinit var notifier: ReminderNotifier

    @Inject lateinit var time: TimeProvider

    @Inject @ApplicationScope
    lateinit var scope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1)
        val date = LocalDate.ofEpochDay(intent.getLongExtra(EXTRA_DATE, 0))
        if (taskId < 0) return
        val pending = goAsync()
        scope.launch {
            try {
                when (intent.action) {
                    ACTION_DONE -> actions.markDone(taskId, date)
                    ACTION_SNOOZE_15 -> actions.snooze(taskId, date, time.now().plusSeconds(15 * 60))
                    ACTION_SNOOZE_60 -> actions.snooze(taskId, date, time.now().plusSeconds(60 * 60))
                    ACTION_SNOOZE_TOMORROW -> actions.reschedule(taskId, date, time.today().plusDays(1))
                }
                notifier.cancel(taskId, date)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_DONE = "com.aquagrow.action.DONE"
        const val ACTION_SNOOZE_15 = "com.aquagrow.action.SNOOZE_15"
        const val ACTION_SNOOZE_60 = "com.aquagrow.action.SNOOZE_60"
        const val ACTION_SNOOZE_TOMORROW = "com.aquagrow.action.SNOOZE_TOMORROW"
        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_DATE = "date"
        const val EXTRA_NOTIFICATION_ID = "notificationId"
    }
}

/** Reboot, app update, clock or timezone change: rebuild every reminder from the database. */
@AndroidEntryPoint
class TimeChangeReceiver : BroadcastReceiver() {
    @Inject lateinit var scheduler: ReminderScheduler

    @Inject lateinit var workScheduler: WorkManagerReminderScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                workScheduler.ensurePeriodicRefresh()
                scheduler.requestReschedule()
            }
        }
    }
}

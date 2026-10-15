package com.aquagrow.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aquagrow.MainActivity
import com.aquagrow.R
import com.aquagrow.domain.model.CareModule
import com.aquagrow.domain.model.CareTask
import com.aquagrow.domain.reminders.ReminderChannel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Creates channels and posts reminder notifications with Mark done / Snooze / Open actions. */
@Singleton
class ReminderNotifier @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun createChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        ReminderChannel.entries.forEach { ch ->
            val name = context.getString(channelName(ch))
            val channel = NotificationChannel(channelId(ch), name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = context.getString(R.string.channel_desc, name)
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun canPost(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled() &&
        (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            )

    /** Posts (or replaces) the reminder for one occurrence. The id is stable per occurrence, so it never duplicates. */
    fun showDue(task: CareTask, date: LocalDate, title: String, text: String, channel: ReminderChannel) {
        if (!canPost()) return
        val id = notificationId(task.id, date)
        val builder = base(channel, title, text, openIntent(task, date, id))
            .addAction(0, context.getString(R.string.notif_action_done), action(ReminderActionReceiver.ACTION_DONE, task.id, date, id))
            .addAction(0, context.getString(R.string.notif_action_snooze), snoozeIntent(task.id, date, id))
        post(id, builder)
    }

    fun showMissed(task: CareTask, date: LocalDate, title: String, text: String) {
        if (!canPost()) return
        val id = notificationId(task.id, date) xor MISSED_SALT
        val builder = base(ReminderChannel.MISSED, title, text, openIntent(task, date, id))
            .addAction(0, context.getString(R.string.notif_action_done), action(ReminderActionReceiver.ACTION_DONE, task.id, date, id))
        post(id, builder)
    }

    fun showInventory(itemId: Long, title: String, text: String) {
        if (!canPost()) return
        val id = (itemId.hashCode() * 31) xor INVENTORY_SALT
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_INVENTORY, true)
        val pi = PendingIntent.getActivity(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        post(id, base(ReminderChannel.INVENTORY, title, text, pi))
    }

    fun cancel(taskId: Long, date: LocalDate) {
        NotificationManagerCompat.from(context).cancel(notificationId(taskId, date))
        NotificationManagerCompat.from(context).cancel(notificationId(taskId, date) xor MISSED_SALT)
    }

    private fun base(channel: ReminderChannel, title: String, text: String, content: PendingIntent) = NotificationCompat.Builder(context, channelId(channel))
        .setSmallIcon(R.drawable.ic_stat_aquagrow)
        .setColor(ContextCompat.getColor(context, R.color.garden_primary))
        .setContentTitle(title)
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setContentIntent(content)
        .setAutoCancel(true)
        .setCategory(NotificationCompat.CATEGORY_REMINDER)
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    @Suppress("MissingPermission") // checked in canPost()
    private fun post(id: Int, builder: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(id, builder.build())
    }

    private fun openIntent(task: CareTask, date: LocalDate, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_PLANT_ID, task.plantId ?: -1L)
            .putExtra(MainActivity.EXTRA_AQUARIUM_ID, task.aquariumId ?: -1L)
            .putExtra(MainActivity.EXTRA_DATE, date.toEpochDay())
            .putExtra(MainActivity.EXTRA_MODULE, task.module.name)
        return PendingIntent.getActivity(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** Opens the small snooze chooser (15 min / 1 hour / tomorrow). */
    private fun snoozeIntent(taskId: Long, date: LocalDate, notificationId: Int): PendingIntent {
        val intent = Intent(context, SnoozeActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderActionReceiver.EXTRA_DATE, date.toEpochDay())
        return PendingIntent.getActivity(context, notificationId * 31 + 7, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun action(action: String, taskId: Long, date: LocalDate, notificationId: Int): PendingIntent {
        val intent = Intent(context, ReminderActionReceiver::class.java)
            .setAction(action)
            .putExtra(ReminderActionReceiver.EXTRA_TASK_ID, taskId)
            .putExtra(ReminderActionReceiver.EXTRA_DATE, date.toEpochDay())
            .putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
        // Unique request code per (occurrence, action) so PendingIntents don't overwrite each other.
        return PendingIntent.getBroadcast(context, (notificationId * 31) + action.hashCode(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    companion object {
        private const val MISSED_SALT = 0x5A5A5A5A
        private const val INVENTORY_SALT = 0x0F0F0F0F

        fun notificationId(taskId: Long, date: LocalDate): Int = "rem-$taskId-$date".hashCode()

        fun channelId(ch: ReminderChannel) = "reminders_${ch.name.lowercase()}"

        fun channelName(ch: ReminderChannel): Int = when (ch) {
            ReminderChannel.WATERING -> R.string.channel_watering
            ReminderChannel.FERTILIZER -> R.string.channel_fertilizer
            ReminderChannel.SOWING -> R.string.channel_sowing
            ReminderChannel.TRANSPLANTING -> R.string.channel_transplanting
            ReminderChannel.HARVEST -> R.string.channel_harvest
            ReminderChannel.FEEDING -> R.string.channel_feeding
            ReminderChannel.WATER_CHANGE -> R.string.channel_water_change
            ReminderChannel.TESTING -> R.string.channel_testing
            ReminderChannel.FILTER -> R.string.channel_filter
            ReminderChannel.GENERAL -> R.string.channel_general
            ReminderChannel.MISSED -> R.string.channel_missed
            ReminderChannel.INVENTORY -> R.string.channel_inventory
        }

        fun moduleOf(name: String?): CareModule? = CareModule.entries.firstOrNull { it.name == name }
    }
}

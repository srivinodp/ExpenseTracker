package com.example.expensetracker.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.expensetracker.util.PreferenceUtils
import java.util.Calendar

object ExpenseNotificationScheduler {
    fun scheduleDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ExpenseNotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1002,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Cancel any existing alarm first to ensure a clean reschedule
        alarmManager.cancel(pendingIntent)

        // Check if daily reminder is enabled by user
        val isEnabled = PreferenceUtils.isReminderEnabled(context)
        if (!isEnabled) {
            Log.d("ExpenseNotification", "Daily reminder is disabled by user.")
            pendingIntent.cancel()
            return
        }

        // Get user custom hour and minute
        val hour = PreferenceUtils.getReminderHour(context)
        val minute = PreferenceUtils.getReminderMinute(context)

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // If the scheduled time has already passed today, schedule for tomorrow
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // Schedule using RTC_WAKEUP to wake up the device when the alarm triggers
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        Log.d("ExpenseNotification", "Daily notification scheduled for $hour:$minute. Next trigger: ${calendar.time}")
    }
}

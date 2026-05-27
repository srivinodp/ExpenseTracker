package com.example.expensetracker.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.expensetracker.MainActivity

class ExpenseNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule daily alarms on device reboot
            ExpenseNotificationScheduler.scheduleDailyNotification(context)
            return
        }

        // Display the travel expense logging notification
        showNotification(context)
        
        // Reschedule the alarm for the next day
        ExpenseNotificationScheduler.scheduleDailyNotification(context)
    }

    private fun showNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "expense_tracker_daily"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to log daily travel and other expenses"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // General App launch click intent (takes user to Dashboard)
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: Log Travel
        val travelIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "log_expense?category=Transport")
        }
        val travelPendingIntent = PendingIntent.getActivity(
            context,
            2001,
            travelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Log Food
        val foodIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("navigate_to", "log_expense?category=Food")
        }
        val foodPendingIntent = PendingIntent.getActivity(
            context,
            2002,
            foodIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Daily Expense Reminder")
            .setContentText("Did you spend on travel, food, or other things today? Log it in one tap!")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_compass, "Log Travel", travelPendingIntent)
            .addAction(android.R.drawable.ic_menu_today, "Log Food", foodPendingIntent)
            .build()

        notificationManager.notify(1001, notification)
    }
}

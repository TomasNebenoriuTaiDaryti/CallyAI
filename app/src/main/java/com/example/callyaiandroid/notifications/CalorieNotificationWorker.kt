package com.example.callyaiandroid.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.callyaiandroid.R
import com.example.callyaiandroid.data.Prefs
import com.example.callyaiandroid.network.RetrofitClient
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class CalorieNotificationWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)

        val notificationsEnabled = prefs.notificationsEnabledFlow.first()
        if (!notificationsEnabled) return Result.success()

        if (!CalorieNotificationScheduler.hasPermission(applicationContext)) {
            return Result.success()
        }

        val token = prefs.tokenFlow.first() ?: return Result.success()
        val dailyGoal = prefs.kcalFlow.first()

        return try {
            val today = LocalDate.now().toString()
            val entries = RetrofitClient.api.getDayEntries("Bearer $token", today)
            val consumed = entries.sumOf { it.totalCalories }
            val remaining = (dailyGoal - consumed).coerceAtLeast(0)

            if (remaining > 0) {
                showNotification(remaining)
            }
            Result.success()
        } catch (_: Exception) {
            Result.success()
        }
    }

    private fun showNotification(remaining: Int) {
        val channelId = CHANNEL_ID
        val manager = NotificationManagerCompat.from(applicationContext)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                applicationContext.getString(R.string.calorie_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = applicationContext.getString(R.string.calorie_channel_description)
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(applicationContext.getString(R.string.calorie_notification_title))
            .setContentText(
                applicationContext.getString(
                    R.string.calorie_notification_body,
                    remaining
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "calorie_reminder_channel"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "calorie_notification_work"
    }
}

object CalorieNotificationScheduler {
    fun hasPermission(context: Context): Boolean {
        val notificationsAllowed = NotificationManagerCompat.from(context).areNotificationsEnabled()
        if (!notificationsAllowed) return false

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun schedule(context: Context, repeatIntervalHours: Int) {
        if (!hasPermission(context)) return

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val safeInterval = repeatIntervalHours.coerceAtLeast(1).toLong()
        val request = PeriodicWorkRequestBuilder<CalorieNotificationWorker>(safeInterval, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CalorieNotificationWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(CalorieNotificationWorker.WORK_NAME)
    }
}
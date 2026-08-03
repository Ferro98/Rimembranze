package com.example.rimembranze.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.*
import com.example.rimembranze.R
import com.example.rimembranze.worker.DeadlineOneShotWorker
import java.util.concurrent.TimeUnit

object DeadlineReminderScheduler {

    private const val CHANNEL_ID    = "deadlines_channel"
    private const val UNIQUE_PREFIX = "deadline_"

    /**
     * Cancella TUTTI i worker precedenti per questa deadline, poi
     * schedula solo quelli previsti da [reminderDaysCsv].
     * Questo evita che worker "orfani" da CSV precedenti sopravvivano.
     */
    fun schedule(
        context: Context,
        deadlineId: Long,
        dueDateEpochMs: Long,
        reminderDaysCsv: String
    ) {
        ensureChannel(context)
        val wm  = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()

        // 1. Cancella tutti i worker esistenti per questa deadline
        wm.cancelAllWorkByTag(UNIQUE_PREFIX + deadlineId)

        // 2. Schedula solo i giorni presenti nel CSV corrente
        reminderDaysCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .forEach { days ->
                val triggerMs = dueDateEpochMs - days.toLong() * 24 * 60 * 60 * 1000
                val delayMs   = triggerMs - now

                if (delayMs <= 0) return@forEach   // preavviso già passato, skip

                val request = OneTimeWorkRequestBuilder<DeadlineOneShotWorker>()
                    .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            "deadlineId" to deadlineId,
                            "daysLeft"   to days
                        )
                    )
                    .addTag(UNIQUE_PREFIX + deadlineId)
                    .build()

                wm.enqueueUniqueWork(
                    "${UNIQUE_PREFIX}${deadlineId}_${days}d",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
    }

    /**
     * Cancella TUTTI i preavvisi di una deadline.
     * Chiamare quando la deadline viene eliminata o segnata come pagata.
     */
    fun cancel(context: Context, deadlineId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(UNIQUE_PREFIX + deadlineId)
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    fun notifySimple(context: Context, title: String, text: String) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    private fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID, "Scadenze", NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
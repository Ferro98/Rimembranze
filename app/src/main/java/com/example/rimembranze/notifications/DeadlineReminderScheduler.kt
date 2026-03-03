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
     * Schedula UN worker per ogni valore in [reminderDaysCsv].
     * Ogni worker ha un uniqueName "deadline_{id}_{days}d" → REPLACE
     * gestisce automaticamente il rescheduling se la data cambia.
     *
     * I preavvisi già scaduti vengono ignorati silenziosamente.
     */
    fun schedule(
        context: Context,
        deadlineId: Long,
        dueDateEpochMs: Long,
        reminderDaysCsv: String = "30,14,7,1"
    ) {
        ensureChannel(context)
        val wm  = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()

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
                            "daysLeft"   to days        // usato nel testo della notifica
                        )
                    )
                    .addTag(UNIQUE_PREFIX + deadlineId) // tag comune → cancel by tag
                    .build()

                wm.enqueueUniqueWork(
                    "${UNIQUE_PREFIX}${deadlineId}_${days}d",
                    ExistingWorkPolicy.REPLACE,
                    request
                )
            }
    }

    /**
     * Cancella TUTTI i preavvisi di una deadline (tutti i days).
     * Chiamare quando la deadline viene eliminata o segnata come pagata.
     */
    fun cancel(context: Context, deadlineId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(UNIQUE_PREFIX + deadlineId)
    }

    // Rimasto per compatibilità — mostra notifica immediata (es. per test)
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
            CHANNEL_ID,
            "Scadenze",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}
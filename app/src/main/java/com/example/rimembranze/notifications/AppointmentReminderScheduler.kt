package com.example.rimembranze.notifications

import android.content.Context
import androidx.work.*
import com.example.rimembranze.worker.AppointmentReminderWorker
import java.util.concurrent.TimeUnit

object AppointmentReminderScheduler {

    private const val UNIQUE_PREFIX = "appointment_"

    fun schedule(
        context: Context,
        appointmentId: Long,
        dateEpochMs: Long
    ) {
        val now = System.currentTimeMillis()
        val triggerMs = dateEpochMs - 60 * 60 * 1000L   // 1 ora prima
        val delayMs = triggerMs - now

        if (delayMs <= 0) return   // appuntamento già passato o tra meno di 1 ora

        val request = OneTimeWorkRequestBuilder<AppointmentReminderWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("appointmentId" to appointmentId))
            .addTag(UNIQUE_PREFIX + appointmentId)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_PREFIX + appointmentId,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, appointmentId: Long) {
        WorkManager.getInstance(context)
            .cancelAllWorkByTag(UNIQUE_PREFIX + appointmentId)
    }
}
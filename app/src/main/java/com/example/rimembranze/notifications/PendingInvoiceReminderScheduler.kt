package com.example.rimembranze.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.rimembranze.worker.PendingInvoiceReminderWorker
import java.util.concurrent.TimeUnit

/**
 * Schedula il controllo settimanale delle sedute da fatturare in sospeso.
 * Va chiamato ad ogni avvio dell'app: [ExistingPeriodicWorkPolicy.KEEP] fa sì che il lavoro
 * periodico venga effettivamente creato una volta sola, gli avvii successivi sono no-op.
 */
object PendingInvoiceReminderScheduler {

    private const val UNIQUE_NAME = "pending_invoice_check"

    fun ensureScheduled(context: Context) {
        val request = PeriodicWorkRequestBuilder<PendingInvoiceReminderWorker>(7, TimeUnit.DAYS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}

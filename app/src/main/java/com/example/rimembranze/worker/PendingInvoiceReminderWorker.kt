package com.example.rimembranze.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rimembranze.MainActivity
import com.example.rimembranze.R
import com.example.rimembranze.data.db.AppDatabase

/**
 * Controllo periodico (settimanale) delle sedute effettuate ma non ancora fatturate.
 * Se ce ne sono da più di [THRESHOLD_MS] (7 giorni), manda un'unica notifica riassuntiva —
 * non apre un item specifico dato che può riguardarne più di uno.
 */
class PendingInvoiceReminderWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val now = System.currentTimeMillis()
        val pending = AppDatabase.get(applicationContext)
            .appointmentDao()
            .getAllDoneNotPaid()
            .filter { now - it.dateEpochMs >= THRESHOLD_MS }

        if (pending.isEmpty()) return Result.success()

        val totalCents = pending.sumOf { it.amountCents ?: 0L }
        val body = if (totalCents > 0) {
            applicationContext.getString(
                R.string.notif_pending_invoice_body_with_amount,
                pending.size, "%.2f".format(totalCents / 100.0)
            )
        } else {
            applicationContext.getString(R.string.notif_pending_invoice_body, pending.size)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, NOTIF_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "pending_invoice_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(channelId, applicationContext.getString(R.string.notif_pending_invoice_channel),
                NotificationManager.IMPORTANCE_DEFAULT)
        )

        nm.notify(
            NOTIF_ID,
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(applicationContext.getString(R.string.notif_pending_invoice_title))
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()
        )

        return Result.success()
    }

    companion object {
        private const val THRESHOLD_MS = 7L * 24 * 60 * 60 * 1000
        private const val NOTIF_ID = 999_001
    }
}

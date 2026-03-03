package com.example.rimembranze.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.rimembranze.MainActivity
import com.example.rimembranze.R
import com.example.rimembranze.data.db.AppDatabase

class DeadlineOneShotWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val deadlineId = inputData.getLong("deadlineId", -1L)
        val daysLeft   = inputData.getInt("daysLeft", 7)
        if (deadlineId == -1L) return Result.failure()

        val deadline = AppDatabase.get(applicationContext)
            .deadlineDao()
            .getById(deadlineId) ?: return Result.success()

        val bodyText = when {
            daysLeft == 0 -> "Scade oggi!"
            daysLeft == 1 -> "Scade domani"
            else          -> "Scade tra $daysLeft giorni"
        }

        // Intent che apre MainActivity portando direttamente alla deadline
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("itemId", deadline.itemId)
            putExtra("deadlineId", deadlineId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            // requestCode unico per ogni notifica
            (deadlineId * 100 + daysLeft).and(0x7FFFFFFF).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        showNotification(
            title         = deadline.category,
            body          = bodyText,
            notifId       = (deadlineId * 100 + daysLeft).and(0x7FFFFFFF).toInt(),
            pendingIntent = pendingIntent
        )

        return Result.success()
    }

    private fun showNotification(
        title: String,
        body: String,
        notifId: Int,
        pendingIntent: PendingIntent
    ) {
        val channelId = "deadlines_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, "Scadenze", NotificationManager.IMPORTANCE_DEFAULT)
        )

        nm.notify(
            notifId,
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)   // ← tap → apre l'app sulla deadline
                .build()
        )
    }
}
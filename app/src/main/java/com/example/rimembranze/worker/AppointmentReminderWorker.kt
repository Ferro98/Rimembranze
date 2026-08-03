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

class AppointmentReminderWorker(
    ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val appointmentId = inputData.getLong("appointmentId", -1L)
        if (appointmentId == -1L) return Result.failure()

        val appointment = AppDatabase.get(applicationContext)
            .appointmentDao()
            .getById(appointmentId) ?: return Result.success() // già eliminato

        // Non notificare se già effettuato
        if (appointment.isDone) return Result.success()

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("itemId", appointment.itemId)
            putExtra("appointmentId", appointmentId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (appointmentId + 50000L).and(0x7FFFFFFF).toInt(), // offset per non collidere con deadline
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "appointments_channel"
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        nm.createNotificationChannel(
            NotificationChannel(channelId, "Appuntamenti", NotificationManager.IMPORTANCE_HIGH)
        )

        nm.notify(
            (appointmentId + 50000L).and(0x7FFFFFFF).toInt(),
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Appuntamento tra 1 ora")
                .setContentText(appointment.title)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .build()
        )

        return Result.success()
    }
}
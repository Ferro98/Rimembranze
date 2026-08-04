package com.example.rimembranze

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.rimembranze.notifications.PendingInvoiceReminderScheduler
import com.example.rimembranze.ui.theme.RimembranzeTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* gestito dal sistema */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Rimembranze contiene dati personali (scadenze mediche, pagamenti, appuntamenti).
        // FLAG_SECURE blocca screenshot/screen recording e nasconde l'anteprima dell'app
        // nello switcher "app recenti" — sempre attivo per design, non è un bug e non è
        // (ancora) disattivabile dall'utente.
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Idempotente: ExistingPeriodicWorkPolicy.KEEP fa sì che il controllo settimanale
        // delle sedute da fatturare venga schedulato una volta sola, non ad ogni avvio.
        PendingInvoiceReminderScheduler.ensureScheduled(this)

        // Legge gli extra inviati dal PendingIntent della notifica
        // Sia al primo avvio (onCreate) sia quando l'app è già aperta (onNewIntent)
        val initialItemId        = intent?.getLongExtra("itemId", -1L).takeIf { it != -1L }
        val initialDeadlineId    = intent?.getLongExtra("deadlineId", -1L).takeIf { it != -1L }
        val initialAppointmentId = intent?.getLongExtra("appointmentId", -1L).takeIf { it != -1L }

        setContent {
            RimembranzeTheme {
                VaultApp(
                    initialItemId        = initialItemId,
                    initialDeadlineId    = initialDeadlineId,
                    initialAppointmentId = initialAppointmentId
                )
            }
        }
    }

    // Chiamato quando l'app è già in foreground/background e arriva un nuovo intent
    // (FLAG_ACTIVITY_SINGLE_TOP nel worker garantisce che passi da qui)
    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)   // aggiorna l'intent corrente — VaultApp lo rileggerà al prossimo recompose
        // Per forzare la navigazione rigeneriamo il content con i nuovi extra
        val itemId        = intent.getLongExtra("itemId", -1L).takeIf { it != -1L }
        val deadlineId    = intent.getLongExtra("deadlineId", -1L).takeIf { it != -1L }
        val appointmentId = intent.getLongExtra("appointmentId", -1L).takeIf { it != -1L }
        setContent {
            RimembranzeTheme {
                VaultApp(
                    initialItemId        = itemId,
                    initialDeadlineId    = deadlineId,
                    initialAppointmentId = appointmentId
                )
            }
        }
    }
}
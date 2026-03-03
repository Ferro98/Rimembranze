package com.example.rimembranze

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.rimembranze.ui.theme.RimembranzeTheme

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* gestito dal sistema */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Legge gli extra inviati dal PendingIntent della notifica
        // Sia al primo avvio (onCreate) sia quando l'app è già aperta (onNewIntent)
        val initialItemId     = intent?.getLongExtra("itemId", -1L).takeIf { it != -1L }
        val initialDeadlineId = intent?.getLongExtra("deadlineId", -1L).takeIf { it != -1L }

        setContent {
            RimembranzeTheme {
                VaultApp(
                    initialItemId     = initialItemId,
                    initialDeadlineId = initialDeadlineId
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
        val itemId     = intent.getLongExtra("itemId", -1L).takeIf { it != -1L }
        val deadlineId = intent.getLongExtra("deadlineId", -1L).takeIf { it != -1L }
        setContent {
            RimembranzeTheme {
                VaultApp(
                    initialItemId     = itemId,
                    initialDeadlineId = deadlineId
                )
            }
        }
    }
}
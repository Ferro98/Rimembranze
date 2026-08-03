package com.example.rimembranze

import androidx.compose.runtime.Composable
import com.example.rimembranze.ui.MainScreen

@Composable
fun VaultApp(
    initialItemId: Long? = null,
    initialDeadlineId: Long? = null,
    initialAppointmentId: Long? = null
) {
    MainScreen(
        initialItemId        = initialItemId,
        initialDeadlineId    = initialDeadlineId,
        initialAppointmentId = initialAppointmentId
    )
}
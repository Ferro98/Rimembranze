package com.example.rimembranze.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rimembranze.data.db.AppDatabase
import com.example.rimembranze.data.db.DeadlineEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.util.*

class DashboardViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    /** Mezzanotte di oggi (00:00:00.000) */
    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    /** Fine del giorno N da oggi (23:59:59.999) */
    private fun endOfDayPlusDays(days: Int): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
        add(Calendar.DAY_OF_YEAR, days)
    }.timeInMillis

    val upcoming: StateFlow<List<DeadlineEntity>> =
        db.deadlineDao()
            .observeUpcoming(
                now   = startOfToday(),       // 00:00 di oggi → include scadenze di oggi
                until = endOfDayPlusDays(30)  // 23:59 tra 30 giorni → include l'ultimo giorno
            )
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

    val expired: StateFlow<List<DeadlineEntity>> =
        db.deadlineDao()
            .observeExpired(now = startOfToday())
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                emptyList()
            )

}
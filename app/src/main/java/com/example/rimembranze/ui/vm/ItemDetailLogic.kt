package com.example.rimembranze.ui.vm

import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.RecordEntity
import java.util.Calendar

/**
 * Pure logic pulled out of ItemDetailViewModel so it can be unit-tested without Room/Android:
 * plain data in, plain data out, no DB access or side effects.
 */

fun computeItemStats(
    records: List<RecordEntity>,
    paidAppointments: List<AppointmentEntity>,
    doneNotPaidAppointments: List<AppointmentEntity>,
    yearStartEpochMs: Long
): ItemStats {
    val totalSpent = records.sumOf { it.amountCents ?: 0L }
    val totalThisYear = records
        .filter { it.dateEpochMs >= yearStartEpochMs }
        .sumOf { it.amountCents ?: 0L }

    val allDone = paidAppointments + doneNotPaidAppointments
    val paidWithAmount = allDone.filter { (it.amountCents ?: 0L) > 0L }
    val avgCents = if (paidWithAmount.isEmpty()) null
    else paidWithAmount.sumOf { it.amountCents!! } / paidWithAmount.size

    return ItemStats(
        totalSpentCents         = totalSpent,
        totalSpentThisYearCents = totalThisYear,
        avgAppointmentCents     = avgCents,
        completedAppointments   = allDone.size
    )
}

/** Mezzanotte del 1° gennaio dell'anno corrente, per il filtro "quest'anno" delle statistiche. */
fun currentYearStartEpochMs(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

data class HomeDashboardStats(
    val spentThisMonthCents: Long,
    val upcomingEstimatedCents: Long
)

/**
 * Riepilogo a livello di app per la card in home: stesso principio di [computeItemStats]
 * (dati puri in ingresso/uscita, niente Room) ma aggregato su tutti gli item invece che uno solo.
 */
fun computeHomeDashboardStats(
    records: List<RecordEntity>,
    appointments: List<AppointmentEntity>,
    upcomingDeadlines: List<DeadlineEntity>,
    monthStartEpochMs: Long,
    monthEndEpochMs: Long
): HomeDashboardStats {
    val spentThisMonth = records
        .filter { it.dateEpochMs in monthStartEpochMs..monthEndEpochMs }
        .sumOf { it.amountCents ?: 0L } +
        appointments
            .filter { it.isPaid && it.dateEpochMs in monthStartEpochMs..monthEndEpochMs }
            .sumOf { it.amountCents ?: 0L }

    val upcomingEstimated = upcomingDeadlines.sumOf { it.lastCostCents ?: 0L }

    return HomeDashboardStats(spentThisMonth, upcomingEstimated)
}

/** Mezzanotte del primo giorno del mese corrente. */
fun currentMonthStartEpochMs(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** Ultimo istante (23:59:59.999) dell'ultimo giorno del mese corrente. */
fun currentMonthEndEpochMs(now: Long = System.currentTimeMillis()): Long =
    Calendar.getInstance().apply {
        timeInMillis = now
        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999)
    }.timeInMillis

/**
 * Prossima data di scadenza per una ricorrenza, a partire dalla data attuale.
 * Ritorna null per "NONE" (nessuna ricorrenza) o un valore di ricorrenza sconosciuto.
 */
fun nextRecurrenceDate(currentDueDateEpochMs: Long, recurrence: String): Long? {
    if (recurrence == "NONE") return null
    val cal = Calendar.getInstance().apply { timeInMillis = currentDueDateEpochMs }
    when (recurrence) {
        "MONTHLY"    -> cal.add(Calendar.MONTH, 1)
        "QUARTERLY"  -> cal.add(Calendar.MONTH, 3)
        "SEMIANNUAL" -> cal.add(Calendar.MONTH, 6)
        "YEARLY"     -> cal.add(Calendar.YEAR, 1)
        else         -> return null
    }
    return cal.timeInMillis
}

/**
 * Escape di un singolo campo CSV secondo RFC4180: se contiene virgola, virgolette o a-capo,
 * viene racchiuso tra virgolette (raddoppiando eventuali virgolette interne).
 */
fun csvEscape(value: String): String =
    if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }

fun csvRow(fields: List<String>): String = fields.joinToString(",") { csvEscape(it) }

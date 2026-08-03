package com.example.rimembranze.ui.vm

import com.example.rimembranze.data.db.AppointmentEntity
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

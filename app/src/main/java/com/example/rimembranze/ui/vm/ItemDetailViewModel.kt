package com.example.rimembranze.ui.vm

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rimembranze.data.db.AppDatabase
import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.RecordEntity
import com.example.rimembranze.data.db.RecordType
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class ItemStats(
    val totalSpentCents: Long = 0L,           // tutti i record con importo
    val totalSpentThisYearCents: Long = 0L,   // record anno corrente
    val avgAppointmentCents: Long? = null,    // media sedute pagate
    val completedAppointments: Int = 0        // sedute totali effettuate
)

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: ItemEntity? = null,
    val deadlines: List<DeadlineEntity> = emptyList(),
    val records: List<RecordEntity> = emptyList(),
    val appointmentsPending: List<AppointmentEntity> = emptyList(),
    val appointmentsDoneNotPaid: List<AppointmentEntity> = emptyList(),
    val appointmentsPaid: List<AppointmentEntity> = emptyList(),
    val appointmentsAscending: Boolean = true,
    val stats: ItemStats = ItemStats(),
    // Feedback pagamento ricorrente: nuova data dopo markAsPaid
    val lastPaidNextDueDate: Long? = null
)

class ItemDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val _appointmentsAscending = MutableStateFlow(true)
    // Notifica temporanea nuova scadenza dopo pagamento ricorrente
    private val _lastPaidNextDueDate = MutableStateFlow<Long?>(null)

    // Feedback temporaneo esito export CSV (true = ok, false = fallito, null = nessun esito da mostrare)
    private val _csvExportResult = MutableStateFlow<Boolean?>(null)
    val csvExportResult: StateFlow<Boolean?> = _csvExportResult.asStateFlow()

    fun clearCsvExportResult() {
        _csvExportResult.value = null
    }

    fun observe(itemId: Long): StateFlow<ItemDetailUiState> {
        return combine(
            db.itemDao().observeById(itemId),
            db.deadlineDao().observeByItem(itemId),
            db.recordDao().observeByItem(itemId),
            db.appointmentDao().observePending(itemId),
            db.appointmentDao().observeDoneNotPaid(itemId),
            db.appointmentDao().observePaid(itemId),
            combine(_appointmentsAscending, _lastPaidNextDueDate) { asc, next -> asc to next }
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val records     = values[2] as List<RecordEntity>
            val pending     = values[3] as List<AppointmentEntity>
            val doneNotPaid = values[4] as List<AppointmentEntity>
            val paid        = values[5] as List<AppointmentEntity>
            val pair        = values[6] as Pair<*, *>
            val asc         = pair.first as Boolean
            val nextDue     = pair.second as? Long

            // ── Calcolo statistiche ───────────────────────────────────────
            val stats = computeItemStats(
                records                 = records,
                paidAppointments        = paid,
                doneNotPaidAppointments = doneNotPaid,
                yearStartEpochMs        = currentYearStartEpochMs()
            )

            ItemDetailUiState(
                isLoading               = false,
                item                    = values[0] as? ItemEntity,
                deadlines               = values[1] as List<DeadlineEntity>,
                records                 = records,
                appointmentsPending     = if (asc) pending else pending.reversed(),
                appointmentsDoneNotPaid = if (asc) doneNotPaid else doneNotPaid.reversed(),
                appointmentsPaid        = if (asc) paid else paid.reversed(),
                appointmentsAscending   = asc,
                stats                   = stats,
                lastPaidNextDueDate     = nextDue
            )
        }.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailUiState(isLoading = true)
        )
    }

    fun toggleAppointmentsOrder() {
        _appointmentsAscending.value = !_appointmentsAscending.value
    }

    /** Chiamato dalla UI dopo che il Snackbar è stato mostrato */
    fun clearNextDueDateFeedback() {
        _lastPaidNextDueDate.value = null
    }

    // ── Items ─────────────────────────────────────────────────────────────────

    fun deleteItem(item: ItemEntity, onDeleted: () -> Unit) {
        viewModelScope.launch { db.itemDao().delete(item); onDeleted() }
    }

    // ── Deadlines ─────────────────────────────────────────────────────────────

    suspend fun addDeadlineAndReturnId(
        itemId: Long, category: String, dueDateMs: Long, recurrence: String,
        notes: String? = null, amountCents: Long? = null, reminderDaysCsv: String = "14,7"
    ): Long = db.deadlineDao().insert(
        DeadlineEntity(itemId = itemId, category = category, dueDateEpochMs = dueDateMs,
            recurrence = recurrence, notes = notes, lastCostCents = amountCents,
            reminderDaysCsv = reminderDaysCsv)
    )

    fun updateDeadline(deadline: DeadlineEntity) {
        viewModelScope.launch { db.deadlineDao().update(deadline) }
    }

    fun deleteDeadline(deadline: DeadlineEntity) {
        viewModelScope.launch { db.deadlineDao().delete(deadline) }
    }

    suspend fun markAsPaidAndReturnNextDueDate(
        deadline: DeadlineEntity, amountCents: Long? = null, notes: String? = null
    ): Long? {
        val now = System.currentTimeMillis()
        db.recordDao().insert(
            RecordEntity(itemId = deadline.itemId, deadlineId = deadline.id,
                type = RecordType.Pagamento.name, title = deadline.category,
                dateEpochMs = now, amountCents = amountCents ?: deadline.lastCostCents, notes = notes)
        )
        val updated = deadline.copy(lastPaidEpochMs = now, lastCostCents = amountCents ?: deadline.lastCostCents)
        val nextDue = nextRecurrenceDate(deadline.dueDateEpochMs, deadline.recurrence)
        return if (nextDue != null) {
            db.deadlineDao().update(updated.copy(dueDateEpochMs = nextDue))
            _lastPaidNextDueDate.value = nextDue   // ← feedback UI
            nextDue
        } else {
            db.deadlineDao().delete(updated)
            null
        }
    }

    // ── Records ───────────────────────────────────────────────────────────────

    suspend fun addRecordAndReturnId(
        itemId: Long, type: RecordType, title: String, dateEpochMs: Long,
        amountCents: Long? = null, notes: String? = null, deadlineId: Long? = null
    ): Long = db.recordDao().insert(
        RecordEntity(itemId = itemId, deadlineId = deadlineId, type = type.name,
            title = title, dateEpochMs = dateEpochMs, amountCents = amountCents, notes = notes)
    )

    fun deleteRecord(record: RecordEntity) { viewModelScope.launch { db.recordDao().delete(record) } }

    fun updateRecordUniSalute(record: RecordEntity, sent: Boolean, status: String?, sentEpochMs: Long?) {
        viewModelScope.launch {
            db.recordDao().update(record.copy(unisaluteSent = sent,
                unisaluteStatus = status, unisaluteSentEpochMs = sentEpochMs))
        }
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    suspend fun addAppointmentAndReturnId(
        itemId: Long, title: String, dateEpochMs: Long,
        notes: String? = null, amountCents: Long? = null
    ): Long = db.appointmentDao().insert(
        AppointmentEntity(itemId = itemId, title = title, dateEpochMs = dateEpochMs,
            notes = notes, amountCents = amountCents)
    )

    fun markAppointmentDone(appointment: AppointmentEntity, notes: String? = null, amountCents: Long? = null) {
        viewModelScope.launch {
            db.appointmentDao().update(appointment.copy(isDone = true,
                notes = notes ?: appointment.notes, amountCents = amountCents ?: appointment.amountCents))
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch { db.appointmentDao().delete(appointment) }
    }

    fun createInvoice(itemId: Long, appointments: List<AppointmentEntity>, notes: String? = null) {
        viewModelScope.launch {
            db.appointmentDao().markAsPaid(appointments.map { it.id })
            val total = appointments.sumOf { it.amountCents ?: 0L }
            db.recordDao().insert(RecordEntity(itemId = itemId, type = RecordType.Pagamento.name,
                title = "Fattura (${appointments.size} sedute)", dateEpochMs = System.currentTimeMillis(),
                amountCents = total.takeIf { it > 0 }, notes = notes))
        }
    }

    // ── Export CSV ────────────────────────────────────────────────────────────

    /** Genera il contenuto CSV come stringa — chiamato dalla UI prima di aprire il file picker */
    fun buildCsvContent(records: List<RecordEntity>, appointments: List<AppointmentEntity>): String {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.ITALY)
        val sb = StringBuilder()
        sb.appendLine(csvRow(listOf("Tipo", "Titolo", "Data", "Importo (€)", "Note")))
        records.forEach { r ->
            val amount = r.amountCents?.let { "%.2f".format(it / 100.0) } ?: ""
            sb.appendLine(csvRow(listOf(
                r.type, r.title, fmt.format(Date(r.dateEpochMs)), amount, r.notes ?: ""
            )))
        }
        appointments.forEach { a ->
            val amount = a.amountCents?.let { "%.2f".format(it / 100.0) } ?: ""
            val stato  = when { a.isPaid -> "Fatturata"; a.isDone -> "Effettuata"; else -> "Programmata" }
            sb.appendLine(csvRow(listOf(
                "Seduta ($stato)", a.title, fmt.format(Date(a.dateEpochMs)), amount, a.notes ?: ""
            )))
        }
        return sb.toString()
    }

    /** Scrive il CSV nell'Uri scelto dall'utente tramite ACTION_CREATE_DOCUMENT */
    fun writeCsvToUri(context: Context, uri: Uri, content: String) {
        viewModelScope.launch {
            _csvExportResult.value = try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
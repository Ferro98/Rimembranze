package com.example.rimembranze.ui.vm

import android.app.Application
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
import java.util.Calendar

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: ItemEntity? = null,
    val deadlines: List<DeadlineEntity> = emptyList(),
    val records: List<RecordEntity> = emptyList(),
    val appointmentsPending: List<AppointmentEntity> = emptyList(),
    val appointmentsDoneNotPaid: List<AppointmentEntity> = emptyList(),
    val appointmentsPaid: List<AppointmentEntity> = emptyList(),
    val appointmentsAscending: Boolean = true   // toggle ordinamento
)

class ItemDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    // Toggle ordinamento appuntamenti — persiste per tutta la sessione
    private val _appointmentsAscending = MutableStateFlow(true)

    fun observe(itemId: Long): StateFlow<ItemDetailUiState> {
        return combine(
            db.itemDao().observeById(itemId),
            db.deadlineDao().observeByItem(itemId),
            db.recordDao().observeByItem(itemId),
            db.appointmentDao().observePending(itemId),
            db.appointmentDao().observeDoneNotPaid(itemId),
            db.appointmentDao().observePaid(itemId),
            _appointmentsAscending
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val pending     = values[3] as List<AppointmentEntity>
            val doneNotPaid = values[4] as List<AppointmentEntity>
            val paid        = values[5] as List<AppointmentEntity>
            val asc         = values[6] as Boolean

            ItemDetailUiState(
                isLoading               = false,
                item                    = values[0] as? ItemEntity,
                deadlines               = values[1] as List<DeadlineEntity>,
                records                 = values[2] as List<RecordEntity>,
                appointmentsPending     = if (asc) pending else pending.reversed(),
                appointmentsDoneNotPaid = if (asc) doneNotPaid else doneNotPaid.reversed(),
                appointmentsPaid        = if (asc) paid else paid.reversed(),
                appointmentsAscending   = asc
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

    // ── Items ─────────────────────────────────────────────────────────────────

    fun deleteItem(item: ItemEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            db.itemDao().delete(item)
            onDeleted()
        }
    }

    // ── Deadlines ─────────────────────────────────────────────────────────────

    suspend fun addDeadlineAndReturnId(
        itemId: Long,
        category: String,
        dueDateMs: Long,
        recurrence: String,
        notes: String? = null,
        amountCents: Long? = null,
        reminderDaysCsv: String = "30,14,7,1,0"
    ): Long = db.deadlineDao().insert(
        DeadlineEntity(
            itemId          = itemId,
            category        = category,
            dueDateEpochMs  = dueDateMs,
            recurrence      = recurrence,
            notes           = notes,
            lastCostCents   = amountCents,
            reminderDaysCsv = reminderDaysCsv
        )
    )

    fun updateDeadline(deadline: DeadlineEntity) {
        viewModelScope.launch { db.deadlineDao().update(deadline) }
    }

    fun deleteDeadline(deadline: DeadlineEntity) {
        viewModelScope.launch { db.deadlineDao().delete(deadline) }
    }

    suspend fun markAsPaidAndReturnNextDueDate(
        deadline: DeadlineEntity,
        amountCents: Long? = null,
        notes: String? = null
    ): Long? {
        val now = System.currentTimeMillis()
        db.recordDao().insert(
            RecordEntity(
                itemId      = deadline.itemId,
                deadlineId  = deadline.id,
                type        = RecordType.Pagamento.name,
                title       = deadline.category,
                dateEpochMs = now,
                amountCents = amountCents ?: deadline.lastCostCents,
                notes       = notes
            )
        )
        val updated = deadline.copy(
            lastPaidEpochMs = now,
            lastCostCents   = amountCents ?: deadline.lastCostCents
        )
        return if (deadline.recurrence != "NONE") {
            val cal = Calendar.getInstance().apply { timeInMillis = deadline.dueDateEpochMs }
            when (deadline.recurrence) {
                "MONTHLY"    -> cal.add(Calendar.MONTH, 1)
                "QUARTERLY"  -> cal.add(Calendar.MONTH, 3)
                "SEMIANNUAL" -> cal.add(Calendar.MONTH, 6)
                "YEARLY"     -> cal.add(Calendar.YEAR, 1)
            }
            val nextDue = cal.timeInMillis
            db.deadlineDao().update(updated.copy(dueDateEpochMs = nextDue))
            nextDue
        } else {
            db.deadlineDao().delete(updated)
            null
        }
    }

    // ── Records ───────────────────────────────────────────────────────────────

    suspend fun addRecordAndReturnId(
        itemId: Long,
        type: RecordType,
        title: String,
        dateEpochMs: Long,
        amountCents: Long? = null,
        notes: String? = null,
        deadlineId: Long? = null
    ): Long = db.recordDao().insert(
        RecordEntity(
            itemId      = itemId,
            deadlineId  = deadlineId,
            type        = type.name,
            title       = title,
            dateEpochMs = dateEpochMs,
            amountCents = amountCents,
            notes       = notes
        )
    )

    fun deleteRecord(record: RecordEntity) {
        viewModelScope.launch { db.recordDao().delete(record) }
    }

    fun updateRecordUniSalute(
        record: RecordEntity,
        sent: Boolean,
        status: String?,
        sentEpochMs: Long?
    ) {
        viewModelScope.launch {
            db.recordDao().update(
                record.copy(
                    unisaluteSent        = sent,
                    unisaluteStatus      = status,
                    unisaluteSentEpochMs = sentEpochMs
                )
            )
        }
    }

    // ── Appointments ──────────────────────────────────────────────────────────

    suspend fun addAppointmentAndReturnId(
        itemId: Long,
        title: String,
        dateEpochMs: Long,
        notes: String? = null,
        amountCents: Long? = null
    ): Long = db.appointmentDao().insert(
        AppointmentEntity(
            itemId      = itemId,
            title       = title,
            dateEpochMs = dateEpochMs,
            notes       = notes,
            amountCents = amountCents
        )
    )

    fun markAppointmentDone(
        appointment: AppointmentEntity,
        notes: String? = null,
        amountCents: Long? = null
    ) {
        viewModelScope.launch {
            db.appointmentDao().update(
                appointment.copy(
                    isDone      = true,
                    notes       = notes ?: appointment.notes,
                    amountCents = amountCents ?: appointment.amountCents
                )
            )
        }
    }

    fun deleteAppointment(appointment: AppointmentEntity) {
        viewModelScope.launch { db.appointmentDao().delete(appointment) }
    }

    fun createInvoice(
        itemId: Long,
        appointments: List<AppointmentEntity>,
        notes: String? = null
    ) {
        viewModelScope.launch {
            db.appointmentDao().markAsPaid(appointments.map { it.id })
            val total = appointments.sumOf { it.amountCents ?: 0L }
            db.recordDao().insert(
                RecordEntity(
                    itemId      = itemId,
                    type        = RecordType.Pagamento.name,
                    title       = "Fattura (${appointments.size} sedute)",
                    dateEpochMs = System.currentTimeMillis(),
                    amountCents = total.takeIf { it > 0 },
                    notes       = notes
                )
            )
        }
    }
}
package com.example.rimembranze.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rimembranze.data.db.AppDatabase
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.RecordEntity
import com.example.rimembranze.data.db.RecordType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: ItemEntity? = null,
    val deadlines: List<DeadlineEntity> = emptyList(),
    val records: List<RecordEntity> = emptyList()
)

class ItemDetailViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)

    fun observe(itemId: Long): StateFlow<ItemDetailUiState> {
        return combine(
            db.itemDao().observeById(itemId),
            db.deadlineDao().observeByItem(itemId),
            db.recordDao().observeByItem(itemId)
        ) { item, deadlines, records ->
            ItemDetailUiState(
                isLoading = false,
                item      = item,
                deadlines = deadlines,
                records   = records
            )
        }.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemDetailUiState(isLoading = true)
        )
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

    fun markAsPaid(
        deadline: DeadlineEntity,
        amountCents: Long? = null,
        notes: String? = null
    ) {
        viewModelScope.launch {
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
            if (deadline.recurrence == "YEARLY") {
                val cal = Calendar.getInstance().apply { timeInMillis = deadline.dueDateEpochMs }
                cal.add(Calendar.YEAR, 1)
                db.deadlineDao().update(updated.copy(dueDateEpochMs = cal.timeInMillis))
            } else {
                db.deadlineDao().delete(updated)
            }
        }
    }

    // Variante che ritorna la prossima dueDateEpochMs (per rischedulare le notifiche)
    // null = deadline eliminata (non ricorrente)
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
}
package com.example.rimembranze.data.backup

import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.data.db.RecordEntity
import kotlinx.serialization.Serializable

// Formato di backup completo — pensato per essere stabile nel tempo (enum salvati
// come stringa, come già fa Room via Converters.kt) e adatto a un futuro sync.
// Bump di schemaVersion se la struttura cambia in modo incompatibile.
const val BACKUP_SCHEMA_VERSION = 1

@Serializable
data class BackupFile(
    val schemaVersion: Int = BACKUP_SCHEMA_VERSION,
    val exportedAtEpochMs: Long,
    val items: List<BackupItem>,
    val deadlines: List<BackupDeadline>,
    val records: List<BackupRecord>,
    val appointments: List<BackupAppointment>
)

@Serializable
data class BackupItem(
    val id: Long,
    val type: String,
    val name: String,
    val notes: String?,
    val createdAtEpochMs: Long
)

@Serializable
data class BackupDeadline(
    val id: Long,
    val itemId: Long,
    val category: String,
    val dueDateEpochMs: Long,
    val reminderDaysCsv: String,
    val recurrence: String,
    val lastCostCents: Long?,
    val lastPaidEpochMs: Long?,
    val notes: String?,
    val createdAtEpochMs: Long
)

@Serializable
data class BackupRecord(
    val id: Long,
    val itemId: Long,
    val deadlineId: Long?,
    val type: String,
    val title: String,
    val dateEpochMs: Long,
    val amountCents: Long?,
    val notes: String?,
    val unisaluteSent: Boolean,
    val unisaluteStatus: String?,
    val unisaluteSentEpochMs: Long?,
    val createdAtEpochMs: Long
)

@Serializable
data class BackupAppointment(
    val id: Long,
    val itemId: Long,
    val title: String,
    val dateEpochMs: Long,
    val notes: String?,
    val amountCents: Long?,
    val isDone: Boolean,
    val isPaid: Boolean,
    val createdAtEpochMs: Long
)

// ── Entity → Backup ───────────────────────────────────────────────────────────

fun ItemEntity.toBackup() = BackupItem(
    id = id, type = type.name, name = name, notes = notes, createdAtEpochMs = createdAtEpochMs
)

fun DeadlineEntity.toBackup() = BackupDeadline(
    id = id, itemId = itemId, category = category, dueDateEpochMs = dueDateEpochMs,
    reminderDaysCsv = reminderDaysCsv, recurrence = recurrence, lastCostCents = lastCostCents,
    lastPaidEpochMs = lastPaidEpochMs, notes = notes, createdAtEpochMs = createdAtEpochMs
)

fun RecordEntity.toBackup() = BackupRecord(
    id = id, itemId = itemId, deadlineId = deadlineId, type = type, title = title,
    dateEpochMs = dateEpochMs, amountCents = amountCents, notes = notes,
    unisaluteSent = unisaluteSent, unisaluteStatus = unisaluteStatus,
    unisaluteSentEpochMs = unisaluteSentEpochMs, createdAtEpochMs = createdAtEpochMs
)

fun AppointmentEntity.toBackup() = BackupAppointment(
    id = id, itemId = itemId, title = title, dateEpochMs = dateEpochMs, notes = notes,
    amountCents = amountCents, isDone = isDone, isPaid = isPaid, createdAtEpochMs = createdAtEpochMs
)

// ── Backup → Entity (id = 0 per lasciare che Room assegni un nuovo id) ────────
// itemId/deadlineId vengono passati già rimappati dal chiamante (BackupManager).

fun BackupItem.toEntity() = ItemEntity(
    id = 0, type = ItemType.valueOf(type), name = name, notes = notes,
    createdAtEpochMs = createdAtEpochMs
)

fun BackupDeadline.toEntity(newItemId: Long) = DeadlineEntity(
    id = 0, itemId = newItemId, category = category, dueDateEpochMs = dueDateEpochMs,
    reminderDaysCsv = reminderDaysCsv, recurrence = recurrence, lastCostCents = lastCostCents,
    lastPaidEpochMs = lastPaidEpochMs, notes = notes, createdAtEpochMs = createdAtEpochMs
)

fun BackupRecord.toEntity(newItemId: Long, newDeadlineId: Long?) = RecordEntity(
    id = 0, itemId = newItemId, deadlineId = newDeadlineId, type = type, title = title,
    dateEpochMs = dateEpochMs, amountCents = amountCents, notes = notes,
    unisaluteSent = unisaluteSent, unisaluteStatus = unisaluteStatus,
    unisaluteSentEpochMs = unisaluteSentEpochMs, createdAtEpochMs = createdAtEpochMs
)

fun BackupAppointment.toEntity(newItemId: Long) = AppointmentEntity(
    id = 0, itemId = newItemId, title = title, dateEpochMs = dateEpochMs, notes = notes,
    amountCents = amountCents, isDone = isDone, isPaid = isPaid, createdAtEpochMs = createdAtEpochMs
)

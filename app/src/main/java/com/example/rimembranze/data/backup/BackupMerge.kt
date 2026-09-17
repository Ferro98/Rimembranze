package com.example.rimembranze.data.backup

import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.RecordEntity
import kotlinx.serialization.json.Json

/**
 * Le due operazioni DAO che servono all'algoritmo di merge per un tipo di entità (leggere tutto,
 * inserire una riga e ottenerne l'id assegnato). Separarle così permette a [importBackupMerging]
 * di girare sia sui DAO Room veri (produzione) sia su semplici fake in memoria (test) — è
 * esattamente la logica che gira quando si ripristina un backup su un database vuoto (un nuovo
 * dispositivo, o una reinstallazione), quindi vale la pena poterla verificare senza un database
 * vero.
 */
class EntityAccess<T>(val getAllOnce: suspend () -> List<T>, val insert: suspend (T) -> Long)

private fun itemKey(type: String, name: String) = "$type|${name.trim().lowercase()}"

private fun deadlineKey(itemId: Long, category: String, dueDateEpochMs: Long) =
    "$itemId|${category.trim().lowercase()}|$dueDateEpochMs"

private fun recordKey(itemId: Long, type: String, title: String, dateEpochMs: Long, amountCents: Long?) =
    "$itemId|$type|${title.trim().lowercase()}|$dateEpochMs|$amountCents"

private fun appointmentKey(itemId: Long, title: String, dateEpochMs: Long) =
    "$itemId|${title.trim().lowercase()}|$dateEpochMs"

/**
 * Import additivo e deduplicato per contenuto: gli ID originali del backup non hanno senso su un
 * altro dispositivo/reinstallazione, quindi ogni riga viene confrontata con quelle già presenti in
 * base ai campi che ne definiscono l'identità "naturale" (vedi le funzioni `*Key` sopra). Se una
 * corrispondenza esiste già, viene riusata (nessun duplicato, nessuna modifica); altrimenti la riga
 * viene inserita come nuova con le chiavi esterne (itemId/deadlineId) rimappate di conseguenza.
 * Nulla di già presente viene mai sovrascritto o cancellato, e importare due volte lo stesso file
 * è sicuro (idempotente).
 */
suspend fun importBackupMerging(
    content: String,
    json: Json,
    items: EntityAccess<ItemEntity>,
    deadlines: EntityAccess<DeadlineEntity>,
    records: EntityAccess<RecordEntity>,
    appointments: EntityAccess<AppointmentEntity>
): BackupManager.ImportResult {
    val backup = json.decodeFromString(BackupFile.serializer(), content)
    require(backup.schemaVersion == BACKUP_SCHEMA_VERSION) {
        "Formato di backup non supportato (versione ${backup.schemaVersion})"
    }

    // old id → new/esistente id, così scadenze/appuntamenti/record puntano all'item giusto.
    // Le mappe "esistenti" partono da ciò che è già in DB e vengono aggiornate man mano che si
    // inserisce, così anche eventuali doppioni interni allo stesso file di backup vengono
    // deduplicati.
    val existingItems = items.getAllOnce()
        .associateByTo(mutableMapOf()) { itemKey(it.type.name, it.name) }

    val itemIdMap = mutableMapOf<Long, Long>()
    var newItems = 0
    backup.items.forEach { b ->
        val key = itemKey(b.type, b.name)
        val existing = existingItems[key]
        if (existing != null) {
            itemIdMap[b.id] = existing.id
        } else {
            val newId = items.insert(b.toEntity())
            itemIdMap[b.id] = newId
            existingItems[key] = b.toEntity().copy(id = newId)
            newItems++
        }
    }

    val existingDeadlines = deadlines.getAllOnce()
        .associateByTo(mutableMapOf()) { deadlineKey(it.itemId, it.category, it.dueDateEpochMs) }

    val deadlineIdMap = mutableMapOf<Long, Long>()
    var newDeadlines = 0
    backup.deadlines.forEach { b ->
        val newItemId = itemIdMap[b.itemId] ?: return@forEach
        val key = deadlineKey(newItemId, b.category, b.dueDateEpochMs)
        val existing = existingDeadlines[key]
        if (existing != null) {
            deadlineIdMap[b.id] = existing.id
        } else {
            val newId = deadlines.insert(b.toEntity(newItemId))
            deadlineIdMap[b.id] = newId
            existingDeadlines[key] = b.toEntity(newItemId).copy(id = newId)
            newDeadlines++
        }
    }

    val existingRecordKeys = records.getAllOnce()
        .mapTo(mutableSetOf()) { recordKey(it.itemId, it.type, it.title, it.dateEpochMs, it.amountCents) }

    var newRecords = 0
    backup.records.forEach { b ->
        val newItemId = itemIdMap[b.itemId] ?: return@forEach
        val key = recordKey(newItemId, b.type, b.title, b.dateEpochMs, b.amountCents)
        if (existingRecordKeys.add(key)) {
            val newDeadlineId = b.deadlineId?.let { deadlineIdMap[it] }
            records.insert(b.toEntity(newItemId, newDeadlineId))
            newRecords++
        }
    }

    val existingAppointmentKeys = appointments.getAllOnce()
        .mapTo(mutableSetOf()) { appointmentKey(it.itemId, it.title, it.dateEpochMs) }

    var newAppointments = 0
    backup.appointments.forEach { b ->
        val newItemId = itemIdMap[b.itemId] ?: return@forEach
        val key = appointmentKey(newItemId, b.title, b.dateEpochMs)
        if (existingAppointmentKeys.add(key)) {
            appointments.insert(b.toEntity(newItemId))
            newAppointments++
        }
    }

    return BackupManager.ImportResult(
        items = newItems,
        deadlines = newDeadlines,
        records = newRecords,
        appointments = newAppointments
    )
}

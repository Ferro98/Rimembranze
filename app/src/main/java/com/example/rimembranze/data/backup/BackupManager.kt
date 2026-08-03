package com.example.rimembranze.data.backup

import com.example.rimembranze.data.db.AppDatabase
import kotlinx.serialization.json.Json

/**
 * Esporta/importa un backup JSON completo (tutti gli item, scadenze, appuntamenti e record).
 * L'import è sempre additivo e deduplicato per contenuto: gli ID originali non hanno senso su un
 * altro dispositivo/reinstallazione, quindi ogni riga del backup viene confrontata con quelle già
 * presenti in base ai campi che ne definiscono l'identità "naturale" (vedi le funzioni `*Key`
 * sotto). Se una corrispondenza esiste già, viene riusata (nessun duplicato, nessuna modifica);
 * altrimenti la riga viene inserita come nuova (id = 0, Room ne assegna uno nuovo) con le chiavi
 * esterne (itemId/deadlineId) rimappate di conseguenza. Nulla di già presente viene mai
 * sovrascritto o cancellato, e importare due volte lo stesso file è sicuro (idempotente).
 */
class BackupManager(private val db: AppDatabase) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    data class ImportResult(
        val items: Int,
        val deadlines: Int,
        val records: Int,
        val appointments: Int
    )

    suspend fun exportAll(): String {
        val backup = BackupFile(
            exportedAtEpochMs = System.currentTimeMillis(),
            items = db.itemDao().getAllOnce().map { it.toBackup() },
            deadlines = db.deadlineDao().getAllOnce().map { it.toBackup() },
            records = db.recordDao().getAllOnce().map { it.toBackup() },
            appointments = db.appointmentDao().getAllOnce().map { it.toBackup() }
        )
        return json.encodeToString(BackupFile.serializer(), backup)
    }

    private fun itemKey(type: String, name: String) = "$type|${name.trim().lowercase()}"

    private fun deadlineKey(itemId: Long, category: String, dueDateEpochMs: Long) =
        "$itemId|${category.trim().lowercase()}|$dueDateEpochMs"

    private fun recordKey(itemId: Long, type: String, title: String, dateEpochMs: Long, amountCents: Long?) =
        "$itemId|$type|${title.trim().lowercase()}|$dateEpochMs|$amountCents"

    private fun appointmentKey(itemId: Long, title: String, dateEpochMs: Long) =
        "$itemId|${title.trim().lowercase()}|$dateEpochMs"

    suspend fun importMerging(content: String): ImportResult {
        val backup = json.decodeFromString(BackupFile.serializer(), content)
        require(backup.schemaVersion == BACKUP_SCHEMA_VERSION) {
            "Formato di backup non supportato (versione ${backup.schemaVersion})"
        }

        // old id → new/esistente id, così scadenze/appuntamenti/record puntano all'item giusto.
        // Le mappe "esistenti" partono da ciò che è già in DB e vengono aggiornate man mano che
        // si inserisce, così anche eventuali doppioni interni allo stesso file di backup vengono
        // deduplicati.
        val existingItems = db.itemDao().getAllOnce()
            .associateByTo(mutableMapOf()) { itemKey(it.type.name, it.name) }

        val itemIdMap = mutableMapOf<Long, Long>()
        var newItems = 0
        backup.items.forEach { b ->
            val key = itemKey(b.type, b.name)
            val existing = existingItems[key]
            if (existing != null) {
                itemIdMap[b.id] = existing.id
            } else {
                val newId = db.itemDao().insert(b.toEntity())
                itemIdMap[b.id] = newId
                existingItems[key] = b.toEntity().copy(id = newId)
                newItems++
            }
        }

        val existingDeadlines = db.deadlineDao().getAllOnce()
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
                val newId = db.deadlineDao().insert(b.toEntity(newItemId))
                deadlineIdMap[b.id] = newId
                existingDeadlines[key] = b.toEntity(newItemId).copy(id = newId)
                newDeadlines++
            }
        }

        val existingRecordKeys = db.recordDao().getAllOnce()
            .mapTo(mutableSetOf()) { recordKey(it.itemId, it.type, it.title, it.dateEpochMs, it.amountCents) }

        var newRecords = 0
        backup.records.forEach { b ->
            val newItemId = itemIdMap[b.itemId] ?: return@forEach
            val key = recordKey(newItemId, b.type, b.title, b.dateEpochMs, b.amountCents)
            if (existingRecordKeys.add(key)) {
                val newDeadlineId = b.deadlineId?.let { deadlineIdMap[it] }
                db.recordDao().insert(b.toEntity(newItemId, newDeadlineId))
                newRecords++
            }
        }

        val existingAppointmentKeys = db.appointmentDao().getAllOnce()
            .mapTo(mutableSetOf()) { appointmentKey(it.itemId, it.title, it.dateEpochMs) }

        var newAppointments = 0
        backup.appointments.forEach { b ->
            val newItemId = itemIdMap[b.itemId] ?: return@forEach
            val key = appointmentKey(newItemId, b.title, b.dateEpochMs)
            if (existingAppointmentKeys.add(key)) {
                db.appointmentDao().insert(b.toEntity(newItemId))
                newAppointments++
            }
        }

        return ImportResult(
            items = newItems,
            deadlines = newDeadlines,
            records = newRecords,
            appointments = newAppointments
        )
    }
}

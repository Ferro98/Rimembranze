package com.example.rimembranze.data.backup

import com.example.rimembranze.data.db.AppDatabase
import kotlinx.serialization.json.Json

/**
 * Esporta/importa un backup JSON completo (tutti gli item, scadenze, appuntamenti e record).
 * L'import è sempre additivo: ogni riga viene inserita come nuova (id = 0, Room ne assegna uno
 * nuovo) e le chiavi esterne (itemId/deadlineId) vengono rimappate di conseguenza — nulla di
 * già presente sul dispositivo viene mai toccato o cancellato.
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

    suspend fun importMerging(content: String): ImportResult {
        val backup = json.decodeFromString(BackupFile.serializer(), content)
        require(backup.schemaVersion == BACKUP_SCHEMA_VERSION) {
            "Formato di backup non supportato (versione ${backup.schemaVersion})"
        }

        // old id → new id, così scadenze/appuntamenti/record puntano al nuovo item
        val itemIdMap = mutableMapOf<Long, Long>()
        backup.items.forEach { b ->
            itemIdMap[b.id] = db.itemDao().insert(b.toEntity())
        }

        // old id → new id, così i record che referenziano una scadenza vengono rimappati
        val deadlineIdMap = mutableMapOf<Long, Long>()
        backup.deadlines.forEach { b ->
            val newItemId = itemIdMap[b.itemId] ?: return@forEach
            deadlineIdMap[b.id] = db.deadlineDao().insert(b.toEntity(newItemId))
        }

        var recordCount = 0
        backup.records.forEach { b ->
            val newItemId = itemIdMap[b.itemId] ?: return@forEach
            val newDeadlineId = b.deadlineId?.let { deadlineIdMap[it] }
            db.recordDao().insert(b.toEntity(newItemId, newDeadlineId))
            recordCount++
        }

        var appointmentCount = 0
        backup.appointments.forEach { b ->
            val newItemId = itemIdMap[b.itemId] ?: return@forEach
            db.appointmentDao().insert(b.toEntity(newItemId))
            appointmentCount++
        }

        return ImportResult(
            items = itemIdMap.size,
            deadlines = deadlineIdMap.size,
            records = recordCount,
            appointments = appointmentCount
        )
    }
}

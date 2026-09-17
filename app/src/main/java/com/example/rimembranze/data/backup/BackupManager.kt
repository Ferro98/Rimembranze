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

    // L'algoritmo di merge/dedup vero e proprio vive in BackupMerge.kt come funzione pura
    // (a parte le due operazioni DAO date via EntityAccess), così è testabile senza Room — questo
    // è solo un adattatore verso i DAO reali.
    suspend fun importMerging(content: String): ImportResult = importBackupMerging(
        content, json,
        items = EntityAccess(db.itemDao()::getAllOnce, db.itemDao()::insert),
        deadlines = EntityAccess(db.deadlineDao()::getAllOnce, db.deadlineDao()::insert),
        records = EntityAccess(db.recordDao()::getAllOnce, db.recordDao()::insert),
        appointments = EntityAccess(db.appointmentDao()::getAllOnce, db.appointmentDao()::insert)
    )
}

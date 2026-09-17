package com.example.rimembranze.data.backup

import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.data.db.RecordEntity
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Esercita il vero algoritmo di import/merge (quello usato in produzione da [BackupManager])
 * contro DAO finti in memoria, così si può verificare senza un dispositivo/emulatore lo scenario
 * che conta davvero: ripristinare un backup su un database vuoto (nuovo dispositivo, o
 * reinstallazione dell'app dopo una disinstallazione).
 */
class BackupMergeTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    /** DAO finto in memoria: stesso contratto (getAllOnce + insert che assegna un id) dei DAO Room veri. */
    private fun <T> fakeStore(withId: (T, Long) -> T): EntityAccess<T> {
        val rows = mutableListOf<T>()
        var nextId = 1L
        return EntityAccess(
            getAllOnce = { rows.toList() },
            insert = { e -> val id = nextId++; rows.add(withId(e, id)); id }
        )
    }

    private fun itemsAccess() = fakeStore<ItemEntity> { e, id -> e.copy(id = id) }
    private fun deadlinesAccess() = fakeStore<DeadlineEntity> { e, id -> e.copy(id = id) }
    private fun recordsAccess() = fakeStore<RecordEntity> { e, id -> e.copy(id = id) }
    private fun appointmentsAccess() = fakeStore<AppointmentEntity> { e, id -> e.copy(id = id) }

    private fun sampleBackup() = BackupFile(
        exportedAtEpochMs = 1_700_000_000_000L,
        items = listOf(
            BackupItem(id = 5, type = "Veicoli", name = "Auto", notes = null, createdAtEpochMs = 1L)
        ),
        deadlines = listOf(
            BackupDeadline(
                id = 10, itemId = 5, category = "Bollo", dueDateEpochMs = 100L,
                reminderDaysCsv = "14,7,1", recurrence = "YEARLY", lastCostCents = 5000L,
                lastPaidEpochMs = null, notes = null, createdAtEpochMs = 2L
            )
        ),
        records = listOf(
            BackupRecord(
                id = 20, itemId = 5, deadlineId = 10, type = "Pagamento", title = "Bollo",
                dateEpochMs = 200L, amountCents = 5000L, notes = null,
                unisaluteSent = false, unisaluteStatus = null, unisaluteSentEpochMs = null,
                createdAtEpochMs = 3L
            )
        ),
        appointments = listOf(
            BackupAppointment(
                id = 30, itemId = 5, title = "Seduta", dateEpochMs = 300L, notes = null,
                amountCents = 8000L, isDone = true, isPaid = false, createdAtEpochMs = 4L
            )
        )
    )

    @Test
    fun `importing into an empty database inserts everything and remaps foreign keys to the new ids`() = runBlocking {
        val items = itemsAccess()
        val deadlines = deadlinesAccess()
        val records = recordsAccess()
        val appointments = appointmentsAccess()
        val content = json.encodeToString(BackupFile.serializer(), sampleBackup())

        val result = importBackupMerging(content, json, items, deadlines, records, appointments)

        assertEquals(BackupManager.ImportResult(items = 1, deadlines = 1, records = 1, appointments = 1), result)

        val insertedItem = items.getAllOnce().single()
        assertEquals(ItemType.Veicoli, insertedItem.type)
        assertEquals("Auto", insertedItem.name)

        // Le chiavi esterne devono puntare al NUOVO id dell'item, non al vecchio id (5) del backup.
        val insertedDeadline = deadlines.getAllOnce().single()
        assertEquals(insertedItem.id, insertedDeadline.itemId)

        val insertedRecord = records.getAllOnce().single()
        assertEquals(insertedItem.id, insertedRecord.itemId)
        assertEquals(insertedDeadline.id, insertedRecord.deadlineId)

        val insertedAppointment = appointments.getAllOnce().single()
        assertEquals(insertedItem.id, insertedAppointment.itemId)
    }

    @Test
    fun `importing the same backup twice is idempotent`() = runBlocking {
        val items = itemsAccess()
        val deadlines = deadlinesAccess()
        val records = recordsAccess()
        val appointments = appointmentsAccess()
        val content = json.encodeToString(BackupFile.serializer(), sampleBackup())

        importBackupMerging(content, json, items, deadlines, records, appointments)
        val second = importBackupMerging(content, json, items, deadlines, records, appointments)

        assertEquals(BackupManager.ImportResult(items = 0, deadlines = 0, records = 0, appointments = 0), second)
        assertEquals(1, items.getAllOnce().size)
        assertEquals(1, deadlines.getAllOnce().size)
        assertEquals(1, records.getAllOnce().size)
        assertEquals(1, appointments.getAllOnce().size)
    }

    @Test
    fun `duplicate items in the same file dedup by type and name, case- and whitespace-insensitive`() = runBlocking {
        val backup = sampleBackup().copy(
            items = listOf(
                BackupItem(id = 5, type = "Veicoli", name = "Auto", notes = null, createdAtEpochMs = 1L),
                BackupItem(id = 6, type = "Veicoli", name = "  AUTO  ", notes = "duplicato", createdAtEpochMs = 2L)
            ),
            // Un solo item finale, ma due scadenze che partono da backup-id diversi (5 e 6): devono
            // finire entrambe sullo stesso item reale.
            deadlines = listOf(
                BackupDeadline(id = 10, itemId = 5, category = "Bollo", dueDateEpochMs = 100L,
                    reminderDaysCsv = "14,7,1", recurrence = "NONE", lastCostCents = null,
                    lastPaidEpochMs = null, notes = null, createdAtEpochMs = 2L),
                BackupDeadline(id = 11, itemId = 6, category = "Assicurazione", dueDateEpochMs = 200L,
                    reminderDaysCsv = "14,7,1", recurrence = "NONE", lastCostCents = null,
                    lastPaidEpochMs = null, notes = null, createdAtEpochMs = 2L)
            ),
            records = emptyList(),
            appointments = emptyList()
        )
        val items = itemsAccess()
        val deadlines = deadlinesAccess()
        val content = json.encodeToString(BackupFile.serializer(), backup)

        val result = importBackupMerging(content, json, items, deadlines, recordsAccess(), appointmentsAccess())

        assertEquals(1, result.items) // il secondo item è un duplicato, non viene inserito
        assertEquals(2, result.deadlines)
        val onlyItem = items.getAllOnce().single()
        assertTrue(deadlines.getAllOnce().all { it.itemId == onlyItem.id })
    }

    @Test
    fun `rows referencing an item missing from the backup are skipped instead of crashing`() = runBlocking {
        val backup = sampleBackup().copy(
            items = emptyList() // simula un file corrotto/modificato a mano: l'item id=5 non c'è
        )
        val items = itemsAccess()
        val deadlines = deadlinesAccess()
        val records = recordsAccess()
        val appointments = appointmentsAccess()
        val content = json.encodeToString(BackupFile.serializer(), backup)

        val result = importBackupMerging(content, json, items, deadlines, records, appointments)

        assertEquals(BackupManager.ImportResult(items = 0, deadlines = 0, records = 0, appointments = 0), result)
        assertTrue(deadlines.getAllOnce().isEmpty())
        assertTrue(records.getAllOnce().isEmpty())
        assertTrue(appointments.getAllOnce().isEmpty())
    }

    @Test
    fun `a record whose deadline is missing is still inserted, with a null deadlineId`() = runBlocking {
        val backup = sampleBackup().copy(
            deadlines = emptyList(), // la scadenza id=10 referenziata dal record non esiste
            appointments = emptyList()
        )
        val items = itemsAccess()
        val deadlines = deadlinesAccess()
        val records = recordsAccess()
        val content = json.encodeToString(BackupFile.serializer(), backup)

        val result = importBackupMerging(content, json, items, deadlines, records, appointmentsAccess())

        assertEquals(1, result.records)
        assertNull(records.getAllOnce().single().deadlineId)
    }
}

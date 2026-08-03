package com.example.rimembranze.data.backup

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * JVM-only round-trip check for the backup JSON format: no Room/Android involved,
 * just verifying every field survives a serialize → deserialize cycle (including
 * the nullable ones, which are the easiest to silently drop by mistake).
 */
class BackupModelsTest {

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    @Test
    fun `backup file round-trips through JSON`() {
        val original = BackupFile(
            exportedAtEpochMs = 1_700_000_000_000L,
            items = listOf(
                BackupItem(id = 1, type = "Veicoli", name = "Auto", notes = "note item", createdAtEpochMs = 1L),
                BackupItem(id = 2, type = "Medico", name = "Dentista", notes = null, createdAtEpochMs = 2L)
            ),
            deadlines = listOf(
                BackupDeadline(
                    id = 10, itemId = 1, category = "Bollo", dueDateEpochMs = 100L,
                    reminderDaysCsv = "14,7,1", recurrence = "YEARLY", lastCostCents = 5000L,
                    lastPaidEpochMs = 50L, notes = "note deadline", createdAtEpochMs = 3L
                )
            ),
            records = listOf(
                BackupRecord(
                    id = 20, itemId = 1, deadlineId = 10, type = "Pagamento", title = "Bollo",
                    dateEpochMs = 200L, amountCents = 5000L, notes = null,
                    unisaluteSent = true, unisaluteStatus = "Approvato", unisaluteSentEpochMs = 150L,
                    createdAtEpochMs = 4L
                ),
                BackupRecord(
                    id = 21, itemId = 2, deadlineId = null, type = "Visita", title = "Controllo",
                    dateEpochMs = 210L, amountCents = null, notes = "senza scadenza collegata",
                    unisaluteSent = false, unisaluteStatus = null, unisaluteSentEpochMs = null,
                    createdAtEpochMs = 5L
                )
            ),
            appointments = listOf(
                BackupAppointment(
                    id = 30, itemId = 2, title = "Seduta", dateEpochMs = 300L, notes = null,
                    amountCents = 8000L, isDone = true, isPaid = false, createdAtEpochMs = 6L
                )
            )
        )

        val encoded = json.encodeToString(BackupFile.serializer(), original)
        val decoded = json.decodeFromString(BackupFile.serializer(), encoded)

        assertEquals(original, decoded)
        assertEquals(BACKUP_SCHEMA_VERSION, decoded.schemaVersion)
    }
}

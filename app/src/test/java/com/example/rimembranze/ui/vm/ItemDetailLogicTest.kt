package com.example.rimembranze.ui.vm

import com.example.rimembranze.data.db.AppointmentEntity
import com.example.rimembranze.data.db.RecordEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar

class ItemDetailLogicTest {

    private fun epochOf(year: Int, month: Int, day: Int): Long =
        Calendar.getInstance().apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }.timeInMillis

    // ── computeItemStats ───────────────────────────────────────────────────────

    @Test
    fun `total spent sums all record amounts, this-year filters by cutoff`() {
        val yearStart = epochOf(2026, Calendar.JANUARY, 1)
        val records = listOf(
            RecordEntity(itemId = 1, type = "Pagamento", title = "A",
                dateEpochMs = epochOf(2025, Calendar.DECEMBER, 20), amountCents = 1000L),
            RecordEntity(itemId = 1, type = "Pagamento", title = "B",
                dateEpochMs = epochOf(2026, Calendar.JANUARY, 5), amountCents = 2000L),
            RecordEntity(itemId = 1, type = "Pagamento", title = "C",
                dateEpochMs = epochOf(2026, Calendar.FEBRUARY, 1), amountCents = null)
        )

        val stats = computeItemStats(records, emptyList(), emptyList(), yearStart)

        assertEquals(3000L, stats.totalSpentCents)
        assertEquals(2000L, stats.totalSpentThisYearCents)
    }

    @Test
    fun `average appointment cost ignores zero-or-null amounts, null when none qualify`() {
        val paid = listOf(
            AppointmentEntity(itemId = 1, title = "Seduta 1", dateEpochMs = 0L, amountCents = 8000L, isDone = true, isPaid = true),
            AppointmentEntity(itemId = 1, title = "Seduta 2", dateEpochMs = 0L, amountCents = 6000L, isDone = true, isPaid = true)
        )
        val doneNotPaid = listOf(
            AppointmentEntity(itemId = 1, title = "Seduta 3", dateEpochMs = 0L, amountCents = null, isDone = true, isPaid = false)
        )

        val stats = computeItemStats(emptyList(), paid, doneNotPaid, yearStartEpochMs = 0L)

        assertEquals(3, stats.completedAppointments)
        assertEquals(7000L, stats.avgAppointmentCents) // (8000+6000)/2, the null-amount one excluded

        val statsNoAmounts = computeItemStats(emptyList(), emptyList(), doneNotPaid, yearStartEpochMs = 0L)
        assertNull(statsNoAmounts.avgAppointmentCents)
    }

    // ── nextRecurrenceDate ─────────────────────────────────────────────────────

    @Test
    fun `NONE recurrence has no next due date`() {
        assertNull(nextRecurrenceDate(epochOf(2026, Calendar.JANUARY, 1), "NONE"))
    }

    @Test
    fun `monthly recurrence advances by one month`() {
        val due = epochOf(2026, Calendar.JANUARY, 31)
        val next = nextRecurrenceDate(due, "MONTHLY")!!
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(Calendar.FEBRUARY, cal.get(Calendar.MONTH))
    }

    @Test
    fun `yearly recurrence advances by one year`() {
        val due = epochOf(2026, Calendar.MARCH, 15)
        val next = nextRecurrenceDate(due, "YEARLY")!!
        val cal = Calendar.getInstance().apply { timeInMillis = next }
        assertEquals(2027, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, cal.get(Calendar.MONTH))
    }

    // ── CSV escaping ───────────────────────────────────────────────────────────

    @Test
    fun `csv fields with commas, quotes or newlines get quoted and escaped`() {
        assertEquals("plain", csvEscape("plain"))
        assertEquals("\"a,b\"", csvEscape("a,b"))
        assertEquals("\"she said \"\"hi\"\"\"", csvEscape("she said \"hi\""))
        assertEquals("\"line1\nline2\"", csvEscape("line1\nline2"))
    }

    @Test
    fun `csv row joins escaped fields with commas`() {
        assertEquals("a,\"b,c\",d", csvRow(listOf("a", "b,c", "d")))
    }
}

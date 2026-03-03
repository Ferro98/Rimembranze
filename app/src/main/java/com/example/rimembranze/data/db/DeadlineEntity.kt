package com.example.rimembranze.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "deadlines",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("dueDateEpochMs")]
)
data class DeadlineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,

    // es: "bollo", "assicurazione", "abbonamento", "certificato", "altro"
    val category: String,

    // Data scadenza (epoch ms)
    val dueDateEpochMs: Long,

    // Es: "30,14,7,1" (giorni prima)
    val reminderDaysCsv: String = "30,14,7,1",

    // Ricorrenza semplice per ora: "NONE", "YEARLY", "MONTHLY", "CUSTOM"
    val recurrence: String = "NONE",

    val lastCostCents: Long? = null,
    val lastPaidEpochMs: Long? = null,

    val notes: String? = null,

    val createdAtEpochMs: Long = System.currentTimeMillis()
)
package com.example.rimembranze.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "appointments",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("itemId"), Index("dateEpochMs")]
)
data class AppointmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,

    val title: String,

    // Data + orario dell'appuntamento (epoch ms)
    val dateEpochMs: Long,

    val notes: String? = null,
    val amountCents: Long? = null,

    // true = appuntamento effettuato (passato nel calendario)
    val isDone: Boolean = false,

    // true = incluso in una fattura / segnato come pagato
    val isPaid: Boolean = false,

    val createdAtEpochMs: Long = System.currentTimeMillis()
)
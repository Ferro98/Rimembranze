package com.example.rimembranze.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "records",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = DeadlineEntity::class,
            parentColumns = ["id"],
            childColumns = ["deadlineId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("itemId"), Index("deadlineId"), Index("dateEpochMs")]
)
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemId: Long,
    val deadlineId: Long? = null,

    // "PAGAMENTO", "VISITA", "ALTRO" (stringa per restare flessibili)
    val type: String,
    val title: String,
    val dateEpochMs: Long,

    val amountCents: Long? = null,
    val notes: String? = null,

    // UniSalute (per ora solo campi, UI dopo)
    val unisaluteSent: Boolean = false,
    val unisaluteStatus: String? = null,
    val unisaluteSentEpochMs: Long? = null,

    val createdAtEpochMs: Long = System.currentTimeMillis()
)
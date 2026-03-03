package com.example.rimembranze.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: ItemType,
    val name: String,
    val notes: String? = null,
    val createdAtEpochMs: Long = System.currentTimeMillis()
)
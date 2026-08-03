package com.example.rimembranze.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {

    @Query("SELECT * FROM items ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ItemEntity>>

    // Snapshot one-shot per l'export/import del backup completo
    @Query("SELECT * FROM items")
    suspend fun getAllOnce(): List<ItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(item: ItemEntity): Long

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    fun observeById(itemId: Long): Flow<ItemEntity?>

    @Query("SELECT * FROM items WHERE id = :itemId LIMIT 1")
    suspend fun getById(itemId: Long): ItemEntity?

    @Delete
    suspend fun delete(record: ItemEntity)
}
package com.example.rimembranze.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Update
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DeadlineDao {

    @Query("SELECT * FROM deadlines WHERE itemId = :itemId ORDER BY dueDateEpochMs ASC")
    fun observeByItem(itemId: Long): Flow<List<DeadlineEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(deadline: DeadlineEntity): Long

    // Scadenze entro una certa data (per dashboard)
    @Query("SELECT * FROM deadlines WHERE dueDateEpochMs <= :untilEpochMs ORDER BY dueDateEpochMs ASC")
    fun observeDueUntil(untilEpochMs: Long): Flow<List<DeadlineEntity>>

    @Query("""
    SELECT * FROM deadlines 
    WHERE dueDateEpochMs BETWEEN :now AND :until 
    ORDER BY dueDateEpochMs ASC
""")
    fun observeUpcoming(now: Long, until: Long): Flow<List<DeadlineEntity>>

    @Query("""
        SELECT * FROM deadlines
        WHERE dueDateEpochMs < :now
        ORDER BY dueDateEpochMs ASC
    """)
    fun observeExpired(now: Long): Flow<List<DeadlineEntity>>

    @androidx.room.Delete
    suspend fun delete(deadline: DeadlineEntity)

    @Update
    suspend fun update(deadline: DeadlineEntity)

    @Query("SELECT * FROM deadlines WHERE id = :deadlineId LIMIT 1")
    suspend fun getById(deadlineId: Long): DeadlineEntity?
}
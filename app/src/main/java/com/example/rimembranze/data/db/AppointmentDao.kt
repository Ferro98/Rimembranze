package com.example.rimembranze.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppointmentDao {

    // Tutti gli appuntamenti di un item, dal più recente
    @Query("SELECT * FROM appointments WHERE itemId = :itemId ORDER BY dateEpochMs ASC")
    fun observeByItem(itemId: Long): Flow<List<AppointmentEntity>>

    // Appuntamenti futuri (non ancora effettuati)
    @Query("""
        SELECT * FROM appointments
        WHERE itemId = :itemId AND isDone = 0
        ORDER BY dateEpochMs ASC
    """)
    fun observePending(itemId: Long): Flow<List<AppointmentEntity>>

    // Effettuati ma non ancora pagati → sezione "Da fatturare"
    @Query("""
        SELECT * FROM appointments
        WHERE itemId = :itemId AND isDone = 1 AND isPaid = 0
        ORDER BY dateEpochMs ASC
    """)
    fun observeDoneNotPaid(itemId: Long): Flow<List<AppointmentEntity>>

    // Storico: effettuati e pagati
    @Query("""
        SELECT * FROM appointments
        WHERE itemId = :itemId AND isDone = 1 AND isPaid = 1
        ORDER BY dateEpochMs DESC
    """)
    fun observePaid(itemId: Long): Flow<List<AppointmentEntity>>

    @Query("SELECT * FROM appointments WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): AppointmentEntity?

    @Insert
    suspend fun insert(appointment: AppointmentEntity): Long

    @Update
    suspend fun update(appointment: AppointmentEntity)

    @Delete
    suspend fun delete(appointment: AppointmentEntity)

    // Segna una lista di appuntamenti come pagati in una sola transazione
    @Query("UPDATE appointments SET isPaid = 1 WHERE id IN (:ids)")
    suspend fun markAsPaid(ids: List<Long>)
}
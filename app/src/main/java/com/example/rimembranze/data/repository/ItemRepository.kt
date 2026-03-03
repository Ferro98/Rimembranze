package com.example.rimembranze.data.repository

import com.example.rimembranze.data.db.ItemDao
import com.example.rimembranze.data.db.ItemEntity
import kotlinx.coroutines.flow.Flow

class ItemRepository(
    private val itemDao: ItemDao
) {
    fun observeItems(): Flow<List<ItemEntity>> = itemDao.observeAll()

    suspend fun addItem(item: ItemEntity): Long = itemDao.insert(item)
}
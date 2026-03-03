package com.example.rimembranze.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rimembranze.data.db.AppDatabase
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.data.repository.ItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemsUiState(
    val items: List<ItemEntity> = emptyList()
)

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = ItemRepository(db.itemDao())

    val uiState: StateFlow<ItemsUiState> = repo.observeItems()
        .map { ItemsUiState(items = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ItemsUiState()
        )

    fun addItem(name: String, type: ItemType, notes: String? = null) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            repo.addItem(
                ItemEntity(
                    type = type,
                    name = trimmed,
                    notes = notes?.trim()?.takeIf { it.isNotEmpty() }
                )
            )
        }
    }
}
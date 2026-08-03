package com.example.rimembranze.ui.vm

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.rimembranze.R
import com.example.rimembranze.data.backup.BackupManager
import com.example.rimembranze.data.db.AppDatabase
import com.example.rimembranze.data.db.ItemEntity
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.data.repository.ItemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ItemsUiState(
    val items: List<ItemEntity> = emptyList()
)

class ItemsViewModel(app: Application) : AndroidViewModel(app) {

    private val db = AppDatabase.get(app)
    private val repo = ItemRepository(db.itemDao())
    private val backupManager = BackupManager(db)

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

    // ── Backup completo (JSON) ─────────────────────────────────────────────────

    private val _backupExportResult = MutableStateFlow<Boolean?>(null)
    val backupExportResult: StateFlow<Boolean?> = _backupExportResult.asStateFlow()

    private val _backupImportResult = MutableStateFlow<BackupManager.ImportResult?>(null)
    val backupImportResult: StateFlow<BackupManager.ImportResult?> = _backupImportResult.asStateFlow()

    private val _backupImportError = MutableStateFlow<String?>(null)
    val backupImportError: StateFlow<String?> = _backupImportError.asStateFlow()

    fun clearBackupFeedback() {
        _backupExportResult.value = null
        _backupImportResult.value = null
        _backupImportError.value = null
    }

    fun exportBackupJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            _backupExportResult.value = try {
                val content = backupManager.exportAll()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    fun importBackupJson(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val content = context.contentResolver.openInputStream(uri)
                    ?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: throw IllegalStateException(context.getString(R.string.main_backup_file_unreadable))
                _backupImportResult.value = backupManager.importMerging(content)
            } catch (e: Exception) {
                _backupImportError.value = e.message ?: context.getString(R.string.main_backup_import_generic_error)
            }
        }
    }
}
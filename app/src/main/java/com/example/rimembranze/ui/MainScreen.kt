package com.example.rimembranze.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rimembranze.R
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.ui.components.AccentAmber
import com.example.rimembranze.ui.components.BackgroundDark
import com.example.rimembranze.ui.components.DestructiveRed
import com.example.rimembranze.ui.components.DestructiveRed as AccentRed
import com.example.rimembranze.ui.components.DividerColor
import com.example.rimembranze.ui.components.SurfaceDark
import com.example.rimembranze.ui.components.SurfaceElevated
import com.example.rimembranze.ui.components.TextPrimary
import com.example.rimembranze.ui.components.TextSecondary
import com.example.rimembranze.ui.vm.DashboardViewModel
import com.example.rimembranze.ui.vm.ItemsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Palette ───────────────────────────────────────────────────────────────────
// Colori condivisi importati da ui.components.SharedComponents (unica sorgente di verità).
// DrawerBackground resta locale: è usato solo qui, non è duplicato altrove.
private val DrawerBackground = Color(0xFF13131A)

private fun typeColor(type: ItemType): Color = when (type) {
    ItemType.Veicoli  -> Color(0xFF5B8DEF)
    ItemType.Palestra -> Color(0xFFEF8C5B)
    ItemType.Medico   -> Color(0xFF5BEF9A)
    ItemType.Altro    -> Color(0xFFBF5BEF)
}

private fun typeIcon(type: ItemType?): ImageVector = when (type) {
    ItemType.Veicoli  -> Icons.Default.DirectionsCar
    ItemType.Palestra -> Icons.Default.FitnessCenter
    ItemType.Medico   -> Icons.Default.LocalHospital
    ItemType.Altro    -> Icons.Default.Category
    null              -> Icons.Default.GridView
}

@Composable
private fun typeLabel(type: ItemType?): String = when (type) {
    ItemType.Veicoli  -> stringResource(R.string.item_type_veicoli)
    ItemType.Palestra -> stringResource(R.string.item_type_palestra)
    ItemType.Medico   -> stringResource(R.string.item_type_medico)
    ItemType.Altro    -> stringResource(R.string.item_type_altro)
    null              -> stringResource(R.string.item_type_all)
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialItemId: Long? = null,
    initialDeadlineId: Long? = null,
    initialAppointmentId: Long? = null
) {
    val vm: ItemsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val dashboardVm: DashboardViewModel = viewModel()
    val upcoming by dashboardVm.upcoming.collectAsState()
    val expired  by dashboardVm.expired.collectAsState()

    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // ── Backup completo (JSON): export/import ────────────────────────────────
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportBackupJson(context, uri) }

    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImportUri = uri }

    val backupExportResult by vm.backupExportResult.collectAsState()
    val backupImportResult by vm.backupImportResult.collectAsState()
    val backupImportError   by vm.backupImportError.collectAsState()
    var backupMessage by remember { mutableStateOf<String?>(null) }

    val backupExportSuccessMsg = stringResource(R.string.main_backup_export_success)
    val backupExportFailureMsg = stringResource(R.string.main_backup_export_failure)
    val backupImportEmptyMsg   = stringResource(R.string.main_backup_import_empty)

    LaunchedEffect(backupExportResult) {
        backupExportResult?.let {
            backupMessage = if (it) backupExportSuccessMsg else backupExportFailureMsg
            vm.clearBackupFeedback()
        }
    }
    LaunchedEffect(backupImportResult) {
        backupImportResult?.let { r ->
            val total = r.items + r.deadlines + r.records + r.appointments
            backupMessage = if (total == 0) {
                backupImportEmptyMsg
            } else {
                context.getString(
                    R.string.main_backup_import_summary,
                    r.items, r.deadlines, r.records, r.appointments
                )
            }
            vm.clearBackupFeedback()
        }
    }
    LaunchedEffect(backupImportError) {
        backupImportError?.let {
            backupMessage = context.getString(R.string.main_backup_import_failure, it)
            vm.clearBackupFeedback()
        }
    }

    var filterType         by remember { mutableStateOf<ItemType?>(null) }
    var newName            by remember { mutableStateOf("") }
    var selectedType       by remember { mutableStateOf(ItemType.Altro) }
    var typeMenuExpanded   by remember { mutableStateOf(false) }
    var selectedItemId        by remember { mutableStateOf<Long?>(initialItemId) }
    var scrollToDeadlineId    by remember { mutableStateOf<Long?>(initialDeadlineId) }
    var scrollToAppointmentId by remember { mutableStateOf<Long?>(initialAppointmentId) }
    var showAddSheet       by remember { mutableStateOf(false) }
    var searchQuery        by remember { mutableStateOf("") }
    var searchActive       by remember { mutableStateOf(false) }

    // ── Transizione slide MainScreen ↔ ItemDetailScreen ──────────────────────
    AnimatedContent(
        targetState = selectedItemId,
        transitionSpec = {
            if (targetState != null) {
                // Entrata dettaglio: slide da destra
                (slideInHorizontally { it } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut(tween(200)))
            } else {
                // Ritorno lista: slide da sinistra
                (slideInHorizontally { -it / 3 } + fadeIn(tween(200))) togetherWith
                        (slideOutHorizontally { it } + fadeOut(tween(200)))
            }
        },
        label = "main_detail_transition"
    ) { currentItemId ->
        if (currentItemId != null) {
            ItemDetailScreen(
                itemId                = currentItemId,
                scrollToDeadlineId    = scrollToDeadlineId,
                scrollToAppointmentId = scrollToAppointmentId,
                onBack = {
                    selectedItemId        = null
                    scrollToDeadlineId    = null
                    scrollToAppointmentId = null
                }
            )
        } else {
            MainList(
                state            = state,
                upcoming         = upcoming,
                expired          = expired,
                filterType       = filterType,
                searchQuery      = searchQuery,
                searchActive     = searchActive,
                drawerState      = drawerState,
                scope            = scope,
                showAddSheet     = showAddSheet,
                newName          = newName,
                selectedType     = selectedType,
                typeMenuExpanded = typeMenuExpanded,
                onFilterType     = { filterType = it },
                onSearchQuery    = { searchQuery = it },
                onSearchActive   = { searchActive = it },
                onSelectItem     = { id -> selectedItemId = id },
                onSelectDeadline = { itemId, deadlineId ->
                    scrollToDeadlineId = deadlineId
                    selectedItemId = itemId
                },
                onShowAdd        = { showAddSheet = true },
                onDismissAdd     = { showAddSheet = false },
                onNewName        = { newName = it },
                onSelectedType   = { selectedType = it },
                onTypeMenu       = { typeMenuExpanded = it },
                onAddItem        = {
                    if (newName.isNotBlank()) {
                        vm.addItem(newName, selectedType)
                        newName = ""
                        showAddSheet = false
                    }
                },
                onExportBackup   = {
                    val fname = "rimembranze_backup_${
                        SimpleDateFormat("yyyyMMdd", Locale.ITALY).format(Date())
                    }.json"
                    exportBackupLauncher.launch(fname)
                },
                onImportBackup   = { importBackupLauncher.launch(arrayOf("application/json")) }
            )
        }
    }

    // ── Conferma import backup ────────────────────────────────────────────────
    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            shape = RoundedCornerShape(24.dp), containerColor = SurfaceDark, tonalElevation = 0.dp,
            icon = { Icon(Icons.Default.Restore, null, tint = AccentAmber, modifier = Modifier.size(28.dp)) },
            title = { Text(stringResource(R.string.main_import_backup_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Text(
                    stringResource(R.string.main_import_backup_body),
                    color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.importBackupJson(context, uri); pendingImportUri = null },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color(0xFF1A1100))
                ) { Text(stringResource(R.string.main_import_backup_confirm), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    // ── Esito export/import backup ───────────────────────────────────────────
    backupMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { backupMessage = null },
            shape = RoundedCornerShape(24.dp), containerColor = SurfaceDark, tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.main_backup_result_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = { Text(msg, color = TextSecondary, fontSize = 14.sp, lineHeight = 20.sp) },
            confirmButton = {
                Button(
                    onClick = { backupMessage = null }, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber, contentColor = Color(0xFF1A1100))
                ) { Text(stringResource(R.string.action_ok), fontWeight = FontWeight.Bold) }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// MainList — estratto per poter essere wrappato nell'AnimatedContent
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainList(
    state: com.example.rimembranze.ui.vm.ItemsUiState,
    upcoming: List<com.example.rimembranze.data.db.DeadlineEntity>,
    expired: List<com.example.rimembranze.data.db.DeadlineEntity>,
    filterType: ItemType?,
    searchQuery: String,
    searchActive: Boolean,
    drawerState: DrawerState,
    scope: kotlinx.coroutines.CoroutineScope,
    showAddSheet: Boolean,
    newName: String,
    selectedType: ItemType,
    typeMenuExpanded: Boolean,
    onFilterType: (ItemType?) -> Unit,
    onSearchQuery: (String) -> Unit,
    onSearchActive: (Boolean) -> Unit,
    onSelectItem: (Long) -> Unit,
    onSelectDeadline: (Long, Long) -> Unit,
    onShowAdd: () -> Unit,
    onDismissAdd: () -> Unit,
    onNewName: (String) -> Unit,
    onSelectedType: (ItemType) -> Unit,
    onTypeMenu: (Boolean) -> Unit,
    onAddItem: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit
) {
    val urgentByItem: Map<Long, Int> = remember(upcoming, expired) {
        val map = mutableMapOf<Long, Int>()
        (upcoming + expired).forEach { d -> map[d.itemId] = (map[d.itemId] ?: 0) + 1 }
        map
    }

    val availableTypes: List<ItemType?> = listOf(null) +
            ItemType.entries.filter { t -> state.items.any { it.type == t } }

    val filteredItems = remember(state.items, filterType, searchQuery, state.deadlines, state.appointments) {
        state.items
            .let { if (filterType == null) it else it.filter { i -> i.type == filterType } }
            .let { list ->
                if (searchQuery.isBlank()) list
                else {
                    // Un item combacia anche se una sua scadenza (categoria) o un suo
                    // appuntamento (titolo) contiene la query, non solo nome/note dell'item
                    val matchingItemIds = buildSet {
                        state.deadlines.filterTo(mutableListOf()) {
                            it.category.contains(searchQuery, ignoreCase = true)
                        }.forEach { add(it.itemId) }
                        state.appointments.filterTo(mutableListOf()) {
                            it.title.contains(searchQuery, ignoreCase = true)
                        }.forEach { add(it.itemId) }
                    }
                    list.filter { i ->
                        i.name.contains(searchQuery, ignoreCase = true) ||
                                i.notes?.contains(searchQuery, ignoreCase = true) == true ||
                                i.id in matchingItemIds
                    }
                }
            }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                availableTypes = availableTypes,
                selectedType   = filterType,
                itemCounts     = state.items.groupBy { it.type }.mapValues { it.value.size },
                totalCount     = state.items.size,
                onSelect       = { type -> onFilterType(type); scope.launch { drawerState.close() } },
                onExportBackup = { scope.launch { drawerState.close() }; onExportBackup() },
                onImportBackup = { scope.launch { drawerState.close() }; onImportBackup() }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)
            .statusBarsPadding().navigationBarsPadding()) {
            Column(modifier = Modifier.fillMaxSize()) {
                BatteryOptimizationBanner()
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)) {

                    // ── App Header ───────────────────────────────────────────
                    item {
                        Box(modifier = Modifier.fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark)))
                            .padding(horizontal = 20.dp, vertical = 24.dp)) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                            .background(SurfaceElevated)) {
                                        Icon(Icons.Default.Menu, stringResource(R.string.main_open_menu),
                                            tint = TextPrimary, modifier = Modifier.size(20.dp))
                                    }
                                    Spacer(Modifier.width(14.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        AnimatedContent(targetState = typeLabel(filterType),
                                            transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith
                                                    slideOutVertically { it } + fadeOut() },
                                            label = "header_title") { label ->
                                            Text(label, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    // Icona ricerca
                                    IconButton(onClick = { onSearchActive(!searchActive) },
                                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                                            .background(if (searchActive) AccentAmber.copy(alpha = 0.15f) else SurfaceElevated)) {
                                        Icon(if (searchActive) Icons.Default.SearchOff else Icons.Default.Search,
                                            stringResource(if (searchActive) R.string.main_search_off else R.string.main_search),
                                            tint = if (searchActive) AccentAmber else TextPrimary,
                                            modifier = Modifier.size(18.dp))
                                    }
                                    if (filterType != null) {
                                        Spacer(Modifier.width(8.dp))
                                        Box(modifier = Modifier.clip(RoundedCornerShape(20.dp))
                                            .background(typeColor(filterType).copy(alpha = 0.15f))
                                            .clickable { onFilterType(null) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Close, stringResource(R.string.main_remove_filter),
                                                    tint = typeColor(filterType), modifier = Modifier.size(12.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text(typeLabel(filterType), color = typeColor(filterType),
                                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                // ── Barra di ricerca animata ─────────────────
                                AnimatedVisibility(
                                    visible = searchActive,
                                    enter = expandVertically() + fadeIn(),
                                    exit  = shrinkVertically() + fadeOut()
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = onSearchQuery,
                                        placeholder = { Text(stringResource(R.string.main_search_placeholder), color = TextSecondary) },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Search, null, tint = TextSecondary) },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { onSearchQuery("") }) {
                                                    Icon(Icons.Default.Close, null, tint = TextSecondary)
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor   = AccentAmber,
                                            unfocusedBorderColor = DividerColor,
                                            focusedTextColor     = TextPrimary,
                                            unfocusedTextColor   = TextPrimary,
                                            cursorColor          = AccentAmber
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // ── Upcoming (solo senza ricerca attiva) ─────────────────
                    if (!searchActive && filterType == null) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, null, tint = AccentRed, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(stringResource(R.string.main_upcoming_header), color = TextSecondary, fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                if (upcoming.isNotEmpty()) {
                                    Box(modifier = Modifier.clip(CircleShape)
                                        .background(AccentRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("${upcoming.size}", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                        }
                        if (upcoming.isEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF3C3C4A)))
                                    Spacer(Modifier.width(12.dp))
                                    Text(stringResource(R.string.main_no_upcoming), color = TextSecondary, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(upcoming) { d ->
                                UpcomingDeadlineRow(category = d.category, dateEpochMs = d.dueDateEpochMs,
                                    onClick = { onSelectDeadline(d.itemId, d.id) })
                            }
                        }
                        if (expired.isNotEmpty()) {
                            item {
                                Row(modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Warning, null, tint = AccentRed, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.main_expired_header), color = TextSecondary, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.clip(CircleShape)
                                        .background(AccentRed.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)) {
                                        Text("${expired.size}", color = AccentRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                            }
                            items(expired) { d ->
                                UpcomingDeadlineRow(category = d.category, dateEpochMs = d.dueDateEpochMs,
                                    isExpired = true, onClick = { onSelectDeadline(d.itemId, d.id) })
                            }
                        }
                    }

                    // ── Items header ─────────────────────────────────────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.main_items_header), color = TextSecondary, fontSize = 11.sp,
                                fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                            AnimatedContent(targetState = filteredItems.size,
                                transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "count_anim") { count ->
                                Text("$count",
                                    color = if (filterType != null) typeColor(filterType) else AccentAmber,
                                    fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                    }

                    // ── Item Cards ───────────────────────────────────────────
                    if (filteredItems.isEmpty()) {
                        item { MainEmptyState(filterType != null || searchQuery.isNotBlank()) }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            ItemRow(
                                name        = item.name,
                                type        = item.type,
                                notes       = item.notes,
                                urgentCount = urgentByItem[item.id] ?: 0,
                                searchQuery = searchQuery,
                                onClick     = { onSelectItem(item.id) },
                                modifier    = Modifier.animateItem(
                                    fadeInSpec    = tween(200),
                                    fadeOutSpec   = tween(200),
                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            )
                        }
                    }
                }
            }

            FloatingActionButton(onClick = onShowAdd,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                containerColor = AccentAmber, contentColor = Color(0xFF1A1100),
                shape = CircleShape, elevation = FloatingActionButtonDefaults.elevation(8.dp)) {
                Icon(Icons.Default.Add, stringResource(R.string.main_add_item_desc))
            }
        }
    }

    if (showAddSheet) {
        AlertDialog(
            onDismissRequest = onDismissAdd,
            shape = RoundedCornerShape(24.dp), containerColor = SurfaceDark, tonalElevation = 0.dp,
            title = { Text(stringResource(R.string.main_new_item_title), color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = newName, onValueChange = onNewName,
                        label = { Text(stringResource(R.string.main_field_name), color = TextSecondary) }, singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber,
                            unfocusedBorderColor = DividerColor, focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary, cursorColor = AccentAmber, focusedLabelColor = AccentAmber),
                        modifier = Modifier.fillMaxWidth())
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(expanded = typeMenuExpanded,
                        onExpandedChange = { onTypeMenu(!typeMenuExpanded) }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(value = selectedType.name, onValueChange = {}, readOnly = true,
                            label = { Text(stringResource(R.string.main_field_type), color = TextSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = DividerColor, focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary, focusedLabelColor = AccentAmber),
                            modifier = Modifier.menuAnchor().fillMaxWidth())
                        ExposedDropdownMenu(expanded = typeMenuExpanded,
                            onDismissRequest = { onTypeMenu(false) },
                            modifier = Modifier.background(SurfaceElevated)) {
                            ItemType.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(typeIcon(t), null, tint = typeColor(t), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(typeLabel(t), color = if (selectedType == t) AccentAmber else TextPrimary)
                                        }
                                    },
                                    onClick = { onSelectedType(t); onTypeMenu(false) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onAddItem, enabled = newName.isNotBlank(), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentAmber,
                        contentColor = Color(0xFF1A1100), disabledContainerColor = AccentAmber.copy(alpha = 0.3f))
                ) { Text(stringResource(R.string.main_add), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = onDismissAdd, shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

// ── MainEmptyState ────────────────────────────────────────────────────────────
@Composable
private fun MainEmptyState(isFiltered: Boolean) {
    val scale by animateFloatAsState(targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "main_empty_scale")
    val breathAlpha by animateFloatAsState(targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "main_empty_breath")
    Box(modifier = Modifier.fillMaxWidth().padding(40.dp).scale(scale), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("✦", color = AccentAmber.copy(alpha = breathAlpha), fontSize = 40.sp)
            Text(stringResource(if (isFiltered) R.string.main_no_items_found else R.string.main_no_items_empty),
                color = TextSecondary, fontSize = 14.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ── NavigationDrawerContent ───────────────────────────────────────────────────
@Composable
private fun NavigationDrawerContent(
    availableTypes: List<ItemType?>, selectedType: ItemType?,
    itemCounts: Map<ItemType, Int>, totalCount: Int, onSelect: (ItemType?) -> Unit,
    onExportBackup: () -> Unit, onImportBackup: () -> Unit
) {
    ModalDrawerSheet(drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = DrawerBackground, drawerTonalElevation = 0.dp,
        modifier = Modifier.width(280.dp).fillMaxHeight()) {
        Box(modifier = Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(SurfaceDark, DrawerBackground)))
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 28.dp)) {
            Column {
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
                    .background(AccentAmber.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Notifications, null, tint = AccentAmber, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.app_name), color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.main_items_total, totalCount), color = TextSecondary, fontSize = 13.sp)
            }
        }
        HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.main_categories_header), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
            DrawerItem(icon = typeIcon(null), label = stringResource(R.string.item_type_all), count = totalCount,
                color = AccentAmber, isSelected = selectedType == null, onClick = { onSelect(null) })
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 0.5.dp, color = DividerColor)
            availableTypes.filterNotNull().forEach { type ->
                DrawerItem(icon = typeIcon(type), label = typeLabel(type), count = itemCounts[type] ?: 0,
                    color = typeColor(type), isSelected = selectedType == type, onClick = { onSelect(type) })
            }
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                thickness = 0.5.dp, color = DividerColor)
            Text(stringResource(R.string.main_data_header), color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp, modifier = Modifier.padding(start = 24.dp, bottom = 8.dp))
            DrawerItem(icon = Icons.Default.Backup, label = stringResource(R.string.main_export_backup), count = 0,
                color = AccentAmber, isSelected = false, onClick = onExportBackup)
            DrawerItem(icon = Icons.Default.Restore, label = stringResource(R.string.main_import_backup), count = 0,
                color = AccentAmber, isSelected = false, onClick = onImportBackup)
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, count: Int,
                       color: Color, isSelected: Boolean, onClick: () -> Unit) {
    val bgAlpha by animateFloatAsState(targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200), label = "drawer_bg")
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 3.dp)
        .clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = 0.10f * bgAlpha))
        .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = if (isSelected) 0.20f else 0.10f)),
            contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isSelected) color else color.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = if (isSelected) TextPrimary else TextSecondary, fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f))
        if (count > 0) {
            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) color.copy(alpha = 0.20f) else Color(0xFF2C2C3A))
                .padding(horizontal = 8.dp, vertical = 3.dp)) {
                Text("$count", color = if (isSelected) color else TextSecondary,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ── ItemRow con badge urgenti e highlight ricerca ─────────────────────────────
@Composable
private fun ItemRow(
    name: String, type: ItemType, notes: String?,
    urgentCount: Int, searchQuery: String,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val color = typeColor(type)
    Card(onClick = onClick, modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp)) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(3.dp).height(40.dp).clip(RoundedCornerShape(2.dp)).background(color))
            Spacer(Modifier.width(14.dp))
            Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(typeIcon(type), null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(color.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(typeLabel(type), color = color, fontSize = 11.sp,
                            fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                    }
                    if (!notes.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(notes, color = TextSecondary, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
            if (urgentCount > 0) {
                Spacer(Modifier.width(8.dp))
                val pulseAlpha by animateFloatAsState(targetValue = 0.5f,
                    animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
                    label = "badge_pulse")
                Box(modifier = Modifier.size(22.dp).clip(CircleShape)
                    .background(AccentRed.copy(alpha = 0.15f + pulseAlpha * 0.1f)),
                    contentAlignment = Alignment.Center) {
                    Text(if (urgentCount > 9) "9+" else "$urgentCount",
                        color = AccentRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
                tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

// ── UpcomingDeadlineRow ───────────────────────────────────────────────────────
@Composable
private fun UpcomingDeadlineRow(category: String, dateEpochMs: Long,
                                onClick: () -> Unit, isExpired: Boolean = false) {
    val daysLate  = if (isExpired) ((System.currentTimeMillis() - dateEpochMs) / (1000L * 60 * 60 * 24)).toInt() else 0
    val rowColor  = if (isExpired) DestructiveRed else AccentAmber
    val bgColor   = if (isExpired) DestructiveRed.copy(alpha = 0.06f) else Color.Transparent
    val dateLabel = if (isExpired) when (daysLate) {
        0 -> stringResource(R.string.main_expired_today)
        1 -> stringResource(R.string.main_expired_yesterday)
        else -> stringResource(R.string.main_expired_days_ago, daysLate)
    } else formatDate(dateEpochMs)

    Row(modifier = Modifier.fillMaxWidth().background(bgColor).clickable(onClick = onClick)
        .padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        val dotAlpha by animateFloatAsState(targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "dot_pulse")
        Box(modifier = Modifier.size(8.dp).clip(CircleShape)
            .background(if (isExpired) rowColor.copy(alpha = dotAlpha) else rowColor))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(category, color = TextPrimary, fontSize = 14.sp,
                fontWeight = if (isExpired) FontWeight.SemiBold else FontWeight.Normal)
            if (isExpired) { Spacer(Modifier.height(1.dp))
                Text(formatDate(dateEpochMs), color = TextSecondary, fontSize = 11.sp) }
        }
        Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
            .background(rowColor.copy(alpha = if (isExpired) 0.18f else 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(dateLabel, color = rowColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null,
            tint = rowColor.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
    HorizontalDivider(modifier = Modifier.padding(start = 40.dp), thickness = 0.5.dp, color = DividerColor)
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))
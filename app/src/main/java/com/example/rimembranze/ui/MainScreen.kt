package com.example.rimembranze.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rimembranze.data.db.ItemType
import com.example.rimembranze.ui.vm.DashboardViewModel
import com.example.rimembranze.ui.vm.ItemsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Palette ──────────────────────────────────────────────────────────────────
private val BackgroundDark   = Color(0xFF0F0F13)
private val SurfaceDark      = Color(0xFF1A1A22)
private val SurfaceElevated  = Color(0xFF23232E)
private val DrawerBackground = Color(0xFF13131A)
private val AccentAmber      = Color(0xFFE8A020)
private val AccentRed        = Color(0xFFE05858)
private val TextPrimary      = Color(0xFFF0EEE8)
private val TextSecondary    = Color(0xFF8A8898)
private val DividerColor     = Color(0xFF2C2C3A)

// ── Tipo → colore ────────────────────────────────────────────────────────────
private fun typeColor(type: ItemType): Color = when (type) {
    ItemType.Veicoli  -> Color(0xFF5B8DEF)
    ItemType.Palestra -> Color(0xFFEF8C5B)
    ItemType.Medico   -> Color(0xFF5BEF9A)
    ItemType.Altro    -> Color(0xFFBF5BEF)
}

// ── Tipo → icona ─────────────────────────────────────────────────────────────
private fun typeIcon(type: ItemType?): ImageVector = when (type) {
    ItemType.Veicoli  -> Icons.Default.DirectionsCar
    ItemType.Palestra -> Icons.Default.FitnessCenter
    ItemType.Medico   -> Icons.Default.LocalHospital
    ItemType.Altro    -> Icons.Default.Category
    null              -> Icons.Default.GridView
}

// ── Tipo → label drawer ───────────────────────────────────────────────────────
private fun typeLabel(type: ItemType?): String = when (type) {
    ItemType.Veicoli  -> "Veicoli"
    ItemType.Palestra -> "Palestra"
    ItemType.Medico   -> "Medico"
    ItemType.Altro    -> "Altro"
    null              -> "Tutti"
}

// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    initialItemId: Long? = null,
    initialDeadlineId: Long? = null
) {
    val vm: ItemsViewModel = viewModel()
    val state by vm.uiState.collectAsState()
    val dashboardVm: DashboardViewModel = viewModel()
    val upcoming by dashboardVm.upcoming.collectAsState()
    val expired by dashboardVm.expired.collectAsState()

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var filterType by remember { mutableStateOf<ItemType?>(null) }
    var newName by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ItemType.Altro) }
    var typeMenuExpanded by remember { mutableStateOf(false) }
    var selectedItemId by remember { mutableStateOf<Long?>(initialItemId) }
    var scrollToDeadlineId by remember { mutableStateOf<Long?>(initialDeadlineId) }

    var showAddSheet by remember { mutableStateOf(false) }

    if (selectedItemId != null) {
        ItemDetailScreen(
            itemId = selectedItemId!!,
            scrollToDeadlineId = scrollToDeadlineId,
            onBack = { selectedItemId = null }
        )
        return
    }

    val availableTypes: List<ItemType?> = listOf(null) +
            ItemType.entries.filter { t -> state.items.any { it.type == t } }

    val filteredItems = if (filterType == null) state.items
    else state.items.filter { it.type == filterType }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NavigationDrawerContent(
                availableTypes = availableTypes,
                selectedType = filterType,
                itemCounts = state.items.groupBy { it.type }.mapValues { it.value.size },
                totalCount = state.items.size,
                onSelect = { type ->
                    filterType = type
                    scope.launch { drawerState.close() }
                }
            )
        },
        scrimColor = Color.Black.copy(alpha = 0.55f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BackgroundDark)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {

                // ── Banner batteria FUORI da LazyColumn ──────────────────────
                BatteryOptimizationBanner()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {

                    // ── App Header ───────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark))
                                )
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SurfaceElevated)
                                ) {
                                    Icon(
                                        Icons.Default.Menu,
                                        contentDescription = "Apri menu",
                                        tint = TextPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    AnimatedContent(
                                        targetState = typeLabel(filterType),
                                        transitionSpec = {
                                            slideInVertically { -it } + fadeIn() togetherWith
                                                    slideOutVertically { it } + fadeOut()
                                        },
                                        label = "header_title"
                                    ) { label ->
                                        Text(
                                            text = label,
                                            color = TextPrimary,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (filterType != null) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(typeColor(filterType!!).copy(alpha = 0.15f))
                                            .clickable { filterType = null }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = "Rimuovi filtro",
                                                tint = typeColor(filterType!!),
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                text = typeLabel(filterType),
                                                color = typeColor(filterType!!),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Upcoming Section (solo se filtro = tutti) ────────────
                    if (filterType == null) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AccentRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "PROSSIME SCADENZE",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 2.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                if (upcoming.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(AccentRed.copy(alpha = 0.15f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "${upcoming.size}",
                                            color = AccentRed,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                        }

                        if (upcoming.isEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF3C3C4A))
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        "Nessuna scadenza nei prossimi 30 giorni",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            items(upcoming) { d ->
                                UpcomingDeadlineRow(
                                    category = d.category,
                                    dateEpochMs = d.dueDateEpochMs,
                                    onClick = {
                                        scrollToDeadlineId = d.id
                                        selectedItemId = d.itemId
                                    }
                                )
                            }
                        }

                        // Sezione scaduti
                        if (expired.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = AccentRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        text = "SCADUTI",
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 2.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (expired.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .background(AccentRed.copy(alpha = 0.15f))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "${expired.size}",
                                                color = AccentRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                            }

                            items(expired) { d ->
                                UpcomingDeadlineRow(
                                    category = d.category,
                                    dateEpochMs = d.dueDateEpochMs,
                                    isExpired = true,
                                    onClick = {
                                        scrollToDeadlineId = d.id
                                        selectedItemId = d.itemId
                                    }
                                )
                            }
                        }
                    }

                    // ── Items Section Header ─────────────────────────────────
                    item {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ELEMENTI",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.weight(1f)
                            )
                            AnimatedContent(
                                targetState = filteredItems.size,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "count_anim"
                            ) { count ->
                                Text(
                                    text = "$count",
                                    color = if (filterType != null) typeColor(filterType!!) else AccentAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                    }

                    // ── Item Cards ───────────────────────────────────────────
                    if (filteredItems.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("—", color = TextSecondary, fontSize = 32.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        if (filterType != null) "Nessun elemento in questa categoria"
                                        else "Nessun elemento. Aggiungine uno!",
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    } else {
                        items(filteredItems, key = { it.id }) { item ->
                            ItemRow(
                                name = item.name,
                                type = item.type,
                                notes = item.notes,
                                onClick = { selectedItemId = item.id },
                                modifier = Modifier.animateItem(
                                    fadeInSpec = tween(200),
                                    fadeOutSpec = tween(200),
                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                )
                            )
                        }
                    }
                }
            }

            // ── FAB ──────────────────────────────────────────────────────────
            FloatingActionButton(
                onClick = { showAddSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp),
                containerColor = AccentAmber,
                contentColor = Color(0xFF1A1100),
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aggiungi elemento")
            }
        }
    }

    // ── Add Item Dialog ───────────────────────────────────────────────────────
    if (showAddSheet) {
        AlertDialog(
            onDismissRequest = { showAddSheet = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = SurfaceDark,
            tonalElevation = 0.dp,
            title = {
                Text(
                    "Nuovo elemento",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Nome", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentAmber,
                            unfocusedBorderColor = DividerColor,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentAmber,
                            focusedLabelColor = AccentAmber
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuBox(
                        expanded = typeMenuExpanded,
                        onExpandedChange = { typeMenuExpanded = !typeMenuExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tipo", color = TextSecondary) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenuExpanded) },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AccentAmber,
                                unfocusedBorderColor = DividerColor,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedLabelColor = AccentAmber
                            ),
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = typeMenuExpanded,
                            onDismissRequest = { typeMenuExpanded = false },
                            modifier = Modifier.background(SurfaceElevated)
                        ) {
                            ItemType.entries.forEach { t ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                typeIcon(t),
                                                contentDescription = null,
                                                tint = typeColor(t),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                typeLabel(t),
                                                color = if (selectedType == t) AccentAmber else TextPrimary
                                            )
                                        }
                                    },
                                    onClick = {
                                        selectedType = t
                                        typeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isNotBlank()) {
                            vm.addItem(newName, selectedType)
                            newName = ""
                            showAddSheet = false
                        }
                    },
                    enabled = newName.isNotBlank(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentAmber,
                        contentColor = Color(0xFF1A1100),
                        disabledContainerColor = AccentAmber.copy(alpha = 0.3f)
                    )
                ) {
                    Text("Aggiungi", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showAddSheet = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
                ) {
                    Text("Annulla")
                }
            }
        )
    }
}

// ── Navigation Drawer Content ─────────────────────────────────────────────────
@Composable
private fun NavigationDrawerContent(
    availableTypes: List<ItemType?>,
    selectedType: ItemType?,
    itemCounts: Map<ItemType, Int>,
    totalCount: Int,
    onSelect: (ItemType?) -> Unit
) {
    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp),
        drawerContainerColor = DrawerBackground,
        drawerTonalElevation = 0.dp,
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(SurfaceDark, DrawerBackground))
                )
                .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 28.dp)
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AccentAmber.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Notifications,
                        contentDescription = null,
                        tint = AccentAmber,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Rimembranze",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "$totalCount elementi totali",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
        Spacer(Modifier.height(12.dp))

        Text(
            text = "CATEGORIE",
            color = TextSecondary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
        )

        DrawerItem(
            icon = typeIcon(null),
            label = "Tutti",
            count = totalCount,
            color = AccentAmber,
            isSelected = selectedType == null,
            onClick = { onSelect(null) }
        )

        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            thickness = 0.5.dp,
            color = DividerColor
        )

        availableTypes
            .filterNotNull()
            .forEach { type ->
                DrawerItem(
                    icon = typeIcon(type),
                    label = typeLabel(type),
                    count = itemCounts[type] ?: 0,
                    color = typeColor(type),
                    isSelected = selectedType == type,
                    onClick = { onSelect(type) }
                )
            }
    }
}

@Composable
private fun DrawerItem(
    icon: ImageVector,
    label: String,
    count: Int,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgAlpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0f,
        animationSpec = tween(200),
        label = "drawer_bg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f * bgAlpha))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.copy(alpha = if (isSelected) 0.20f else 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (isSelected) color else color.copy(alpha = 0.7f),
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(Modifier.width(14.dp))

        Text(
            text = label,
            color = if (isSelected) TextPrimary else TextSecondary,
            fontSize = 15.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )

        if (count > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSelected) color.copy(alpha = 0.20f)
                        else Color(0xFF2C2C3A)
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "$count",
                    color = if (isSelected) color else TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ── ItemRow ───────────────────────────────────────────────────────────────────
@Composable
private fun ItemRow(
    name: String,
    type: ItemType,
    notes: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val color = typeColor(type)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )

            Spacer(Modifier.width(14.dp))

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    typeIcon(type),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = typeLabel(type),
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    if (!notes.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = notes,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ── UpcomingDeadlineRow ───────────────────────────────────────────────────────
@Composable
private fun UpcomingDeadlineRow(
    category: String,
    dateEpochMs: Long,
    onClick: () -> Unit,
    isExpired: Boolean = false
) {
    val daysLate = if (isExpired)
        ((System.currentTimeMillis() - dateEpochMs) / (1000L * 60 * 60 * 24)).toInt()
    else 0

    val rowColor   = if (isExpired) DestructiveRed else AccentAmber
    val bgColor    = if (isExpired) DestructiveRed.copy(alpha = 0.06f) else Color.Transparent
    val dateLabel  = if (isExpired) {
        when (daysLate) {
            0    -> "Scaduto oggi"
            1    -> "Scaduto ieri"
            else -> "Scaduto da $daysLate giorni"
        }
    } else formatDate(dateEpochMs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot — per gli scaduti pulsa visivamente con alpha
        val dotAlpha by androidx.compose.animation.core.animateFloatAsState(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                animation = androidx.compose.animation.core.tween(800),
                repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
            ),
            label = "dot_pulse"
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(
                    if (isExpired) rowColor.copy(alpha = dotAlpha)
                    else rowColor
                )
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category,
                color = if (isExpired) TextPrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = if (isExpired) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isExpired) {
                Spacer(Modifier.height(1.dp))
                Text(
                    text = formatDate(dateEpochMs),
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(rowColor.copy(alpha = if (isExpired) 0.18f else 0.12f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = dateLabel,
                color = rowColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = rowColor.copy(alpha = 0.5f),
            modifier = Modifier.size(16.dp)
        )
    }
    HorizontalDivider(
        modifier = Modifier.padding(start = 40.dp),
        thickness = 0.5.dp,
        color = DividerColor
    )
}

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))
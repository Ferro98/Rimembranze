package com.example.rimembranze.ui

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.data.db.RecordEntity
import com.example.rimembranze.data.db.RecordType
import com.example.rimembranze.notifications.DeadlineReminderScheduler
import com.example.rimembranze.ui.vm.ItemDetailViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Palette ────────────────────────────────────────────────────────────────────
private val BackgroundDark   = Color(0xFF0F0F13)
private val SurfaceDark      = Color(0xFF1A1A22)
private val SurfaceElevated  = Color(0xFF23232E)
private val AccentAmber      = Color(0xFFE8A020)
private val AccentAmberLight = Color(0xFFFFCA6A)
private val AccentGreen      = Color(0xFF5BEF9A)
private val TextPrimary      = Color(0xFFF0EEE8)
private val TextSecondary    = Color(0xFF8A8898)
private val DividerColor     = Color(0xFF2C2C3A)
val DestructiveRed   = Color(0xFFE05858)

// Opzioni preavviso disponibili — ordine decrescente per UI
private val REMINDER_OPTIONS = listOf(
    30 to "30 giorni",
    14 to "14 giorni",
    7  to "7 giorni",
    1 to "1 giorno",
    0  to "Mattina"
)

private fun csvToSet(csv: String): Set<Int> =
    csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

private fun setToCsv(set: Set<Int>): String =
    set.sortedDescending().joinToString(",")

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun ItemDetailScreen(
    itemId: Long,
    scrollToDeadlineId: Long? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val vm: ItemDetailViewModel = viewModel()
    val stateFlow = remember(itemId) { vm.observe(itemId) }
    val state by stateFlow.collectAsState()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showAddDeadlineDialog by remember { mutableStateOf(false) }
    var showAddRecordDialog   by remember { mutableStateOf(false) }
    var showDeleteItemDialog  by remember { mutableStateOf(false) }
    var fabExpanded           by remember { mutableStateOf(false) }
    var editingDeadline       by remember { mutableStateOf<DeadlineEntity?>(null) }

    val listState = rememberLazyListState()
    var hasScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(scrollToDeadlineId, state.deadlines) {
        if (scrollToDeadlineId == null) return@LaunchedEffect
        if (hasScrolled) return@LaunchedEffect
        if (state.deadlines.isEmpty()) return@LaunchedEffect

        val deadlineIndex = state.deadlines.indexOfFirst { it.id == scrollToDeadlineId }
        if (deadlineIndex == -1) return@LaunchedEffect

        hasScrolled = true
        while (listState.layoutInfo.totalItemsCount == 0) {
            kotlinx.coroutines.delay(16)
        }
        listState.animateScrollToItem(index = 2 + deadlineIndex, scrollOffset = -80)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        when {
            state.isLoading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = AccentAmber,
                strokeWidth = 2.dp
            )

            state.item == null -> Text(
                "Item non trovato",
                color = TextSecondary,
                modifier = Modifier.align(Alignment.Center)
            )

            else -> {
                val item = state.item!!

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 160.dp)
                ) {

                    // ── Hero header ──────────────────────────────────────────
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark))
                                )
                                .padding(horizontal = 20.dp, vertical = 24.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(SurfaceElevated)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Indietro",
                                            tint = TextPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { showDeleteItemDialog = true },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(DestructiveRed.copy(alpha = 0.12f))
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Elimina item",
                                            tint = DestructiveRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(20.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(AccentAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = item.type.name.uppercase(),
                                        color = AccentAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.5.sp
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = item.name,
                                    color = TextPrimary,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 34.sp
                                )
                                if (!item.notes.isNullOrBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = item.notes,
                                        color = TextSecondary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // ── Sezione SCADENZE ─────────────────────────────────────
                    item { SectionHeader("SCADENZE", state.deadlines.size) }

                    if (state.deadlines.isEmpty()) {
                        item { EmptyState("Nessuna scadenza registrata") }
                    }

                    items(state.deadlines, key = { "deadline_${it.id}" }) { d ->
                        DeadlineCard(
                            deadline      = d,
                            isHighlighted = d.id == scrollToDeadlineId,
                            onMarkPaid = { cents ->
                                scope.launch {
                                    val nextDue = vm.markAsPaidAndReturnNextDueDate(d, amountCents = cents)
                                    if (nextDue == null) {
                                        DeadlineReminderScheduler.cancel(context, d.id)
                                    } else {
                                        DeadlineReminderScheduler.schedule(
                                            context        = context,
                                            deadlineId     = d.id,
                                            dueDateEpochMs = nextDue,
                                            reminderDaysCsv = d.reminderDaysCsv
                                        )
                                    }
                                }
                            },
                            onDelete = {
                                vm.deleteDeadline(d)
                                DeadlineReminderScheduler.cancel(context, d.id)
                            },
                            onEdit = { editingDeadline = d }
                        )
                    }

                    // ── Sezione STORICO ──────────────────────────────────────
                    item {
                        Spacer(Modifier.height(10.dp))
                        SectionHeader("STORICO", state.records.size)
                    }

                    if (state.records.isEmpty()) {
                        item { EmptyState("Nessuna registrazione") }
                    } else {
                        items(state.records, key = { "record_${it.id}" }) { r ->
                            RecordCard(
                                record = r,
                                onDelete = { vm.deleteRecord(r) },
                                onUpdateUniSalute = { sent, status, sentEpochMs ->
                                    vm.updateRecordUniSalute(r, sent, status, sentEpochMs)
                                }
                            )
                        }
                    }
                }

                // ── FAB espandibile ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    AnimatedVisibility(
                        visible = fabExpanded,
                        enter = fadeIn() + slideInVertically { it },
                        exit  = fadeOut() + slideOutVertically { it }
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Aggiungi record",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceElevated)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = { showAddRecordDialog = true; fabExpanded = false },
                                    containerColor = SurfaceElevated,
                                    contentColor = AccentGreen
                                ) {
                                    Icon(Icons.Default.Receipt, contentDescription = null)
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Aggiungi scadenza",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(SurfaceElevated)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                                SmallFloatingActionButton(
                                    onClick = { showAddDeadlineDialog = true; fabExpanded = false },
                                    containerColor = SurfaceElevated,
                                    contentColor = AccentAmber
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                                }
                            }
                        }
                    }

                    FloatingActionButton(
                        onClick = { fabExpanded = !fabExpanded },
                        containerColor = AccentAmber,
                        contentColor = Color(0xFF1A1100),
                        shape = CircleShape,
                        elevation = FloatingActionButtonDefaults.elevation(8.dp)
                    ) {
                        AnimatedContent(
                            targetState = fabExpanded,
                            transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                            label = "fab_icon"
                        ) { expanded ->
                            Icon(
                                if (expanded) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Dialog aggiungi scadenza ─────────────────────────────────────────────
    if (showAddDeadlineDialog) {
        DeadlineDialog(
            title     = "Nuova scadenza",
            onDismiss = { showAddDeadlineDialog = false },
            onSave    = { category, dateMs, recurrence, notes, amountCents, reminderDaysCsv ->
                scope.launch {
                    val id = vm.addDeadlineAndReturnId(
                        itemId          = itemId,
                        category        = category,
                        dueDateMs       = dateMs,
                        recurrence      = recurrence,
                        notes           = notes,
                        amountCents     = amountCents,
                        reminderDaysCsv = reminderDaysCsv
                    )
                    DeadlineReminderScheduler.schedule(
                        context         = context,
                        deadlineId      = id,
                        dueDateEpochMs  = dateMs,
                        reminderDaysCsv = reminderDaysCsv
                    )
                }
                showAddDeadlineDialog = false
            }
        )
    }

    // ── Dialog modifica scadenza ─────────────────────────────────────────────
    editingDeadline?.let { d ->
        DeadlineDialog(
            title                  = "Modifica scadenza",
            initialCategory        = d.category,
            initialDateEpochMs     = d.dueDateEpochMs,
            initialRecurrence      = d.recurrence,
            initialNotes           = d.notes,
            initialAmountCents     = d.lastCostCents,
            initialReminderDaysCsv = d.reminderDaysCsv,
            onDismiss              = { editingDeadline = null },
            onSave = { category, dateMs, recurrence, notes, amountCents, reminderDaysCsv ->
                scope.launch {
                    vm.updateDeadline(
                        d.copy(
                            category        = category,
                            dueDateEpochMs  = dateMs,
                            recurrence      = recurrence,
                            notes           = notes,
                            lastCostCents   = amountCents,
                            reminderDaysCsv = reminderDaysCsv
                        )
                    )
                    DeadlineReminderScheduler.schedule(
                        context         = context,
                        deadlineId      = d.id,
                        dueDateEpochMs  = dateMs,
                        reminderDaysCsv = reminderDaysCsv
                    )
                }
                editingDeadline = null
            }
        )
    }

    // ── Dialog aggiungi record ───────────────────────────────────────────────
    if (showAddRecordDialog) {
        AddRecordDialog(
            onDismiss = { showAddRecordDialog = false },
            onSave = { type, title, dateMs, amountCents, notes ->
                scope.launch {
                    vm.addRecordAndReturnId(
                        itemId      = itemId,
                        type        = type,
                        title       = title,
                        dateEpochMs = dateMs,
                        amountCents = amountCents,
                        notes       = notes
                    )
                }
                showAddRecordDialog = false
            }
        )
    }

    // ── Dialog elimina item ──────────────────────────────────────────────────
    if (showDeleteItemDialog) {
        state.item?.let { itemToDelete ->
            AlertDialog(
                onDismissRequest = { showDeleteItemDialog = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = SurfaceDark,
                tonalElevation = 0.dp,
                icon = {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = DestructiveRed,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Elimina item",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Stai per eliminare", color = TextSecondary,
                            fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text("\"${itemToDelete.name}\"", color = TextPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 16.sp,
                            textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Verranno eliminate anche tutte le scadenze e lo storico associati. L'operazione non è reversibile.",
                            color = TextSecondary, fontSize = 13.sp,
                            textAlign = TextAlign.Center, lineHeight = 18.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteItemDialog = false
                            vm.deleteItem(itemToDelete) { onBack() }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DestructiveRed,
                            contentColor   = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Elimina definitivamente", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteItemDialog = false },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Annulla") }
                }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DeadlineDialog — con selettore preavvisi
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeadlineDialog(
    title: String,
    initialCategory: String = "",
    initialDateEpochMs: Long? = null,
    initialRecurrence: String = "NONE",
    initialNotes: String? = null,
    initialAmountCents: Long? = null,
    initialReminderDaysCsv: String = "30,14,7,1",
    onDismiss: () -> Unit,
    onSave: (
        category: String,
        dateEpochMs: Long,
        recurrence: String,
        notes: String?,
        amountCents: Long?,
        reminderDaysCsv: String
    ) -> Unit
) {
    var category       by remember { mutableStateOf(initialCategory) }
    var selectedDate   by remember { mutableStateOf(initialDateEpochMs) }
    var recurrence     by remember { mutableStateOf(initialRecurrence) }
    var notes          by remember { mutableStateOf(initialNotes ?: "") }
    var amountRaw      by remember {
        mutableStateOf(initialAmountCents?.let { "%.2f".format(it / 100.0) } ?: "")
    }
    var recExpanded    by remember { mutableStateOf(false) }
    var selectedDays   by remember { mutableStateOf(csvToSet(initialReminderDaysCsv)) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = {
            Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
            ) {

                // ── Categoria ────────────────────────────────────────────────
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoria", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Data ─────────────────────────────────────────────────────
                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                selectedDate = Calendar.getInstance().apply {
                                    set(year, month, day, 0, 0, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            },
                            cal.get(Calendar.YEAR),
                            cal.get(Calendar.MONTH),
                            cal.get(Calendar.DAY_OF_MONTH)
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedDate != null) AccentAmberLight else TextSecondary
                    )
                ) {
                    Text(
                        selectedDate?.let { "📅  ${formatDate(it)}" } ?: "Seleziona data",
                        fontSize = 14.sp
                    )
                }

                // ── Ricorrenza ───────────────────────────────────────────────
                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(
                    expanded = recExpanded,
                    onExpandedChange = { recExpanded = !recExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = recurrenceLabel(recurrence),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ricorrenza", color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(recExpanded) },
                        shape = RoundedCornerShape(12.dp),
                        colors = dialogFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = recExpanded,
                        onDismissRequest = { recExpanded = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        recurrenceOptions.forEach { (v, l) ->
                            DropdownMenuItem(
                                text = {
                                    Text(l, color = if (recurrence == v) AccentAmber else TextPrimary)
                                },
                                onClick = { recurrence = v; recExpanded = false }
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                // ── Preavvisi ────────────────────────────────────────────────
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Notificami",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    REMINDER_OPTIONS.chunked(3).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            repeat(3) { index ->
                                val option = row.getOrNull(index)
                                if (option != null) {
                                    val (days, label) = option
                                    val selected = days in selectedDays
                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            selectedDays = if (selected)
                                                selectedDays - days
                                            else
                                                selectedDays + days
                                        },
                                        label = {
                                            Text(
                                                label,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth(),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AccentAmber.copy(alpha = 0.20f),
                                            selectedLabelColor     = AccentAmber,
                                            containerColor         = SurfaceElevated,
                                            labelColor             = TextSecondary
                                        ),
                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled             = true,
                                            selected            = selected,
                                            selectedBorderColor = AccentAmber.copy(alpha = 0.5f),
                                            selectedBorderWidth = 1.dp,
                                            borderColor         = DividerColor,
                                            borderWidth         = 0.5.dp
                                        )
                                    )
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                // ── Importo (opzionale) ──────────────────────────────────────
                OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { raw ->
                        if (raw.isEmpty() || raw.matches(Regex("\\d{0,7}([.,]\\d{0,2})?")))
                            amountRaw = raw
                    },
                    label = { Text("Importo (opzionale)", color = TextSecondary) },
                    placeholder = { Text("es. 85.00", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text(
                            "€",
                            color = if (amountRaw.isNotBlank()) AccentAmberLight else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Note (opzionale) ─────────────────────────────────────────
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (opzionale)", color = TextSecondary) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (category.isNotBlank() && selectedDate != null) {
                        val cents = amountRaw.replace(",", ".")
                            .toDoubleOrNull()?.let { (it * 100).toLong() }
                        onSave(
                            category.trim(),
                            selectedDate!!,
                            recurrence,
                            notes.trimEnd().ifBlank { null },
                            cents,
                            setToCsv(selectedDays)
                        )
                    }
                },
                enabled = category.isNotBlank() && selectedDate != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor = Color(0xFF1A1100),
                    disabledContainerColor = AccentAmber.copy(alpha = 0.3f)
                )
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}

private fun recurrenceLabel(value: String): String = when (value) {
    com.example.rimembranze.data.Recurrence.NONE       -> "Nessuna"
    com.example.rimembranze.data.Recurrence.MONTHLY    -> "Mensile"
    com.example.rimembranze.data.Recurrence.QUARTERLY  -> "Trimestrale"
    com.example.rimembranze.data.Recurrence.SEMIANNUAL -> "Semestrale"
    com.example.rimembranze.data.Recurrence.YEARLY     -> "Annuale"
    else -> "Nessuna"
}

private val recurrenceOptions = listOf(
    com.example.rimembranze.data.Recurrence.NONE       to "Nessuna",
    com.example.rimembranze.data.Recurrence.MONTHLY    to "Mensile",
    com.example.rimembranze.data.Recurrence.QUARTERLY  to "Trimestrale",
    com.example.rimembranze.data.Recurrence.SEMIANNUAL to "Semestrale",
    com.example.rimembranze.data.Recurrence.YEARLY     to "Annuale"
)

// ─────────────────────────────────────────────────────────────────────────────
// DeadlineCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeadlineCard(
    deadline: DeadlineEntity,
    isHighlighted: Boolean = false,
    onMarkPaid: (amountCents: Long?) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var deleteConfirm      by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }

    val daysLeft = ((deadline.dueDateEpochMs - System.currentTimeMillis()) /
            (1000L * 60 * 60 * 24)).toInt()
    val urgencyColor = when {
        daysLeft < 0   -> DestructiveRed
        daysLeft <= 7  -> DestructiveRed
        daysLeft <= 30 -> AccentAmber
        else           -> AccentAmberLight
    }

    val cardBorder = if (isHighlighted)
        androidx.compose.foundation.BorderStroke(1.5.dp, AccentAmber.copy(alpha = 0.6f))
    else null

    // Stringa leggibile dei preavvisi, es. "14 giorni prima · 7 giorni prima · giorno stesso"
    val reminderLabel = remember(deadline.reminderDaysCsv) {
        deadline.reminderDaysCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .sortedDescending()
            .joinToString(" · ") { days ->
                when (days) {
                    0    -> "giorno stesso"
                    1    -> "1 giorno prima"
                    else -> "$days giorni prima"
                }
            }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(urgencyColor))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = deadline.category,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Modifica",
                        tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = { deleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina",
                        tint = if (deleteConfirm) DestructiveRed else TextSecondary,
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    label = if (daysLeft < 0) "Scaduta il" else "Scade il",
                    value = formatDate(deadline.dueDateEpochMs),
                    valueColor = urgencyColor,
                    modifier = Modifier.weight(1f)
                )
                deadline.lastCostCents?.let { cents ->
                    InfoChip(
                        label = "Importo",
                        value = "€ ${"%.2f".format(cents / 100.0)}",
                        valueColor = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!deadline.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(text = deadline.notes, color = TextSecondary,
                        fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            // ── Nota preavvisi ───────────────────────────────────────────────
            if (reminderLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.NotificationsNone,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = reminderLabel,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = deleteConfirm, label = "del_anim") { confirming ->
                if (confirming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { deleteConfirm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = ButtonDefaults.outlinedButtonBorder.copy()
                        ) { Text("Annulla", fontSize = 13.sp) }
                        Button(
                            onClick = { onDelete(); deleteConfirm = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DestructiveRed,
                                contentColor = Color.White
                            )
                        ) { Text("Conferma", fontSize = 13.sp) }
                    }
                } else {
                    Button(
                        onClick = { showMarkPaidDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentAmber.copy(alpha = 0.15f),
                            contentColor = AccentAmber
                        ),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Segna come pagata", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showMarkPaidDialog) {
        MarkAsPaidDialog(
            prefilledCents = deadline.lastCostCents,
            onDismiss = { showMarkPaidDialog = false },
            onConfirm = { amountCents ->
                onMarkPaid(amountCents)
                showMarkPaidDialog = false
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// RecordCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecordCard(
    record: RecordEntity,
    onDelete: () -> Unit,
    onUpdateUniSalute: (sent: Boolean, status: String?, sentEpochMs: Long?) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (record.type) {
                                RecordType.Pagamento.name -> AccentAmber
                                RecordType.Visita.name    -> AccentGreen
                                else                      -> Color(0xFFBF5BEF)
                            }
                        )
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = record.title,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Elimina",
                        tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(
                    label = record.type,
                    value = formatDate(record.dateEpochMs),
                    valueColor = AccentAmberLight,
                    modifier = Modifier.weight(1f)
                )
                record.amountCents?.let { cents ->
                    InfoChip(
                        label = "Importo",
                        value = "€ ${"%.2f".format(cents / 100.0)}",
                        valueColor = AccentGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            if (!record.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceElevated)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(record.notes, color = TextSecondary,
                        fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            if (record.type == RecordType.Visita.name || record.type == RecordType.Pagamento.name) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                Spacer(Modifier.height(10.dp))

                var sent           by remember { mutableStateOf(record.unisaluteSent) }
                var status         by remember { mutableStateOf(record.unisaluteStatus ?: "") }
                var statusExpanded by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00AEEF))
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Rimborso UniSalute",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = sent,
                        onCheckedChange = { checked ->
                            sent = checked
                            val newStatus   = if (checked) "InAttesa" else null
                            val newEpochMs  = if (checked) System.currentTimeMillis() else null
                            status = newStatus ?: ""
                            onUpdateUniSalute(checked, newStatus, newEpochMs)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF00AEEF)
                        )
                    )
                }

                AnimatedVisibility(visible = sent) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        val statusOptions = listOf(
                            "InAttesa"  to "In attesa",
                            "Approvato" to "Approvato",
                            "Rifiutato" to "Rifiutato"
                        )
                        val statusColor = when (status) {
                            "Approvato" -> AccentGreen
                            "Rifiutato" -> DestructiveRed
                            else        -> Color(0xFF00AEEF)
                        }

                        @OptIn(ExperimentalMaterial3Api::class)
                        ExposedDropdownMenuBox(
                            expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = statusOptions.firstOrNull { it.first == status }?.second ?: "In attesa",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Stato rimborso", color = TextSecondary, fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                leadingIcon = {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor   = statusColor,
                                    unfocusedBorderColor = DividerColor,
                                    focusedTextColor     = TextPrimary,
                                    unfocusedTextColor   = TextPrimary,
                                    focusedLabelColor    = statusColor
                                ),
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false },
                                modifier = Modifier.background(SurfaceElevated)
                            ) {
                                statusOptions.forEach { (value, label) ->
                                    val optColor = when (value) {
                                        "Approvato" -> AccentGreen
                                        "Rifiutato" -> DestructiveRed
                                        else        -> Color(0xFF00AEEF)
                                    }
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(optColor))
                                                Spacer(Modifier.width(10.dp))
                                                Text(label, color = if (status == value) optColor else TextPrimary)
                                            }
                                        },
                                        onClick = {
                                            status = value
                                            statusExpanded = false
                                            onUpdateUniSalute(true, value, record.unisaluteSentEpochMs)
                                        }
                                    )
                                }
                            }
                        }

                        record.unisaluteSentEpochMs?.let { ms ->
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Inviato il ${formatDate(ms)}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Componenti condivisi
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = TextSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            modifier = Modifier.weight(1f))
        Text("$count", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    Divider(color = DividerColor, thickness = 0.5.dp)
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(28.dp),
        contentAlignment = Alignment.Center
    ) { Text(message, color = TextSecondary, fontSize = 14.sp) }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentAmber,
    unfocusedBorderColor = DividerColor,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AccentAmber,
    focusedLabelColor    = AccentAmber
)

private fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))
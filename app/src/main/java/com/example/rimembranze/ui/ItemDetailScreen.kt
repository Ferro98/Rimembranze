package com.example.rimembranze.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.notifications.AppointmentReminderScheduler
import com.example.rimembranze.notifications.DeadlineReminderScheduler
import com.example.rimembranze.ui.components.*
import com.example.rimembranze.ui.vm.ItemDetailViewModel
import com.example.rimembranze.ui.vm.ItemStats
import kotlinx.coroutines.launch

private enum class DetailTab { SCADENZE, APPUNTAMENTI }

@Composable
fun ItemDetailScreen(
    itemId: Long,
    scrollToDeadlineId: Long? = null,
    scrollToAppointmentId: Long? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val vm: ItemDetailViewModel = viewModel()
    val state by remember(itemId) { vm.observe(itemId) }.collectAsState()

    val context      = LocalContext.current
    val scope        = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // Launcher per ACTION_CREATE_DOCUMENT — salva CSV nella posizione scelta dall'utente
    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            val allAppointments = state.appointmentsPending +
                    state.appointmentsDoneNotPaid + state.appointmentsPaid
            val csvContent = vm.buildCsvContent(state.records, allAppointments)
            vm.writeCsvToUri(context, uri, csvContent)
        }
    }

    var activeTab by remember {
        mutableStateOf(if (scrollToAppointmentId != null) DetailTab.APPUNTAMENTI else DetailTab.SCADENZE)
    }
    var showAddDeadlineDialog    by remember { mutableStateOf(false) }
    var showAddRecordDialog      by remember { mutableStateOf(false) }
    var showAddAppointmentDialog by remember { mutableStateOf(false) }
    var showDeleteItemDialog     by remember { mutableStateOf(false) }
    var showInvoiceDialog        by remember { mutableStateOf(false) }
    var fabExpanded              by remember { mutableStateOf(false) }
    var editingDeadline          by remember { mutableStateOf<DeadlineEntity?>(null) }

    val listState       = rememberLazyListState()
    var hasScrolled     by remember { mutableStateOf(false) }
    var activeHighlight by remember { mutableStateOf(scrollToDeadlineId) }

    var hasScrolledAppointment     by remember { mutableStateOf(false) }
    var activeHighlightAppointment by remember { mutableStateOf(scrollToAppointmentId) }

    // Scroll + highlight scadenza
    LaunchedEffect(scrollToDeadlineId, state.deadlines) {
        if (scrollToDeadlineId == null || hasScrolled || state.deadlines.isEmpty()) return@LaunchedEffect
        val idx = state.deadlines.indexOfFirst { it.id == scrollToDeadlineId }
        if (idx == -1) return@LaunchedEffect
        hasScrolled = true
        while (listState.layoutInfo.totalItemsCount == 0) kotlinx.coroutines.delay(16)
        listState.animateScrollToItem(index = 3 + idx, scrollOffset = -80)
        kotlinx.coroutines.delay(2000)
        activeHighlight = null
    }

    // Scroll + highlight appuntamento (stesso schema della scadenza: header(0) + tab(1) +
    // header sezione "prossimi"(2) → l'appuntamento pending idx-esimo è all'indice 3+idx)
    LaunchedEffect(scrollToAppointmentId, activeTab, state.appointmentsPending) {
        if (scrollToAppointmentId == null || hasScrolledAppointment ||
            activeTab != DetailTab.APPUNTAMENTI || state.appointmentsPending.isEmpty()
        ) return@LaunchedEffect
        val idx = state.appointmentsPending.indexOfFirst { it.id == scrollToAppointmentId }
        if (idx == -1) return@LaunchedEffect
        hasScrolledAppointment = true
        while (listState.layoutInfo.totalItemsCount == 0) kotlinx.coroutines.delay(16)
        listState.animateScrollToItem(index = 3 + idx, scrollOffset = -80)
        kotlinx.coroutines.delay(2000)
        activeHighlightAppointment = null
    }

    // Snackbar feedback pagamento ricorrente
    LaunchedEffect(state.lastPaidNextDueDate) {
        val next = state.lastPaidNextDueDate ?: return@LaunchedEffect
        snackbarHost.showSnackbar(
            message = "✓ Pagata — prossima scadenza: ${formatDate(next)}",
            duration = SnackbarDuration.Short
        )
        vm.clearNextDueDateFeedback()
    }

    // Snackbar feedback esito export CSV
    val csvExportResult by vm.csvExportResult.collectAsState()
    LaunchedEffect(csvExportResult) {
        val success = csvExportResult ?: return@LaunchedEffect
        snackbarHost.showSnackbar(
            message = if (success) "✓ CSV esportato con successo" else "✗ Esportazione CSV fallita",
            duration = SnackbarDuration.Short
        )
        vm.clearCsvExportResult()
    }

    val fabColor by animateColorAsState(
        targetValue = if (activeTab == DetailTab.SCADENZE) AccentAmber else AccentBlue,
        animationSpec = tween(300), label = "fab_color"
    )

    Scaffold(
        containerColor = BackgroundDark,
        snackbarHost = {
            SnackbarHost(snackbarHost) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceElevated,
                    contentColor = if (data.visuals.message.startsWith("✗")) DestructiveRed else AccentGreen,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .statusBarsPadding()
        ) {
            when {
                state.isLoading    -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center), color = AccentAmber, strokeWidth = 2.dp)
                state.item == null -> Text("Item non trovato", color = TextSecondary,
                    modifier = Modifier.align(Alignment.Center))
                else -> {
                    val item             = state.item!!
                    val deadlineCount    = state.deadlines.size
                    val appointmentCount = state.appointmentsPending.size +
                            state.appointmentsDoneNotPaid.size + state.appointmentsPaid.size

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {

                        // ── Hero header ──────────────────────────────────────
                        item {
                            Box(modifier = Modifier.fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark)))
                                .padding(horizontal = 20.dp, vertical = 24.dp)) {
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = onBack,
                                            modifier = Modifier.clip(CircleShape).background(SurfaceElevated).size(40.dp)) {
                                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro",
                                                tint = TextPrimary, modifier = Modifier.size(18.dp))
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            // Export CSV
                                            IconButton(onClick = {
                                                val safeName = item.name.replace(Regex("[^A-Za-z0-9_\\-]"), "_")
                                                csvLauncher.launch("${safeName}_export.csv")
                                            }, modifier = Modifier.clip(CircleShape)
                                                .background(AccentAmber.copy(alpha = 0.12f)).size(40.dp)) {
                                                Icon(Icons.Default.Share, "Esporta CSV",
                                                    tint = AccentAmber, modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { showDeleteItemDialog = true },
                                                modifier = Modifier.clip(CircleShape)
                                                    .background(DestructiveRed.copy(alpha = 0.12f)).size(40.dp)) {
                                                Icon(Icons.Default.Delete, "Elimina",
                                                    tint = DestructiveRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                                        .background(AccentAmber.copy(alpha = 0.15f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text(item.type.name.uppercase(), color = AccentAmber,
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(item.name, color = TextPrimary, fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold, lineHeight = 34.sp)
                                    if (!item.notes.isNullOrBlank()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text(item.notes, color = TextSecondary, fontSize = 14.sp)
                                    }
                                    // ── Stats strip ─────────────────────────
                                    Spacer(Modifier.height(16.dp))
                                    StatsStrip(stats = state.stats)
                                }
                            }
                        }

                        // ── Tab switcher ─────────────────────────────────────
                        item {
                            Row(modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TabCard(label = "SCADENZE", count = deadlineCount,
                                    active = activeTab == DetailTab.SCADENZE, color = AccentAmber,
                                    modifier = Modifier.weight(1f)) { activeTab = DetailTab.SCADENZE }
                                TabCard(label = "APPUNTAMENTI", count = appointmentCount,
                                    active = activeTab == DetailTab.APPUNTAMENTI, color = AccentBlue,
                                    modifier = Modifier.weight(1f)) { activeTab = DetailTab.APPUNTAMENTI }
                            }
                        }

                        // ── Tab: Scadenze ─────────────────────────────────────
                        if (activeTab == DetailTab.SCADENZE) {
                            item(key = "sec_scadenze") { SectionHeader("SCADENZE", state.deadlines.size) }
                            if (state.deadlines.isEmpty()) {
                                item(key = "empty_deadlines") { EmptyState("Nessuna scadenza registrata", "📅") }
                            } else {
                                items(state.deadlines, key = { "dl_${it.id}" }) { d ->
                                    DeadlineCard(
                                        deadline      = d,
                                        isHighlighted = d.id == activeHighlight,
                                        onMarkPaid = { cents ->
                                            scope.launch {
                                                val next = vm.markAsPaidAndReturnNextDueDate(d, cents)
                                                if (next == null) DeadlineReminderScheduler.cancel(context, d.id)
                                                else DeadlineReminderScheduler.schedule(context, d.id, next, d.reminderDaysCsv)
                                            }
                                        },
                                        onDelete = { vm.deleteDeadline(d); DeadlineReminderScheduler.cancel(context, d.id) },
                                        onEdit   = { editingDeadline = d },
                                        modifier = Modifier.animateItem(
                                            fadeInSpec    = tween(250),
                                            fadeOutSpec   = tween(200),
                                            placementSpec = spring(stiffness = Spring.StiffnessMediumLow)
                                        )
                                    )
                                }
                            }
                            item(key = "spacer_records") { Spacer(Modifier.height(10.dp)) }
                            item(key = "sec_records") { SectionHeader("STORICO PAGAMENTI", state.records.size) }
                            if (state.records.isEmpty()) {
                                item(key = "empty_records") { EmptyState("Nessun pagamento registrato", "💳") }
                            } else {
                                items(state.records, key = { "rec_${it.id}" }) { r ->
                                    RecordCard(record = r, onDelete = { vm.deleteRecord(r) },
                                        onUpdateUniSalute = { s, st, ms -> vm.updateRecordUniSalute(r, s, st, ms) })
                                }
                            }
                        }

                        // ── Tab: Appuntamenti ─────────────────────────────────
                        if (activeTab == DetailTab.APPUNTAMENTI) {
                            item(key = "sec_pending") {
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text("PROSSIMI", color = TextSecondary, fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                    Text("${state.appointmentsPending.size}", color = AccentBlue,
                                        fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(12.dp))
                                    IconButton(onClick = { vm.toggleAppointmentsOrder() }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = if (state.appointmentsAscending) "Ordine discendente" else "Ordine ascendente",
                                            tint = AccentBlue, modifier = Modifier.size(16.dp))
                                    }
                                    Text(if (state.appointmentsAscending) "↑" else "↓",
                                        color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                            }
                            if (state.appointmentsPending.isEmpty()) {
                                item(key = "empty_pending") { EmptyState("Nessun appuntamento programmato", "🗓") }
                            } else {
                                items(state.appointmentsPending, key = { "appt_${it.id}" }) { a ->
                                    AppointmentCard(
                                        appointment   = a,
                                        isHighlighted = a.id == activeHighlightAppointment,
                                        onMarkDone = { notes, cents ->
                                            vm.markAppointmentDone(a, notes, cents)
                                            AppointmentReminderScheduler.cancel(context, a.id)
                                        },
                                        onDelete = {
                                            vm.deleteAppointment(a)
                                            AppointmentReminderScheduler.cancel(context, a.id)
                                        }
                                    )
                                }
                            }

                            if (state.appointmentsDoneNotPaid.isNotEmpty()) {
                                item(key = "spacer_donenotpaid") { Spacer(Modifier.height(10.dp)) }
                                item(key = "sec_donenotpaid") {
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        Text("DA FATTURARE", color = AccentGreen, fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.weight(1f))
                                        Text("${state.appointmentsDoneNotPaid.size}", color = AccentGreen,
                                            fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                                }
                                items(state.appointmentsDoneNotPaid, key = { "apptdone_${it.id}" }) { a ->
                                    AppointmentDoneCard(appointment = a, onDelete = {
                                        vm.deleteAppointment(a)
                                        AppointmentReminderScheduler.cancel(context, a.id)
                                    })
                                }
                                item(key = "invoice_summary") {
                                    val total = state.appointmentsDoneNotPaid.sumOf { it.amountCents ?: 0L }
                                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("${state.appointmentsDoneNotPaid.size} sedute non fatturate",
                                                color = TextSecondary, fontSize = 12.sp)
                                            if (total > 0) Text("Totale: €${"%.2f".format(total / 100.0)}",
                                                color = AccentGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                        Button(onClick = { showInvoiceDialog = true }, shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = AccentGreen.copy(alpha = 0.15f), contentColor = AccentGreen),
                                            elevation = ButtonDefaults.buttonElevation(0.dp)) {
                                            Icon(Icons.Default.Receipt, null, modifier = Modifier.size(15.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Crea fattura", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }

                            item(key = "spacer_history") { Spacer(Modifier.height(10.dp)) }
                            item(key = "sec_history") { SectionHeader("STORICO SEDUTE", state.appointmentsPaid.size, AccentBlue) }
                            if (state.appointmentsPaid.isEmpty()) {
                                item(key = "empty_history") { EmptyState("Nessuna seduta completata", "✓") }
                            } else {
                                items(state.appointmentsPaid, key = { "apptpaid_${it.id}" }) { a ->
                                    AppointmentDoneCard(appointment = a, showPaidBadge = true, onDelete = {
                                        vm.deleteAppointment(a)
                                        AppointmentReminderScheduler.cancel(context, a.id)
                                    })
                                }
                            }
                        }
                    }

                    // ── FAB ──────────────────────────────────────────────────
                    Column(modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.End) {
                        AnimatedVisibility(visible = fabExpanded,
                            enter = fadeIn() + slideInVertically { it },
                            exit  = fadeOut() + slideOutVertically { it }) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.End) {
                                if (activeTab == DetailTab.SCADENZE) {
                                    FabOption("Aggiungi record", AccentGreen, Icons.Default.Receipt) {
                                        showAddRecordDialog = true; fabExpanded = false }
                                    FabOption("Aggiungi scadenza", AccentAmber, Icons.Default.CalendarMonth) {
                                        showAddDeadlineDialog = true; fabExpanded = false }
                                } else {
                                    FabOption("Aggiungi appuntamento", AccentBlue, Icons.Default.EventAvailable) {
                                        showAddAppointmentDialog = true; fabExpanded = false }
                                }
                            }
                        }
                        FloatingActionButton(
                            onClick = { fabExpanded = !fabExpanded },
                            containerColor = fabColor,
                            contentColor = Color(0xFF0F0F13), shape = CircleShape,
                            elevation = FloatingActionButtonDefaults.elevation(8.dp)) {
                            AnimatedContent(targetState = fabExpanded,
                                transitionSpec = { fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut() },
                                label = "fab_icon") { expanded ->
                                Icon(if (expanded) Icons.Default.Close else Icons.Default.Add, null)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Dialogs ───────────────────────────────────────────────────────────────

    if (showAddDeadlineDialog) {
        DeadlineDialog(title = "Nuova scadenza", onDismiss = { showAddDeadlineDialog = false },
            onSave = { cat, dateMs, rec, notes, cents, csv ->
                scope.launch {
                    val id = vm.addDeadlineAndReturnId(itemId, cat, dateMs, rec, notes, cents, csv)
                    DeadlineReminderScheduler.schedule(context, id, dateMs, csv)
                }
                showAddDeadlineDialog = false
            })
    }

    editingDeadline?.let { d ->
        DeadlineDialog(title = "Modifica scadenza", initialCategory = d.category,
            initialDateEpochMs = d.dueDateEpochMs, initialRecurrence = d.recurrence,
            initialNotes = d.notes, initialAmountCents = d.lastCostCents,
            initialReminderDaysCsv = d.reminderDaysCsv,
            onDismiss = { editingDeadline = null },
            onSave = { cat, dateMs, rec, notes, cents, csv ->
                scope.launch {
                    vm.updateDeadline(d.copy(category = cat, dueDateEpochMs = dateMs,
                        recurrence = rec, notes = notes, lastCostCents = cents, reminderDaysCsv = csv))
                    DeadlineReminderScheduler.schedule(context, d.id, dateMs, csv)
                }
                editingDeadline = null
            })
    }

    if (showAddRecordDialog) {
        AddRecordDialog(onDismiss = { showAddRecordDialog = false },
            onSave = { type, title, dateMs, cents, notes ->
                scope.launch { vm.addRecordAndReturnId(itemId, type, title, dateMs, cents, notes) }
                showAddRecordDialog = false
            })
    }

    if (showAddAppointmentDialog) {
        AppointmentDialog(onDismiss = { showAddAppointmentDialog = false },
            onSave = { title, dateMs, notes, cents ->
                scope.launch {
                    val id = vm.addAppointmentAndReturnId(itemId, title, dateMs, notes, cents)
                    AppointmentReminderScheduler.schedule(context, id, dateMs)
                }
                showAddAppointmentDialog = false
            })
    }

    if (showInvoiceDialog) {
        InvoiceDialog(appointments = state.appointmentsDoneNotPaid,
            onDismiss = { showInvoiceDialog = false },
            onConfirm = { ids, notes ->
                val selected = state.appointmentsDoneNotPaid.filter { it.id in ids }
                vm.createInvoice(itemId, selected, notes)
                showInvoiceDialog = false
            })
    }

    if (showDeleteItemDialog) {
        state.item?.let { it ->
            AlertDialog(
                onDismissRequest = { showDeleteItemDialog = false },
                shape = RoundedCornerShape(24.dp), containerColor = SurfaceDark, tonalElevation = 0.dp,
                icon = { Icon(Icons.Default.Warning, null, tint = DestructiveRed, modifier = Modifier.size(32.dp)) },
                title = { Text("Elimina item", color = TextPrimary, fontWeight = FontWeight.Bold,
                    fontSize = 20.sp, textAlign = TextAlign.Center) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stai per eliminare", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Text("\"${it.name}\"", color = TextPrimary, fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("Verranno eliminati anche scadenze, appuntamenti e storico. Operazione non reversibile.",
                            color = TextSecondary, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp)
                    }
                },
                confirmButton = {
                    Button(onClick = { showDeleteItemDialog = false; vm.deleteItem(it) { onBack() } },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Elimina definitivamente", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteItemDialog = false },
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
// StatsStrip — strip orizzontale con statistiche item
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StatsStrip(stats: ItemStats) {
    if (stats.totalSpentCents == 0L && stats.completedAppointments == 0) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stats.totalSpentThisYearCents > 0L) {
            StatChip(
                label = "Quest'anno",
                value = "€${"%.0f".format(stats.totalSpentThisYearCents / 100.0)}",
                color = AccentAmber,
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.totalSpentCents > 0L) {
            StatChip(
                label = "Totale speso",
                value = "€${"%.0f".format(stats.totalSpentCents / 100.0)}",
                color = AccentAmberLight,
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.completedAppointments > 0) {
            StatChip(
                label = "Sedute",
                value = "${stats.completedAppointments}",
                color = AccentBlue,
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.avgAppointmentCents != null) {
            StatChip(
                label = "Media seduta",
                value = "€${"%.0f".format(stats.avgAppointmentCents / 100.0)}",
                color = AccentGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}


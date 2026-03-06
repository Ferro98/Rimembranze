package com.example.rimembranze.ui.components

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.data.db.DeadlineEntity
import com.example.rimembranze.ui.MarkAsPaidDialog
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// DeadlineCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeadlineCard(
    deadline: DeadlineEntity,
    isHighlighted: Boolean = false,
    onMarkPaid: (amountCents: Long?) -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var deleteConfirm      by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }

    val daysLeft = ((deadline.dueDateEpochMs - System.currentTimeMillis()) / (1000L * 60 * 60 * 24)).toInt()
    val urgencyColor = when {
        daysLeft < 0   -> DestructiveRed
        daysLeft <= 7  -> DestructiveRed
        daysLeft <= 30 -> AccentAmber
        else           -> AccentAmberLight
    }

    val reminderLabel = remember(deadline.reminderDaysCsv) {
        deadline.reminderDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
            .sortedDescending().joinToString(" · ") { d ->
                when (d) { 0 -> "giorno stesso"; 1 -> "1 giorno prima"; else -> "$d giorni prima" }
            }
    }

    val cardBorder = if (isHighlighted)
        androidx.compose.foundation.BorderStroke(1.5.dp, AccentAmber.copy(alpha = 0.6f)) else null

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = cardBorder,
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(urgencyColor))
                Spacer(Modifier.width(10.dp))
                Text(deadline.category, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, modifier = Modifier.weight(1f))
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
                    valueColor = urgencyColor, modifier = Modifier.weight(1f)
                )
                deadline.lastCostCents?.let { cents ->
                    InfoChip(label = "Importo", value = "€${"%.2f".format(cents / 100.0)}",
                        valueColor = AccentGreen, modifier = Modifier.weight(1f))
                }
            }

            if (!deadline.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(deadline.notes, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            if (reminderLabel.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null,
                        tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(reminderLabel, color = TextSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = deleteConfirm, label = "del_deadline") { confirming ->
                if (confirming) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { deleteConfirm = false }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = ButtonDefaults.outlinedButtonBorder.copy()
                        ) { Text("Annulla", fontSize = 13.sp) }
                        Button(onClick = { onDelete(); deleteConfirm = false }, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DestructiveRed, contentColor = Color.White)
                        ) { Text("Conferma", fontSize = 13.sp) }
                    }
                } else {
                    Button(onClick = { showMarkPaidDialog = true },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentAmber.copy(alpha = 0.15f), contentColor = AccentAmber),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
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
            onConfirm = { cents -> onMarkPaid(cents); showMarkPaidDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DeadlineDialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun DeadlineDialog(
    title: String,
    initialCategory: String = "",
    initialDateEpochMs: Long? = null,
    initialRecurrence: String = "NONE",
    initialNotes: String? = null,
    initialAmountCents: Long? = null,
    initialReminderDaysCsv: String = "14,7",
    onDismiss: () -> Unit,
    onSave: (category: String, dateEpochMs: Long, recurrence: String,
             notes: String?, amountCents: Long?, reminderDaysCsv: String) -> Unit
) {
    var category     by remember { mutableStateOf(initialCategory) }
    var selectedDate by remember { mutableStateOf(initialDateEpochMs) }
    var recurrence   by remember { mutableStateOf(initialRecurrence) }
    var notes        by remember { mutableStateOf(initialNotes ?: "") }
    var amountRaw    by remember {
        mutableStateOf(initialAmountCents?.let { "%.2f".format(it / 100.0) } ?: "")
    }
    var recExpanded  by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(csvToSet(initialReminderDaysCsv)) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = { Text(title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = category, onValueChange = { category = it },
                    label = { Text("Categoria", color = TextSecondary) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())

                OutlinedButton(
                    onClick = {
                        val cal = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            selectedDate = Calendar.getInstance().apply {
                                set(y, m, d, 0, 0, 0); set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedDate != null) AccentAmberLight else TextSecondary)
                ) {
                    Text(selectedDate?.let { "📅  ${formatDate(it)}" } ?: "Seleziona data", fontSize = 14.sp)
                }

                @OptIn(ExperimentalMaterial3Api::class)
                ExposedDropdownMenuBox(expanded = recExpanded,
                    onExpandedChange = { recExpanded = !recExpanded },
                    modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = recurrenceLabel(recurrence), onValueChange = {},
                        readOnly = true, label = { Text("Ricorrenza", color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(recExpanded) },
                        shape = RoundedCornerShape(12.dp), colors = dialogFieldColors(),
                        modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = recExpanded,
                        onDismissRequest = { recExpanded = false },
                        modifier = Modifier.background(SurfaceElevated)) {
                        recurrenceOptions.forEach { (v, l) ->
                            DropdownMenuItem(
                                text = { Text(l, color = if (recurrence == v) AccentAmber else TextPrimary) },
                                onClick = { recurrence = v; recExpanded = false })
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Notificami", color = TextSecondary, fontSize = 11.sp,
                        fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()) {
                        repeat(3) { index ->
                            val option = REMINDER_OPTIONS.getOrNull(index)
                            if (option != null) {
                                val (days, label) = option
                                val selected = days in selectedDays
                                FilterChip(
                                    selected = selected,
                                    onClick = { selectedDays = if (selected) selectedDays - days else selectedDays + days },
                                    label = {
                                        Text(label, fontSize = 11.sp, maxLines = 1,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth(),
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentAmber.copy(alpha = 0.20f),
                                        selectedLabelColor = AccentAmber,
                                        containerColor = SurfaceElevated, labelColor = TextSecondary),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true, selected = selected,
                                        selectedBorderColor = AccentAmber.copy(alpha = 0.5f),
                                        selectedBorderWidth = 1.dp,
                                        borderColor = DividerColor, borderWidth = 0.5.dp)
                                )
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                OutlinedTextField(value = amountRaw,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d{0,7}([.,]\\d{0,2})?"))) amountRaw = it },
                    label = { Text("Importo (opzionale)", color = TextSecondary) },
                    placeholder = { Text("es. 85.00", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text("€", color = if (amountRaw.isNotBlank()) AccentAmberLight else TextSecondary,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    },
                    shape = RoundedCornerShape(12.dp), colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = notes, onValueChange = { notes = it },
                    label = { Text("Note (opzionale)", color = TextSecondary) },
                    minLines = 2, maxLines = 4, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (category.isNotBlank() && selectedDate != null) {
                        val cents = amountRaw.replace(",", ".").toDoubleOrNull()?.let { (it * 100).toLong() }
                        onSave(category.trim(), selectedDate!!, recurrence,
                            notes.trimEnd().ifBlank { null }, cents, setToCsv(selectedDays))
                    }
                },
                enabled = category.isNotBlank() && selectedDate != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber,
                    contentColor = Color(0xFF1A1100),
                    disabledContainerColor = AccentAmber.copy(alpha = 0.3f))
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}
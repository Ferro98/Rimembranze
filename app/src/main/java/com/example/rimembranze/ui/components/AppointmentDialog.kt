package com.example.rimembranze.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.data.db.AppointmentEntity
import java.util.*

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentDialog — nuovo appuntamento con data + orario
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppointmentDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, dateEpochMs: Long, notes: String?, amountCents: Long?) -> Unit
) {
    var title      by remember { mutableStateOf("") }
    var selectedMs by remember { mutableStateOf<Long?>(null) }
    var notes      by remember { mutableStateOf("") }
    var amountRaw  by remember { mutableStateOf("") }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = { Text("Nuovo appuntamento", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Titolo", color = TextSecondary) },
                    singleLine = true, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth()
                )

                OutlinedButton(
                    onClick = {
                        val now = Calendar.getInstance()
                        DatePickerDialog(context, { _, y, m, d ->
                            TimePickerDialog(context, { _, h, min ->
                                selectedMs = Calendar.getInstance().apply {
                                    set(y, m, d, h, min, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }, now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), true).show()
                        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH)).show()
                    },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (selectedMs != null) AccentAmberLight else TextSecondary)
                ) {
                    Text(selectedMs?.let { "📅  ${formatDateTime(it)}" } ?: "Seleziona data e ora",
                        fontSize = 14.sp)
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d{0,7}([.,]\\d{0,2})?"))) amountRaw = it },
                    label = { Text("Importo (opzionale)", color = TextSecondary) },
                    placeholder = { Text("es. 50.00", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text("€", color = if (amountRaw.isNotBlank()) AccentAmberLight else TextSecondary,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    },
                    shape = RoundedCornerShape(12.dp), colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Note (opzionale)", color = TextSecondary) },
                    minLines = 2, maxLines = 4, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedMs != null) {
                        val cents = amountRaw.replace(",", ".").toDoubleOrNull()?.let { (it * 100).toLong() }
                        onSave(title.trim(), selectedMs!!, notes.trimEnd().ifBlank { null }, cents)
                    }
                },
                enabled = title.isNotBlank() && selectedMs != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentBlue, contentColor = Color.White,
                    disabledContainerColor = AccentBlue.copy(alpha = 0.3f))
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentDoneDialog — segna come effettuato
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppointmentDoneDialog(
    appointment: AppointmentEntity,
    onDismiss: () -> Unit,
    onConfirm: (notes: String?, amountCents: Long?) -> Unit
) {
    var notes     by remember { mutableStateOf(appointment.notes ?: "") }
    var amountRaw by remember {
        mutableStateOf(appointment.amountCents?.let { "%.2f".format(it / 100.0) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        icon = { Icon(Icons.Default.Check, contentDescription = null,
            tint = AccentBlue, modifier = Modifier.size(28.dp)) },
        title = { Text("Seduta effettuata", color = TextPrimary,
            fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(appointment.title, color = AccentAmberLight,
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d{0,7}([.,]\\d{0,2})?"))) amountRaw = it },
                    label = { Text("Importo (opzionale)", color = TextSecondary) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text("€", color = if (amountRaw.isNotBlank()) AccentAmberLight else TextSecondary,
                            fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    },
                    shape = RoundedCornerShape(12.dp), colors = dialogFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Note (opzionale)", color = TextSecondary) },
                    minLines = 2, maxLines = 4, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = amountRaw.replace(",", ".").toDoubleOrNull()?.let { (it * 100).toLong() }
                    onConfirm(notes.trimEnd().ifBlank { null }, cents)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, contentColor = Color.White)
            ) { Text("Conferma", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// InvoiceDialog — selezione sedute da fatturare
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun InvoiceDialog(
    appointments: List<AppointmentEntity>,
    onDismiss: () -> Unit,
    onConfirm: (selectedIds: Set<Long>, notes: String?) -> Unit
) {
    var selectedIds by remember { mutableStateOf(appointments.map { it.id }.toSet()) }
    var notes       by remember { mutableStateOf("") }

    val totalCents = appointments
        .filter { it.id in selectedIds }
        .sumOf { it.amountCents ?: 0L }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = { Text("Crea fattura", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text("Seleziona le sedute da includere:", color = TextSecondary, fontSize = 13.sp)

                appointments.forEach { a ->
                    val checked = a.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (checked) AccentGreen.copy(alpha = 0.08f) else SurfaceElevated)
                            .clickable { selectedIds = if (checked) selectedIds - a.id else selectedIds + a.id }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { selectedIds = if (it) selectedIds + a.id else selectedIds - a.id },
                            colors = CheckboxDefaults.colors(
                                checkedColor = AccentGreen, uncheckedColor = TextSecondary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(a.title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(formatDateTime(a.dateEpochMs), color = TextSecondary, fontSize = 12.sp)
                        }
                        a.amountCents?.let { cents ->
                            Text("€${"%.2f".format(cents / 100.0)}", color = AccentGreen,
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (totalCents > 0) {
                    HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Totale (${selectedIds.size} sedute)", color = TextSecondary, fontSize = 13.sp)
                        Text("€${"%.2f".format(totalCents / 100.0)}", color = AccentGreen,
                            fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Note fattura (opzionale)", color = TextSecondary) },
                    minLines = 2, maxLines = 3, shape = RoundedCornerShape(12.dp),
                    colors = dialogFieldColors(), modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedIds, notes.trimEnd().ifBlank { null }) },
                enabled = selectedIds.isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen, contentColor = Color(0xFF0F0F13),
                    disabledContainerColor = AccentGreen.copy(alpha = 0.3f))
            ) { Text("Crea fattura", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}
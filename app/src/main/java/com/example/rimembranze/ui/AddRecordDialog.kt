package com.example.rimembranze.ui

// ════════════════════════════════════════════════════════════════════════════
// FUNZIONALITÀ 1 — Aggiunta Record manuale
//
// 1. Aggiungi questo composable al file ItemDetailScreen.kt
// 2. Aggiorna il FAB per mostrare due opzioni (scadenza / record)
// 3. Chiama vm.addRecordAndReturnId(...) al salvataggio
// ════════════════════════════════════════════════════════════════════════════

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.data.db.RecordType
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordDialog(
    onDismiss: () -> Unit,
    onSave: (
        type: RecordType,
        title: String,
        dateEpochMs: Long,
        amountCents: Long?,
        notes: String?
    ) -> Unit
) {
    var selectedType  by remember { mutableStateOf(RecordType.Pagamento) }
    var title         by remember { mutableStateOf("") }
    var selectedDate  by remember { mutableStateOf<Long?>(null) }
    var amountRaw     by remember { mutableStateOf("") }
    var notes         by remember { mutableStateOf("") }
    var typeExpanded  by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Colori inline (stessi del resto dell'app)
    val SurfaceDark     = Color(0xFF1A1A22)
    val SurfaceElevated = Color(0xFF23232E)
    val AccentAmber     = Color(0xFFE8A020)
    val AccentAmberLight= Color(0xFFFFCA6A)
    val TextPrimary     = Color(0xFFF0EEE8)
    val TextSecondary   = Color(0xFF8A8898)
    val DividerColor    = Color(0xFF2C2C3A)

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = AccentAmber,
        unfocusedBorderColor = DividerColor,
        focusedTextColor     = TextPrimary,
        unfocusedTextColor   = TextPrimary,
        cursorColor          = AccentAmber,
        focusedLabelColor    = AccentAmber
    )

    // Icona e colore per tipo record
    fun typeColor(t: RecordType) = when (t) {
        RecordType.Pagamento -> Color(0xFFE8A020)
        RecordType.Visita    -> Color(0xFF5BEF9A)
        RecordType.Altro     -> Color(0xFFBF5BEF)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = {
            Text("Nuovo record", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                // ── Tipo ─────────────────────────────────────────────────────
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo", color = TextSecondary) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(
                                        typeColor(selectedType),
                                        androidx.compose.foundation.shape.CircleShape
                                    )
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = fieldColors,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false },
                        modifier = Modifier.background(SurfaceElevated)
                    ) {
                        RecordType.entries.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .background(
                                                    typeColor(t),
                                                    androidx.compose.foundation.shape.CircleShape
                                                )
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(t.name,
                                            color = if (selectedType == t) AccentAmber else TextPrimary)
                                    }
                                },
                                onClick = { selectedType = t; typeExpanded = false }
                            )
                        }
                    }
                }

                // ── Titolo ───────────────────────────────────────────────────
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo", color = TextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
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
                        selectedDate?.let {
                            "📅  ${SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(it))}"
                        } ?: "Seleziona data",
                        fontSize = 14.sp
                    )
                }

                Divider(color = DividerColor, thickness = 0.5.dp)

                // ── Importo opzionale ─────────────────────────────────────────
                OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { raw ->
                        if (raw.isEmpty() || raw.matches(Regex("\\d{0,7}([.,]\\d{0,2})?")))
                            amountRaw = raw
                    },
                    label = { Text("Importo (opzionale)", color = TextSecondary) },
                    placeholder = { Text("es. 45.00", color = TextSecondary.copy(alpha = 0.5f)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    leadingIcon = {
                        Text("€",
                            color = if (amountRaw.isNotBlank()) AccentAmberLight else TextSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp))
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Note opzionale ────────────────────────────────────────────
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Note (opzionale)", color = TextSecondary) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedDate != null) {
                        val cents = amountRaw.replace(",", ".")
                            .toDoubleOrNull()?.let { (it * 100).toLong() }
                        onSave(
                            selectedType,
                            title.trim(),
                            selectedDate!!,
                            cents,
                            notes.trimEnd().ifBlank { null }
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedDate != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor = Color(0xFF1A1100),
                    disabledContainerColor = AccentAmber.copy(alpha = 0.3f)
                )
            ) { Text("Salva", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text("Annulla") }
        }
    )
}
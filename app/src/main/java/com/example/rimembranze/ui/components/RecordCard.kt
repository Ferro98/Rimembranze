package com.example.rimembranze.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.data.db.RecordEntity
import com.example.rimembranze.data.db.RecordType

@Composable
fun RecordCard(
    record: RecordEntity,
    onDelete: () -> Unit,
    onUpdateUniSalute: (sent: Boolean, status: String?, sentEpochMs: Long?) -> Unit
) {
    var deleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(
                    when (record.type) {
                        RecordType.Pagamento.name -> AccentAmber
                        RecordType.Visita.name    -> AccentGreen
                        else                      -> Color(0xFFBF5BEF)
                    }))
                Spacer(Modifier.width(10.dp))
                Text(record.title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { deleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = if (deleteConfirm) DestructiveRed else TextSecondary,
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = record.type, value = formatDate(record.dateEpochMs),
                    valueColor = AccentAmberLight, modifier = Modifier.weight(1f))
                record.amountCents?.let { cents ->
                    InfoChip(label = "Importo", value = "€${"%.2f".format(cents / 100.0)}",
                        valueColor = AccentGreen, modifier = Modifier.weight(1f))
                }
            }

            if (!record.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(record.notes, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            // ── Conferma eliminazione ─────────────────────────────────────
            AnimatedVisibility(visible = deleteConfirm) {
                Column {
                    Spacer(Modifier.height(12.dp))
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
                }
            }

            if (!deleteConfirm &&
                (record.type == RecordType.Visita.name || record.type == RecordType.Pagamento.name)) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(thickness = 0.5.dp, color = DividerColor)
                Spacer(Modifier.height(10.dp))

                var sent           by remember { mutableStateOf(record.unisaluteSent) }
                var status         by remember { mutableStateOf(record.unisaluteStatus ?: "") }
                var statusExpanded by remember { mutableStateOf(false) }

                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00AEEF)))
                    Spacer(Modifier.width(8.dp))
                    Text("Rimborso UniSalute", color = TextSecondary, fontSize = 12.sp,
                        fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Switch(
                        checked = sent,
                        onCheckedChange = { checked ->
                            sent = checked
                            val newStatus  = if (checked) "InAttesa" else null
                            val newEpochMs = if (checked) System.currentTimeMillis() else null
                            status = newStatus ?: ""
                            onUpdateUniSalute(checked, newStatus, newEpochMs)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF00AEEF))
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
                        ExposedDropdownMenuBox(expanded = statusExpanded,
                            onExpandedChange = { statusExpanded = !statusExpanded },
                            modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = statusOptions.firstOrNull { it.first == status }?.second ?: "In attesa",
                                onValueChange = {}, readOnly = true,
                                label = { Text("Stato rimborso", color = TextSecondary, fontSize = 12.sp) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(statusExpanded) },
                                leadingIcon = {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statusColor))
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = statusColor, unfocusedBorderColor = DividerColor,
                                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                                    focusedLabelColor = statusColor),
                                modifier = Modifier.menuAnchor().fillMaxWidth())
                            ExposedDropdownMenu(expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false },
                                modifier = Modifier.background(SurfaceElevated)) {
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
                                            status = value; statusExpanded = false
                                            onUpdateUniSalute(true, value, record.unisaluteSentEpochMs)
                                        }
                                    )
                                }
                            }
                        }

                        record.unisaluteSentEpochMs?.let { ms ->
                            Spacer(Modifier.height(4.dp))
                            Text("Inviato il ${formatDate(ms)}", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
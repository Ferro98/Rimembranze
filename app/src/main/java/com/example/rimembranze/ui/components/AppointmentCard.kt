package com.example.rimembranze.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.data.db.AppointmentEntity

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentCard — appuntamento futuro
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppointmentCard(
    appointment: AppointmentEntity,
    onMarkDone: (notes: String?, amountCents: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var showDoneDialog by remember { mutableStateOf(false) }
    var deleteConfirm  by remember { mutableStateOf(false) }

    val msLeft    = appointment.dateEpochMs - System.currentTimeMillis()
    val hoursLeft = msLeft / (1000L * 60 * 60)
    val daysLeft  = msLeft / (1000L * 60 * 60 * 24)

    val timeLabel = when {
        msLeft < 0     -> "Passato"
        hoursLeft < 24 -> "Tra ${hoursLeft}h"
        daysLeft == 1L -> "Domani"
        else           -> "Tra ${daysLeft}g"
    }
    val urgencyColor = when {
        msLeft < 0     -> DestructiveRed
        hoursLeft < 24 -> DestructiveRed
        daysLeft <= 3  -> AccentAmber
        else           -> AccentBlue
    }
    val isUrgent = msLeft in 0..(24 * 60 * 60 * 1000L)

    // Pulse solo per appuntamenti urgenti (< 24h)
    val dotAlpha by animateFloatAsState(
        targetValue = if (isUrgent) 0.3f else 1f,
        animationSpec = if (isUrgent) infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ) else snap(),
        label = "dot_pulse"
    )

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(urgencyColor.copy(alpha = dotAlpha)))
                Spacer(Modifier.width(10.dp))
                Text(appointment.title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = { deleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = null,
                        tint = if (deleteConfirm) DestructiveRed else TextSecondary,
                        modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = "Data", value = formatDateTime(appointment.dateEpochMs),
                    valueColor = urgencyColor, modifier = Modifier.weight(1f))
                InfoChip(label = "Manca", value = timeLabel,
                    valueColor = urgencyColor, modifier = Modifier.weight(1f))
            }

            if (!appointment.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(appointment.notes, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            AnimatedContent(targetState = deleteConfirm, label = "del_appt") { confirming ->
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
                    Button(
                        onClick = { showDoneDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentBlue.copy(alpha = 0.15f), contentColor = AccentBlue),
                        elevation = ButtonDefaults.buttonElevation(0.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Segna come effettuato", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showDoneDialog) {
        AppointmentDoneDialog(
            appointment = appointment,
            onDismiss   = { showDoneDialog = false },
            onConfirm   = { notes, cents -> onMarkDone(notes, cents); showDoneDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AppointmentDoneCard — seduta effettuata (da fatturare o storico)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppointmentDoneCard(
    appointment: AppointmentEntity,
    showPaidBadge: Boolean = false,
    onDelete: (() -> Unit)? = null
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
                Box(modifier = Modifier.size(8.dp).clip(CircleShape)
                    .background(if (showPaidBadge) AccentGreen else AccentBlue))
                Spacer(Modifier.width(10.dp))
                Text(appointment.title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (showPaidBadge) {
                    Box(modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(AccentGreen.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text("Fatturata", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                if (onDelete != null) {
                    IconButton(onClick = { deleteConfirm = true }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null,
                            tint = if (deleteConfirm) DestructiveRed else TextSecondary,
                            modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                InfoChip(label = "Data", value = formatDateTime(appointment.dateEpochMs),
                    valueColor = AccentAmberLight, modifier = Modifier.weight(1f))
                appointment.amountCents?.let { cents ->
                    InfoChip(label = "Importo", value = "€${"%.2f".format(cents / 100.0)}",
                        valueColor = AccentGreen, modifier = Modifier.weight(1f))
                }
            }

            if (!appointment.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(SurfaceElevated).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(appointment.notes, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }

            if (onDelete != null && deleteConfirm) {
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
    }
}
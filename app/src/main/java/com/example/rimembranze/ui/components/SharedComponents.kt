package com.example.rimembranze.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

// ── Palette ───────────────────────────────────────────────────────────────────
val BackgroundDark   = Color(0xFF0F0F13)
val SurfaceDark      = Color(0xFF1A1A22)
val SurfaceElevated  = Color(0xFF23232E)
val AccentAmber      = Color(0xFFE8A020)
val AccentAmberLight = Color(0xFFFFCA6A)
val AccentGreen      = Color(0xFF5BEF9A)
val AccentBlue       = Color(0xFF5B8DEF)
val TextPrimary      = Color(0xFFF0EEE8)
val TextSecondary    = Color(0xFF8A8898)
val DividerColor     = Color(0xFF2C2C3A)
val DestructiveRed   = Color(0xFFE05858)

// ── Reminder options ──────────────────────────────────────────────────────────
val REMINDER_OPTIONS = listOf(
    14 to "14 giorni",
    7  to "7 giorni",
    0  to "Giorno stesso"
)

fun csvToSet(csv: String): Set<Int> =
    csv.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()

fun setToCsv(set: Set<Int>): String =
    set.sortedDescending().joinToString(",")

// ── Date formatting ───────────────────────────────────────────────────────────
fun formatDate(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))

fun formatDateTime(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY).format(Date(epochMs))

// Data e ora separate — usato negli AppointmentInfoChip
fun formatDateOnly(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))

fun formatTimeOnly(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.ITALY).format(Date(epochMs))

// ── Recurrence ────────────────────────────────────────────────────────────────
fun recurrenceLabel(value: String): String = when (value) {
    com.example.rimembranze.data.Recurrence.NONE       -> "Nessuna"
    com.example.rimembranze.data.Recurrence.MONTHLY    -> "Mensile"
    com.example.rimembranze.data.Recurrence.QUARTERLY  -> "Trimestrale"
    com.example.rimembranze.data.Recurrence.SEMIANNUAL -> "Semestrale"
    com.example.rimembranze.data.Recurrence.YEARLY     -> "Annuale"
    else -> "Nessuna"
}

val recurrenceOptions = listOf(
    com.example.rimembranze.data.Recurrence.NONE       to "Nessuna",
    com.example.rimembranze.data.Recurrence.MONTHLY    to "Mensile",
    com.example.rimembranze.data.Recurrence.QUARTERLY  to "Trimestrale",
    com.example.rimembranze.data.Recurrence.SEMIANNUAL to "Semestrale",
    com.example.rimembranze.data.Recurrence.YEARLY     to "Annuale"
)

// ── TabCard ───────────────────────────────────────────────────────────────────
@Composable
fun TabCard(
    label: String,
    count: Int,
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor     = if (active) color.copy(alpha = 0.12f) else SurfaceElevated
    val borderColor = if (active) color.copy(alpha = 0.5f) else Color.Transparent

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                color = if (active) color else TextSecondary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 28.sp
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                color = if (active) color else TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── FabOption ─────────────────────────────────────────────────────────────────
@Composable
fun FabOption(
    label: String,
    color: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label, color = TextPrimary, fontSize = 13.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceElevated)
                .padding(horizontal = 10.dp, vertical = 6.dp)
        )
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = SurfaceElevated,
            contentColor = color
        ) { Icon(icon, contentDescription = null) }
    }
}

// ── SectionHeader ─────────────────────────────────────────────────────────────
@Composable
fun SectionHeader(title: String, count: Int, accentColor: Color = AccentAmber) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title, color = TextSecondary, fontSize = 11.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 2.sp,
            modifier = Modifier.weight(1f)
        )
        Text("$count", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

// ── EmptyState animata ────────────────────────────────────────────────────────
@Composable
fun EmptyState(message: String, icon: String = "○") {
    // Scala in con bounce all'ingresso
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow
        ),
        label = "empty_scale"
    )
    // Respiro lento continuo sull'icona
    val breathAlpha by animateFloatAsState(
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_breath"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 28.dp)
            .scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text  = icon,
                color = TextSecondary.copy(alpha = breathAlpha),
                fontSize = 36.sp
            )
            Text(
                text  = message,
                color = TextSecondary,
                fontSize = 14.sp
            )
        }
    }
}

// ── InfoChip ──────────────────────────────────────────────────────────────────
@Composable
fun InfoChip(
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

// ── AppointmentInfoChip ───────────────────────────────────────────────────────
// Chip con data su riga 1 e ora su riga 2 — altezza uniforme tramite fillMaxHeight
@Composable
fun AppointmentInfoChip(
    epochMs: Long,
    valueColor: Color = AccentAmberLight,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Data", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(formatDateOnly(epochMs), color = valueColor,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(formatTimeOnly(epochMs), color = valueColor,
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── DialogFieldColors ─────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentAmber,
    unfocusedBorderColor = DividerColor,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AccentAmber,
    focusedLabelColor    = AccentAmber
)
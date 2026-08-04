package com.example.rimembranze.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.R
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

// ── Reminder options — 4 chip: 14g / 7g / 1g / giorno stesso ─────────────────
@Composable
fun reminderOptions(): List<Pair<Int, String>> = listOf(
    14 to stringResource(R.string.reminder_14_days),
    7  to stringResource(R.string.reminder_7_days),
    1  to stringResource(R.string.reminder_1_day),
    0  to stringResource(R.string.reminder_same_day)
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

fun formatDateOnly(epochMs: Long): String =
    SimpleDateFormat("dd/MM/yyyy", Locale.ITALY).format(Date(epochMs))

fun formatTimeOnly(epochMs: Long): String =
    SimpleDateFormat("HH:mm", Locale.ITALY).format(Date(epochMs))

// ── Recurrence ────────────────────────────────────────────────────────────────
@Composable
fun recurrenceLabel(value: String): String = when (value) {
    com.example.rimembranze.data.Recurrence.MONTHLY    -> stringResource(R.string.recurrence_monthly)
    com.example.rimembranze.data.Recurrence.QUARTERLY  -> stringResource(R.string.recurrence_quarterly)
    com.example.rimembranze.data.Recurrence.SEMIANNUAL -> stringResource(R.string.recurrence_semiannual)
    com.example.rimembranze.data.Recurrence.YEARLY     -> stringResource(R.string.recurrence_yearly)
    else -> stringResource(R.string.recurrence_none)
}

@Composable
fun recurrenceOptions(): List<Pair<String, String>> = listOf(
    com.example.rimembranze.data.Recurrence.NONE       to stringResource(R.string.recurrence_none),
    com.example.rimembranze.data.Recurrence.MONTHLY    to stringResource(R.string.recurrence_monthly),
    com.example.rimembranze.data.Recurrence.QUARTERLY  to stringResource(R.string.recurrence_quarterly),
    com.example.rimembranze.data.Recurrence.SEMIANNUAL to stringResource(R.string.recurrence_semiannual),
    com.example.rimembranze.data.Recurrence.YEARLY     to stringResource(R.string.recurrence_yearly)
)

// ── TabCard con contatore animato ─────────────────────────────────────────────
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
            AnimatedContent(
                targetState = count,
                transitionSpec = {
                    val up = targetState > initialState
                    slideInVertically { if (up) -it else it } togetherWith
                            slideOutVertically { if (up) it else -it }
                },
                label = "tab_count_$label"
            ) { c ->
                Text(
                    text = "$c",
                    color = if (active) color else TextSecondary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
            }
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
fun SectionHeader(
    title: String,
    count: Int,
    accentColor: Color = AccentAmber,
    trailing: (@Composable () -> Unit)? = null
) {
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
        trailing?.invoke()
    }
    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
}

// ── EmptyState animata ────────────────────────────────────────────────────────
@Composable
fun EmptyState(message: String, icon: String = "○") {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "empty_scale"
    )
    val breathAlpha by animateFloatAsState(
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Reverse),
        label = "empty_breath"
    )
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 36.dp, horizontal = 28.dp).scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(icon, color = TextSecondary.copy(alpha = breathAlpha), fontSize = 36.sp)
            Text(message, color = TextSecondary, fontSize = 14.sp)
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
            .fillMaxHeight()                          // ← stessa altezza del chip più alto
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center      // ← centra verticalmente
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── AppointmentInfoChip ───────────────────────────────────────────────────────
// Chip con data su riga 1 e ora su riga 2
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
        Text(stringResource(R.string.shared_date_label), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(formatDateOnly(epochMs), color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        Text(formatTimeOnly(epochMs), color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

// ── MancaInfoChip ────────────────────────────────────────────────────────────
// Chip "Manca" su due righe (numero grande + unità) per stessa altezza di AppointmentInfoChip
@Composable
fun MancaInfoChip(
    timeLabel: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    // Separa numero e unità: "Tra 5g" → "Tra 5" + "giorni", "Tra 3h" → "Tra 3" + "ore", ecc.
    val (main, sub) = when {
        timeLabel == "Passato"  -> "—" to "passato"
        timeLabel == "Domani"   -> "Dom" to "ani"
        timeLabel.endsWith("h") -> timeLabel.dropLast(1) to "ore"
        timeLabel.endsWith("g") -> timeLabel.dropLast(1) to "giorni"
        else                    -> timeLabel to ""
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceElevated)
            .fillMaxHeight()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(R.string.shared_manca_label), color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(2.dp))
        Text(main, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        if (sub.isNotEmpty()) Text(sub, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
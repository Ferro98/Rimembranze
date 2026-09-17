package com.example.rimembranze.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.R
import com.example.rimembranze.ui.vm.HomeDashboardStats

/**
 * Riepilogo home: spesa di questo mese e stima dei prossimi 30 giorni, aggregati su tutti gli
 * item. A differenza degli altri chip dell'app (colori fissi in [AccentAmber]/[AccentBlue] ecc.),
 * questo usa `MaterialTheme.colorScheme` — che nel tema di Rimembranze è comunque costruito sugli
 * stessi valori (`primary` = ambra, `secondary` = blu, vedi `ui/theme/Theme.kt`) — così un domani
 * un cambio di palette centralizzato nel tema si riflette qui senza toccare questo file.
 */
@Composable
fun HomeSummaryCard(stats: HomeDashboardStats, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (stats.spentThisMonthCents > 0L) {
            HomeStatChip(
                label = stringResource(R.string.main_dashboard_spent_this_month),
                valueCents = stats.spentThisMonthCents,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
        if (stats.upcomingEstimatedCents > 0L) {
            HomeStatChip(
                label = stringResource(R.string.main_dashboard_upcoming_estimate),
                valueCents = stats.upcomingEstimatedCents,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HomeStatChip(label: String, valueCents: Long, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Crossfade quando il valore cambia (nuovo pagamento registrato, mese che scorre, ecc.)
        // invece del numero che salta di colpo.
        AnimatedContent(
            targetState = valueCents,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "home_stat_value"
        ) { cents ->
            Text("€${"%.0f".format(cents / 100.0)}", color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, color = color.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

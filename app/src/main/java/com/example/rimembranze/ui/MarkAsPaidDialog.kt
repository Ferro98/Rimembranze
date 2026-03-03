package com.example.rimembranze.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SurfaceDark    = Color(0xFF1A1A22)
private val SurfaceElevated= Color(0xFF23232E)
private val AccentAmber    = Color(0xFFE8A020)
private val AccentAmberLight = Color(0xFFFFCA6A)
private val TextPrimary    = Color(0xFFF0EEE8)
private val TextSecondary  = Color(0xFF8A8898)
private val DividerColor   = Color(0xFF2C2C3A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsPaidDialog(
    prefilledCents: Long?,          // lastCostCents della deadline, pre-compila il campo
    onDismiss: () -> Unit,
    onConfirm: (amountCents: Long?) -> Unit
) {
    var amountRaw by remember {
        mutableStateOf(prefilledCents?.let { "%.2f".format(it / 100.0) } ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = SurfaceDark,
        tonalElevation = 0.dp,
        title = {
            Text(
                "Conferma pagamento",
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Inserisci l'importo pagato (opzionale).",
                    color = TextSecondary,
                    fontSize = 14.sp
                )

                OutlinedTextField(
                    value = amountRaw,
                    onValueChange = { raw ->
                        if (raw.isEmpty() || raw.matches(Regex("\\d{0,7}([.,]\\d{0,2})?"))) {
                            amountRaw = raw
                        }
                    },
                    label = { Text("Importo (€)", color = TextSecondary) },
                    placeholder = {
                        Text(
                            prefilledCents?.let { "%.2f".format(it / 100.0) } ?: "es. 85.00",
                            color = TextSecondary.copy(alpha = 0.5f)
                        )
                    },
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = AccentAmber,
                        unfocusedBorderColor = DividerColor,
                        focusedTextColor     = TextPrimary,
                        unfocusedTextColor   = TextPrimary,
                        cursorColor          = AccentAmber,
                        focusedLabelColor    = AccentAmber
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cents = amountRaw
                        .replace(",", ".")
                        .toDoubleOrNull()
                        ?.let { (it * 100).toLong() }
                    onConfirm(cents)
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentAmber,
                    contentColor   = Color(0xFF1A1100)
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null,
                    modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Segna pagata", fontWeight = FontWeight.Bold)
            }
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
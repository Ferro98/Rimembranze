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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rimembranze.R
import com.example.rimembranze.ui.components.AccentAmber
import com.example.rimembranze.ui.components.AccentAmberLight
import com.example.rimembranze.ui.components.DividerColor
import com.example.rimembranze.ui.components.SurfaceDark
import com.example.rimembranze.ui.components.TextPrimary
import com.example.rimembranze.ui.components.TextSecondary

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
                stringResource(R.string.mark_paid_title),
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    stringResource(R.string.mark_paid_body),
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
                    label = { Text(stringResource(R.string.field_amount_label), color = TextSecondary) },
                    placeholder = {
                        Text(
                            prefilledCents?.let { "%.2f".format(it / 100.0) } ?: stringResource(R.string.mark_paid_amount_placeholder),
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
                Text(stringResource(R.string.mark_paid_confirm), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}
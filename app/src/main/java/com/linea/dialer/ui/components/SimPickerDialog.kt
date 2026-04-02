package com.linea.dialer.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.linea.dialer.ui.viewmodel.SimSlot
import com.linea.dialer.ui.theme.*

/**
 * In-app SIM selection dialog — replaces the system's SIM picker entirely.
 * Shown by DialpadScreen when the device has multiple active SIMs.
 */
@Composable
fun SimPickerDialog(
    number: String,
    sims: List<SimSlot>,
    onSimSelected: (SimSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor  = MaterialTheme.colorScheme.surface,
        shape           = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(
                    "Choose SIM",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "Calling $number",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sims.forEachIndexed { index, sim ->
                    SimRow(
                        sim     = sim,
                        index   = index,
                        onClick = { onSimSelected(sim) }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun SimRow(sim: SimSlot, index: Int, onClick: () -> Unit) {
    // Alternate gradient direction per SIM for visual distinction
    val accentColor = if (index == 0) GradientStart else GradientEnd

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
                0.5.dp,
                accentColor.copy(alpha = 0.3f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SIM icon badge
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(0.5.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.SimCard,
                contentDescription = null,
                tint               = accentColor,
                modifier           = Modifier.size(22.dp)
            )
        }

        // SIM name + slot
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = sim.displayName,
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text  = "Slot ${sim.slotIndex + 1}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Call icon
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (index == 0) listOf(GradientStart, GradientEnd)
                        else listOf(GradientEnd, GradientStart)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Rounded.Call,
                contentDescription = "Call with ${sim.displayName}",
                tint               = Color.White,
                modifier           = Modifier.size(16.dp)
            )
        }
    }
}

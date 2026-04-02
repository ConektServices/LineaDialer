package com.linea.dialer.ui.screens

import android.Manifest
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.google.accompanist.permissions.*
import com.linea.dialer.ui.components.GradientButton
import com.linea.dialer.ui.theme.*

private data class PermItem(
    val permission: String,
    val icon: ImageVector,
    val title: String,
    val reason: String,
)

private val requiredPermissions = listOf(
    PermItem(Manifest.permission.READ_CALL_LOG,  Icons.Rounded.History,       "Call Log",    "View and organise your recent calls"),
    PermItem(Manifest.permission.READ_CONTACTS,  Icons.Rounded.Contacts,      "Contacts",    "Show names and enable smart tagging"),
    PermItem(Manifest.permission.CALL_PHONE,     Icons.Rounded.Call,          "Place Calls", "Dial numbers directly from the app"),
    PermItem(Manifest.permission.READ_PHONE_STATE, Icons.Rounded.PhoneAndroid, "Phone State", "Detect active calls and manage them"),
    PermItem("android.permission.POST_NOTIFICATIONS", Icons.Rounded.Notifications, "Notifications", "Receive follow-up reminder alerts"),
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(onAllGranted: () -> Unit) {

    val multiplePermissionsState = rememberMultiplePermissionsState(
        permissions = requiredPermissions.map { it.permission }
    )

    LaunchedEffect(multiplePermissionsState.allPermissionsGranted) {
        if (multiplePermissionsState.allPermissionsGranted) onAllGranted()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        // Background glow
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(360.dp)
            .align(Alignment.TopCenter)
            .background(
                Brush.radialGradient(
                    colors = listOf(GradientStart.copy(alpha = 0.06f), Color.Transparent),
                    radius = 600f
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text       = "A few permissions\nbefore we begin.",
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Linea needs these to work as your\ndefault dialer.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(Modifier.height(36.dp))

            // Permission items
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                requiredPermissions.forEachIndexed { index, item ->
                    val granted = multiplePermissionsState.permissions.getOrNull(index)?.status?.isGranted == true

                    PermissionRow(
                        item    = item,
                        granted = granted
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // Grant button
            GradientButton(
                text    = if (multiplePermissionsState.allPermissionsGranted) "Continue" else "Grant Permissions",
                onClick = {
                    if (multiplePermissionsState.allPermissionsGranted) onAllGranted()
                    else multiplePermissionsState.launchMultiplePermissionRequest()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // Settings nudge if permanently denied
            AnimatedVisibility(
                visible = multiplePermissionsState.permissions.any {
                    it.status is PermissionStatus.Denied && !it.status.shouldShowRationale
                }
            ) {
                Text(
                    text  = "Some permissions were denied. Open Settings to grant them manually.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionRow(item: PermItem, granted: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                if (granted) CallIncoming.copy(0.3f) else MaterialTheme.colorScheme.outline.copy(0.15f),
                RoundedCornerShape(16.dp)
            )
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Icon box
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (granted) CallIncoming.copy(0.12f)
                    else GradientStart.copy(0.10f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = item.icon,
                contentDescription = null,
                tint               = if (granted) CallIncoming else GradientStart,
                modifier           = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(item.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        AnimatedContent(targetState = granted, label = "perm_check") { g ->
            if (g) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Granted", tint = CallIncoming, modifier = Modifier.size(22.dp))
            } else {
                Icon(Icons.Rounded.RadioButtonUnchecked, contentDescription = "Required", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(22.dp))
            }
        }
    }
}

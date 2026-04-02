package com.linea.dialer.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.*
import com.linea.dialer.data.model.*
import com.linea.dialer.data.repository.formatCallDate
import com.linea.dialer.ui.components.*
import com.linea.dialer.ui.viewmodel.LogError
import com.linea.dialer.ui.viewmodel.LogFilter
import com.linea.dialer.ui.viewmodel.RecentsViewModel
import com.linea.dialer.ui.theme.*

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RecentsScreen(
    onCallNumber: (String) -> Unit,
    onViewContact: (Long) -> Unit,
    vm: RecentsViewModel = viewModel(),
) {
    val context  = LocalContext.current
    val uiState  by vm.uiState.collectAsState()
    val search   by vm.search.collectAsState()
    val filter   by vm.filter.collectAsState()

    // Track READ_CALL_LOG permission state
    val callLogPermission = rememberPermissionState(Manifest.permission.READ_CALL_LOG) { granted ->
        if (granted) vm.loadLogs()
    }

    // Reload when permission is granted
    LaunchedEffect(callLogPermission.status) {
        if (callLogPermission.status.isGranted) vm.loadLogs()
    }

    val grouped = remember(uiState.logs) {
        uiState.logs.groupBy { log ->
            when {
                formatCallDate(log.dateMs).startsWith("Today")     -> "TODAY"
                formatCallDate(log.dateMs).startsWith("Yesterday") -> "YESTERDAY"
                else -> "EARLIER"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Recents",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(onClick = { vm.loadLogs() }) {
                Icon(Icons.Rounded.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.18f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            TextField(
                value = search, onValueChange = vm::onSearch,
                placeholder = { Text("Search call logs...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                modifier = Modifier.weight(1f), singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))).clickable {},
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Mic, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.height(10.dp))

        // Filter chips
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(LogFilter.entries) { f ->
                val sel = filter == f
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                        .then(
                            if (sel) Modifier.background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                            else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f), RoundedCornerShape(24.dp))
                        )
                        .clickable { vm.onFilter(f) }
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        f.label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (sel) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // ── Content ────────────────────────────────────────────────────────────
        when {

            // Permission not granted — show actionable card
            !callLogPermission.status.isGranted -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(72.dp).clip(CircleShape)
                                .background(GradientStart.copy(0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.History, null, tint = GradientStart, modifier = Modifier.size(32.dp))
                        }
                        Text(
                            "Call log permission needed",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            "Linea needs access to your call log to show recent calls here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        // Grant button
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                                .clickable {
                                    if (callLogPermission.status.shouldShowRationale) {
                                        callLogPermission.launchPermissionRequest()
                                    } else {
                                        // Permanently denied — open app settings
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.fromParts("package", context.packageName, null))
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (callLogPermission.status.shouldShowRationale) "Grant Permission" else "Open Settings",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
            }

            // Error — distinguish permission denied vs other
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Rounded.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            uiState.error ?: "Couldn't load call log",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        if (uiState.errorType == LogError.PERMISSION_DENIED) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                                    .clickable {
                                        context.startActivity(
                                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                                .setData(Uri.fromParts("package", context.packageName, null))
                                        )
                                    }
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Text("Open App Settings", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        } else {
                            TextButton(onClick = { vm.loadLogs() }) {
                                Text("Retry", color = GradientStart)
                            }
                        }
                    }
                }
            }

            uiState.logs.isEmpty() -> {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("No calls found", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }

            else -> {
                LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), modifier = Modifier.fillMaxSize()) {
                    listOf("TODAY", "YESTERDAY", "EARLIER").forEach { group ->
                        val items = grouped[group]
                        if (!items.isNullOrEmpty()) {
                            item { SectionLabel(group) }
                            items(items, key = { it.id }) { log ->
                                RealCallLogRow(
                                    log    = log,
                                    onCall = { onCallNumber(log.number) },
                                    onView = { log.contact?.let { onViewContact(it.id) } }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RealCallLogRow(log: RealCallLog, onCall: () -> Unit, onView: () -> Unit) {
    val tag       = log.contact?.tag ?: ContactTag.UNKNOWN
    val nameColor = if (log.type == CallType.MISSED) CallMissed else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContactAvatar(initials = log.contact?.initials ?: "?", tag = tag, size = 48.dp)

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    log.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = nameColor,
                    maxLines = 1
                )
                if (log.contact != null) TagChip(tag = tag, small = true)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                CallTypeIndicator(type = log.type)
                Text(formatCallDate(log.dateMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                log.durationFormatted?.let { dur ->
                    Text("·", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                    Text(dur, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (log.simLabel.isNotEmpty()) {
                    Text("·", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodySmall)
                    Text(log.simLabel, style = MaterialTheme.typography.bodySmall, color = GradientStart)
                }
            }
        }

        IconButton(onClick = onCall, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Call, "Call back", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        }
    }
}

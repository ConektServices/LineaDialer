package com.linea.dialer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.linea.dialer.data.model.*
import com.linea.dialer.telecom.CallHelper
import com.linea.dialer.telecom.CallManager
import com.linea.dialer.ui.components.*
import com.linea.dialer.ui.theme.*

/**
 * Active call screen.
 *
 * This screen handles TWO entry paths:
 *
 * 1. From DialpadScreen — vm.dial() already called placeCall() before navigating here.
 *    CallManager.callState will be Calling/Connected shortly after arrival.
 *
 * 2. From ContactsScreen / RecentsScreen — only navigation happened; no call was placed yet.
 *    In this case callState is still Idle when we arrive. The LaunchedEffect below
 *    detects that and calls CallHelper.placeCall() immediately.
 *
 * Both paths converge here and the UI reacts to CallManager.callState in both cases.
 */
@Composable
fun ActiveCallScreen(
    number: String,
    onEndCall: () -> Unit,
) {
    val context   = LocalContext.current
    val callState by CallManager.callState.collectAsState()

    // ── Place call if we arrived here without dialling first ─────────────────
    // This handles tapping "call" from ContactsScreen or RecentsScreen, where
    // navigation happens but TelecomManager.placeCall() was never invoked.
    var callInitiated by remember { mutableStateOf(false) }
    LaunchedEffect(number) {
        if (number.isNotEmpty() && !callInitiated) {
            callInitiated = true
            if (callState is ActiveCallState.Idle) {
                CallHelper.placeCall(context = context, number = number)
            }
        }
    }

    // ── Derive display values from callState ─────────────────────────────────
    val contact = when (val s = callState) {
        is ActiveCallState.Calling   -> s.contact
        is ActiveCallState.Connected -> s.contact
        is ActiveCallState.OnHold    -> s.contact
        is ActiveCallState.Ended     -> s.contact
        else -> null
    }
    val displayNumber = when (val s = callState) {
        is ActiveCallState.Calling   -> s.number.ifEmpty { number }
        is ActiveCallState.Connected -> s.number.ifEmpty { number }
        is ActiveCallState.OnHold    -> s.number.ifEmpty { number }
        else -> number
    }

    val tag      = contact?.tag ?: ContactTag.UNKNOWN
    val tagColor = tag.color()

    // ── Local UI state ────────────────────────────────────────────────────────
    var muted   by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    var showPad by remember { mutableStateOf(false) }
    var elapsed by remember { mutableLongStateOf(0L) }

    var showNoteSheet     by remember { mutableStateOf(false) }
    var endedContact      by remember { mutableStateOf<RealContact?>(null) }
    var endedDurationSecs by remember { mutableLongStateOf(0L) }
    var endedTimestampMs  by remember { mutableLongStateOf(0L) }

    // ── Call timer ────────────────────────────────────────────────────────────
    val startMs = (callState as? ActiveCallState.Connected)?.startMs
    LaunchedEffect(startMs) {
        if (startMs == null) return@LaunchedEffect
        while (true) {
            elapsed = (System.currentTimeMillis() - startMs) / 1000
            kotlinx.coroutines.delay(1000)
        }
    }

    // ── Handle call end ───────────────────────────────────────────────────────
    LaunchedEffect(callState) {
        if (callState is ActiveCallState.Ended) {
            val ended = callState as ActiveCallState.Ended
            endedContact      = ended.contact
            endedDurationSecs = ended.durationSecs
            endedTimestampMs  = System.currentTimeMillis()
            showNoteSheet     = true
        }
    }

    fun formatElapsed(s: Long) = "%02d:%02d".format(s / 60, s % 60)

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush  = Brush.radialGradient(
                        colors = listOf(tagColor.copy(alpha = 0.10f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.28f),
                        radius = size.width * 1.1f
                    )
                )
            }
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            // Header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text(
                    text = when (callState) {
                        is ActiveCallState.Calling   -> "CALLING"
                        is ActiveCallState.Connected -> "ACTIVE CALL"
                        is ActiveCallState.OnHold    -> "ON HOLD"
                        is ActiveCallState.Ended     -> "CALL ENDED"
                        is ActiveCallState.Idle      -> "CONNECTING…"
                        else                         -> ""
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                if (contact != null) TagChip(tag = tag)
            }

            Spacer(Modifier.weight(1f))

            // Avatar
            SmartPulsingAvatar(
                contact = contact,
                size    = 108.dp,
                active  = callState is ActiveCallState.Connected
            )

            Spacer(Modifier.height(24.dp))

            // Name / number
            Text(
                text       = contact?.name ?: displayNumber,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            if (contact != null) {
                Text(displayNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
            }

            // Status / timer
            AnimatedContent(targetState = callState, label = "call_status") { state ->
                when (state) {
                    is ActiveCallState.Idle ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CallingDots()
                            Text("Connecting", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    is ActiveCallState.Calling ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            CallingDots()
                            Text("Calling", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    is ActiveCallState.Connected ->
                        Text(formatElapsed(elapsed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = tagColor, letterSpacing = 1.sp)
                    is ActiveCallState.OnHold ->
                        Text("On Hold", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = GradientStart)
                    is ActiveCallState.Ended ->
                        Text("Call Ended", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                    else -> {}
                }
            }

            Spacer(Modifier.weight(1.5f))

            // In-call keypad
            AnimatedVisibility(
                visible = showPad,
                enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { 20 },
                exit  = fadeOut(tween(150)) + slideOutVertically(tween(150)) { 20 }
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                        Text("Keypad", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { showPad = false }) {
                            Text("Close", color = GradientStart, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    listOf("123", "456", "789", "*0#").forEach { row ->
                        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp)) {
                            row.forEach { d ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f).height(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            CallManager.sendDtmf(d)
                                            CallManager.stopDtmf()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(d.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Call control grid
            AnimatedVisibility(visible = !showPad, enter = fadeIn(tween(200)), exit = fadeOut(tween(150))) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        CallControlButton(
                            icon   = if (muted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label  = if (muted) "Unmute" else "Mute",
                            active = muted, modifier = Modifier.weight(1f)
                        ) { muted = !muted; CallManager.mute(muted) }

                        CallControlButton(Icons.Rounded.Dialpad, "Keypad", false, Modifier.weight(1f)) {
                            showPad = true
                        }

                        CallControlButton(
                            icon   = if (speaker) Icons.Rounded.VolumeUp else Icons.Rounded.VolumeDown,
                            label  = if (speaker) "Earpiece" else "Speaker",
                            active = speaker, modifier = Modifier.weight(1f)
                        ) { speaker = !speaker }
                    }

                    Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(12.dp)) {
                        val onHold = callState is ActiveCallState.OnHold
                        CallControlButton(
                            Icons.Rounded.PauseCircle,
                            if (onHold) "Resume" else "Hold",
                            onHold,
                            Modifier.weight(1f)
                        ) { if (onHold) CallManager.unhold() else CallManager.hold() }

                        CallControlButton(Icons.Rounded.NoteAdd, "Note", false, Modifier.weight(1f)) {
                            showNoteSheet = true
                        }

                        CallControlButton(Icons.Rounded.Add, "Add call", false, Modifier.weight(1f)) {}
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            EndCallButton(onClick = { CallManager.endCall() })

            Spacer(Modifier.height(32.dp))
        }

        // After-call note sheet
        AnimatedVisibility(
            visible  = showNoteSheet,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter    = slideInVertically(tween(350)) { it } + fadeIn(tween(350)),
            exit     = slideOutVertically(tween(280)) { it } + fadeOut(tween(280)),
        ) {
            AfterCallNoteSheet(
                contact          = endedContact ?: contact,
                callLogId        = System.currentTimeMillis(),
                callDurationSecs = endedDurationSecs.takeIf { it > 0 } ?: elapsed,
                callType         = "OUTGOING",
                callTimestampMs  = endedTimestampMs,
                number           = displayNumber,
                onDismiss        = {
                    showNoteSheet = false
                    CallManager.reset()
                    onEndCall()
                }
            )
        }
    }
}

// ── Reusable composables ──────────────────────────────────────────────────────

@Composable
private fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (active) GradientStart.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant)
            .border(
                0.5.dp,
                if (active) GradientStart.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, label, tint = if (active) GradientStart else MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) GradientStart else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CallingDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dotCount by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 3f, label = "count",
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart)
    )
    Text(
        ".".repeat(dotCount.toInt() + 1),
        style = MaterialTheme.typography.bodyMedium,
        color = GradientStart,
        modifier = Modifier.width(24.dp)
    )
}
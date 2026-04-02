package com.linea.dialer.ui.screens

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linea.dialer.ui.components.TagChip
import com.linea.dialer.ui.viewmodel.DialViewModel
import com.linea.dialer.ui.viewmodel.SimSlot
import com.linea.dialer.ui.theme.*

private data class DialKey(val digit: String, val letters: String)

private val dialKeys = listOf(
    DialKey("1", ""),     DialKey("2", "ABC"), DialKey("3", "DEF"),
    DialKey("4", "GHI"),  DialKey("5", "JKL"), DialKey("6", "MNO"),
    DialKey("7", "PQRS"), DialKey("8", "TUV"), DialKey("9", "WXYZ"),
    DialKey("*", ""),     DialKey("0", "+"),   DialKey("#", ""),
)

@Composable
fun DialpadScreen(
    onCall: (String) -> Unit,
    vm: DialViewModel = viewModel(),
) {
    val context       = LocalContext.current
    val input         by vm.input.collectAsState()
    val matched       by vm.matchedContact.collectAsState()
    val error         by vm.dialError.collectAsState()
    val showSimPicker by vm.showSimPicker.collectAsState()
    val availableSims by vm.availableSims.collectAsState()
    val haptic         = LocalHapticFeedback.current

    // Check if Linea is the default dialer (recomputed on each recompose after returning
    // from the system settings screen, so the banner hides automatically)
    val isDefaultDialer = remember(context) {
        val tm = context.getSystemService(TelecomManager::class.java)
        tm?.defaultDialerPackage == context.packageName
    }

    val defaultDialerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { /* recompose will re-evaluate isDefaultDialer */ }

    fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = context.getSystemService(RoleManager::class.java)
            if (rm.isRoleAvailable(RoleManager.ROLE_DIALER) && !rm.isRoleHeld(RoleManager.ROLE_DIALER)) {
                defaultDialerLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
            }
        } else {
            defaultDialerLauncher.launch(
                Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
                    .putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .systemBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(20.dp))

            // Title row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "linea.",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                IconButton(onClick = {}) {
                    Icon(Icons.Rounded.MoreVert, "More", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Default dialer banner — only visible when not set as default
            if (!isDefaultDialer) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(GradientStart.copy(alpha = 0.10f))
                        .border(0.5.dp, GradientStart.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { requestDefaultDialer() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.PhoneAndroid, null, tint = GradientStart, modifier = Modifier.size(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Set Linea as default dialer",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            "Required to place calls within this app",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Set →", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = GradientStart)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Number display
            Box(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = input,
                    transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) },
                    label = "dial_display"
                ) { num ->
                    if (num.isEmpty()) {
                        Text(
                            "Enter number",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            vm.formatDisplay(num),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            textAlign = TextAlign.Center,
                            letterSpacing = 2.sp
                        )
                    }
                }
            }

            // Matched contact suggestion
            AnimatedVisibility(
                visible = matched != null,
                enter   = fadeIn(tween(200)) + slideInVertically(tween(200)) { -8 },
                exit    = fadeOut(tween(150))
            ) {
                matched?.let { c ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = GradientStart, modifier = Modifier.size(14.dp))
                        Text(c.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                        Text("·", color = MaterialTheme.colorScheme.outline)
                        TagChip(tag = c.tag, small = true)
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }

            // Error banner
            AnimatedVisibility(visible = error != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Text(
                        error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = vm::clearError, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(14.dp))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Dial pad grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                dialKeys.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { key ->
                            DialKeyButton(
                                key     = key,
                                modifier = Modifier.weight(1f),
                                onPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    vm.onDigit(key.digit)
                                },
                                onLongPress = {
                                    if (key.digit == "0") {
                                        vm.onBackspace()
                                        vm.onDigit("+")
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Action row: call button + backspace
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(Modifier.size(56.dp))

                // Gradient call FAB
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .then(
                            if (input.isNotEmpty())
                                Modifier.background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                            else
                                Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        .clickable(enabled = input.isNotEmpty()) {
                            // dial() returns the number immediately if call was placed,
                            // or null if it opened the SIM picker (onSimSelected handles nav then).
                            // We navigate RIGHT AWAY — ActiveCallScreen observes CallManager state
                            // reactively, so it will update as soon as InCallService fires.
                            val number = vm.dial()
                            if (number != null) onCall(number)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Call,
                        "Call",
                        tint = if (input.isNotEmpty()) Color.White else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(30.dp)
                    )
                }

                if (input.isNotEmpty()) {
                    IconButton(onClick = vm::onBackspace, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.Backspace, "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.size(56.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }

        // In-app SIM picker sheet
        if (showSimPicker) {
            SimPickerSheet(
                sims      = availableSims,
                onSelect  = { sim ->
                    val number = vm.onSimSelected(sim)
                    if (number != null) onCall(number)
                },
                onDismiss = { vm.dismissSimPicker() }
            )
        }
    }
}

// ── SIM Picker sheet ─────────────────────────────────────────────────────────

@Composable
private fun SimPickerSheet(
    sims: List<SimSlot>,
    onSelect: (SimSlot) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { /* consume touch */ }
                .padding(24.dp)
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    .align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(16.dp))
            Text("Choose SIM", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text("Select which SIM to place this call", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))

            sims.forEach { sim ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.15f), RoundedCornerShape(14.dp))
                        .clickable { onSelect(sim) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${sim.slotIndex + 1}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sim.displayName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                        Text("SIM ${sim.slotIndex + 1}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Rounded.Call, null, tint = GradientStart, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Dial key button ──────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialKeyButton(
    key: DialKey,
    modifier: Modifier,
    onPress: () -> Unit,
    onLongPress: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, tween(80), label = "key_scale")

    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (pressed) MaterialTheme.colorScheme.surfaceVariant
                else MaterialTheme.colorScheme.surface
            )
            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.18f), RoundedCornerShape(16.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onPress,
                onLongClick       = onLongPress
            )
            .drawBehind {
                if (pressed) {
                    drawCircle(
                        brush  = Brush.radialGradient(
                            listOf(GradientStart.copy(0.08f), Color.Transparent),
                            center = center,
                            radius = size.minDimension * 0.8f
                        )
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                key.digit,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (key.letters.isNotEmpty()) {
                Text(
                    key.letters,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}
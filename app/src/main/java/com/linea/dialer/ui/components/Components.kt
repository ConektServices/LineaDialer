package com.linea.dialer.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.linea.dialer.data.model.CallType
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.ui.theme.*

// ── Gradient brush ───────────────────────────────────────────────────────────
val lineaGradient: Brush
    get() = Brush.linearGradient(colors = listOf(GradientStart, GradientEnd))

fun Modifier.lineaGradientBackground(shape: Shape = RectangleShape) = this
    .clip(shape)
    .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))

// ── Gradient Button ──────────────────────────────────────────────────────────
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(shape)
            .then(
                if (enabled)
                    Modifier.background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                else
                    Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            leadingIcon?.invoke()
            Text(
                text      = text,
                style     = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color     = if (enabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 0.5.sp
            )
        }
    }
}

// ── Gradient FAB ────────────────────────────────────────────────────────────
@Composable
fun GradientFab(
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { icon() }
}

// ── End Call Button (red pulsing) ────────────────────────────────────────────
@Composable
fun EndCallButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0f, label = "pulseAlpha",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart)
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.5f, label = "pulseScale",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart)
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Pulse ring
        Box(modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = pulseScale; scaleY = pulseScale; alpha = pulseAlpha }
            .clip(CircleShape)
            .background(BrandRed.copy(alpha = 0.4f))
        )
        // Button
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(BrandRed)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End call",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Contact Avatar ───────────────────────────────────────────────────────────
@Composable
fun ContactAvatar(
    initials: String,
    tag: ContactTag,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val color = tag.color()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text      = initials.take(2).uppercase(),
            style     = MaterialTheme.typography.labelMedium.copy(
                fontSize   = (size.value * 0.3).sp,
                fontWeight = FontWeight.SemiBold
            ),
            color     = color,
            textAlign = TextAlign.Center
        )
    }
}

// Active call variant — pulsing avatar
@Composable
fun PulsingAvatar(
    initials: String,
    tag: ContactTag,
    size: Dp = 96.dp,
    active: Boolean = true,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = if (active) 1.08f else 1f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse)
    )
    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f, label = "ringAlpha",
        animationSpec = infiniteRepeatable(tween(1400, easing = EaseOut), RepeatMode.Restart)
    )
    val color = tag.color()
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.5f)) {
        Box(modifier = Modifier
            .size(size * 1.4f)
            .graphicsLayer { alpha = ringAlpha }
            .clip(CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.5f), CircleShape)
        )
        Box(modifier = Modifier
            .size(size * 1.15f)
            .graphicsLayer { alpha = ringAlpha * 0.6f }
            .clip(CircleShape)
            .border(1.dp, color.copy(alpha = 0.3f), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(1.5.dp, color.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = initials.take(2).uppercase(),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = color
            )
        }
    }
}

// ── Tag Chip ────────────────────────────────────────────────────────────────
@Composable
fun TagChip(tag: ContactTag, modifier: Modifier = Modifier, small: Boolean = false) {
    val color = tag.color()
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = if (small) 7.dp else 10.dp, vertical = if (small) 3.dp else 5.dp)
    ) {
        Text(
            text  = tag.label,
            style = if (small) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

// ── Call type indicator ──────────────────────────────────────────────────────
@Composable
fun CallTypeIndicator(type: CallType, modifier: Modifier = Modifier) {
    val (color, arrow) = when (type) {
        CallType.INCOMING -> Pair(CallIncoming,  "↙")
        CallType.OUTGOING -> Pair(CallOutgoing,  "↗")
        CallType.MISSED   -> Pair(CallMissed,    "↙")
    }
    Text(
        text     = arrow,
        color    = color,
        style    = MaterialTheme.typography.labelMedium,
        modifier = modifier
    )
}

// ── Section header ───────────────────────────────────────────────────────────
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text     = text.uppercase(),
        style    = MaterialTheme.typography.labelSmall,
        color    = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        letterSpacing = 1.2.sp
    )
}

// ── Gradient divider ─────────────────────────────────────────────────────────
@Composable
fun GradientDivider(modifier: Modifier = Modifier) {
    Box(modifier = modifier
        .fillMaxWidth()
        .height(1.dp)
        .background(Brush.horizontalGradient(listOf(
            GradientStart.copy(alpha = 0.6f),
            GradientEnd.copy(alpha = 0.6f)
        )))
    )
}

// Extension: tag color
fun ContactTag.color(): Color = when (this) {
    ContactTag.CLIENT   -> TagClient
    ContactTag.LEAD     -> TagLead
    ContactTag.PARTNER  -> TagPartner
    ContactTag.SUPPLIER -> TagSupplier
    ContactTag.PERSONAL -> TagPersonal
    ContactTag.UNKNOWN  -> TagUnknown
}

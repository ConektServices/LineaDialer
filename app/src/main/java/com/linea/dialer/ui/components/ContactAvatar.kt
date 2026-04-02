package com.linea.dialer.ui.components

import android.graphics.Color.alpha
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.graphicsLayer
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.data.model.RealContact

/**
 * Smart avatar that shows the Android contact photo when available,
 * falling back to coloured initials if no photo exists.
 *
 * Replaces the old text-only [ContactAvatar] in Components.kt.
 */
@Composable
fun SmartAvatar(
    contact: RealContact,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    SmartAvatar(
        photoUri = contact.photoUri,
        initials = contact.initials,
        tag      = contact.tag,
        size     = size,
        modifier = modifier,
    )
}

@Composable
fun SmartAvatar(
    photoUri: Uri?,
    initials: String,
    tag: ContactTag,
    size: Dp = 48.dp,
    modifier: Modifier = Modifier,
) {
    val color = tag.color()

    if (photoUri != null) {
        SubcomposeAsyncImage(
            model   = photoUri,
            contentDescription = initials,
            modifier = modifier
                .size(size)
                .clip(CircleShape)
                .border(1.dp, color.copy(alpha = 0.25f), CircleShape),
            contentScale = ContentScale.Crop,
            loading = {
                // Show initials while loading
                InitialsAvatar(initials, color, size)
            },
            error = {
                // Fall back to initials on load failure
                InitialsAvatar(initials, color, size)
            },
            success = {
                SubcomposeAsyncImageContent()
            }
        )
    } else {
        InitialsAvatar(initials, color, size, modifier)
    }
}

@Composable
private fun InitialsAvatar(
    initials: String,
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
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
                fontSize   = (size.value * 0.30f).sp,
                fontWeight = FontWeight.SemiBold
            ),
            color     = color,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * Large pulsing avatar for the active call screen — also photo-aware.
 */
@Composable
fun SmartPulsingAvatar(
    contact: RealContact?,
    size: Dp = 96.dp,
    active: Boolean = true,
) {
    if (contact?.photoUri != null) {
        val color = contact.tag.color()
        val infiniteTransition = rememberInfiniteTransition(label = "ring")
        val ringAlpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue  = 0f,
            label        = "a",
            animationSpec = infiniteRepeatable(
                animation  = tween(1400, easing = EaseOut),
                repeatMode = RepeatMode.Restart
            )
        )
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.5f)) {
            Box(
                modifier = Modifier
                    .size(size * 1.4f)
                    .graphicsLayer { alpha = if (active) ringAlpha else 0f }
                    .clip(CircleShape)
                    .border(1.5.dp, color.copy(alpha = 0.5f), CircleShape)
            )
            AsyncImage(
                model            = contact.photoUri,
                contentDescription = contact.name,
                modifier         = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(2.dp, color.copy(alpha = 0.4f), CircleShape),
                contentScale     = ContentScale.Crop,
            )
        }
    } else {
        PulsingAvatar(
            initials = contact?.initials ?: "?",
            tag      = contact?.tag ?: ContactTag.UNKNOWN,
            size     = size,
            active   = active,
        )
    }
}

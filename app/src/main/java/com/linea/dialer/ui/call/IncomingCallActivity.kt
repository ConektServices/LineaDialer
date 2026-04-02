package com.linea.dialer.ui.call

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.linea.dialer.data.model.*
import com.linea.dialer.telecom.CallManager
import com.linea.dialer.ui.components.*
import com.linea.dialer.ui.theme.*
import kotlinx.coroutines.delay

class IncomingCallActivity : ComponentActivity() {

    companion object {
        const val EXTRA_NUMBER      = "incoming_number"
        const val EXTRA_AUTO_ANSWER = "auto_answer"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show over lock screen — required flags
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val number     = intent.getStringExtra(EXTRA_NUMBER) ?: ""
        val autoAnswer = intent.getBooleanExtra(EXTRA_AUTO_ANSWER, false)

        setContent {
            LineaTheme {
                val callState by CallManager.callState.collectAsState()

                // Auto-dismiss when call is no longer ringing
                LaunchedEffect(callState) {
                    when (callState) {
                        is ActiveCallState.Connected -> {
                            // Call was answered — close this screen (go back to app)
                            delay(300)
                            finish()
                        }
                        is ActiveCallState.Ended,
                        is ActiveCallState.Idle -> {
                            // Call ended/missed
                            delay(800)
                            finish()
                        }
                        else -> {}
                    }
                }

                // Get contact from current call state
                val contact = (callState as? ActiveCallState.Calling)?.contact

                IncomingCallUi(
                    number    = number,
                    contact   = contact,
                    onAnswer  = {
                        CallManager.answer()
                        finish()
                    },
                    onDecline = {
                        CallManager.endCall()
                        finish()
                    }
                )
            }
        }

        // If launched by the Answer notification button, accept immediately
        if (autoAnswer) {
            CallManager.answer()
            finish()
        }
    }
}

@Composable
private fun IncomingCallUi(
    number: String,
    contact: RealContact?,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
) {
    val tag      = contact?.tag ?: ContactTag.UNKNOWN
    val tagColor = tag.color()

    // Double-ring pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val pulse1 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.25f, label = "p1",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart)
    )
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0f, label = "a1",
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseOut), RepeatMode.Restart)
    )
    val pulse2 by infiniteTransition.animateFloat(
        initialValue = 0.85f, targetValue = 1.25f, label = "p2",
        animationSpec = infiniteRepeatable(tween(1200, 400, easing = EaseOut), RepeatMode.Restart)
    )
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.45f, targetValue = 0f, label = "a2",
        animationSpec = infiniteRepeatable(tween(1200, 400, easing = EaseOut), RepeatMode.Restart)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(tagColor.copy(alpha = 0.12f), Color.Transparent),
                        center = Offset(size.width * 0.5f, size.height * 0.3f),
                        radius = size.width
                    )
                )
            }
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))

            // "Incoming Call" pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(GradientStart.copy(0.15f), GradientEnd.copy(0.08f))))
                    .padding(horizontal = 18.dp, vertical = 7.dp)
            ) {
                Text(
                    "INCOMING CALL",
                    style = MaterialTheme.typography.labelMedium,
                    color = GradientStart,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(Modifier.height(48.dp))

            // Pulsing avatar
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
                // Outer ring
                Box(modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer { scaleX = pulse2; scaleY = pulse2; alpha = alpha2 }
                    .clip(CircleShape)
                    .background(tagColor.copy(alpha = 0.15f))
                )
                // Inner ring
                Box(modifier = Modifier
                    .size(220.dp)
                    .graphicsLayer { scaleX = pulse1; scaleY = pulse1; alpha = alpha1 }
                    .clip(CircleShape)
                    .background(tagColor.copy(alpha = 0.2f))
                )
                // Avatar
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(tagColor.copy(alpha = 0.18f))
                        .border(2.dp, tagColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        contact?.initials ?: "?",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Name
            Text(
                text       = contact?.name ?: number,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.onBackground,
                textAlign  = TextAlign.Center
            )

            Spacer(Modifier.height(6.dp))

            if (contact != null) {
                Text(number, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                TagChip(tag = tag)
            }

            Spacer(Modifier.weight(1f))

            // Answer / Decline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Decline
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(BrandRed)
                            .clickable(onClick = onDecline),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CallEnd, "Decline", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text("Decline", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Answer
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(CallIncoming)
                            .clickable(onClick = onAnswer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Call, "Answer", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    Text("Answer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(56.dp))
        }
    }
}
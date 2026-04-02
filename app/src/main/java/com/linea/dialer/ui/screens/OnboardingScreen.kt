package com.linea.dialer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.linea.dialer.ui.components.GradientButton
import com.linea.dialer.ui.theme.*
import kotlinx.coroutines.launch

private data class Page(
    val emoji: String,
    val headline: String,
    val body: String,
    val accentStart: Color,
    val accentEnd: Color,
)

private val pages = listOf(
    Page("📞", "Your calls,\nbeautifully organised.",
        "Linea reimagines the default dialer with a clean, minimal interface built for speed and clarity.",
        GradientStart, GradientEnd),
    Page("🏷️", "Tag every call\nwith context.",
        "Mark contacts as Clients, Leads, Partners or Suppliers. Instantly see who matters most.",
        Color(0xFF34D399), Color(0xFF059669)),
    Page("📝", "Notes after\nevery conversation.",
        "Add quick notes to any contact right after a call. Never forget a commitment or follow-up again.",
        Color(0xFF60A5FA), Color(0xFF3B82F6)),
    Page("⚡", "Fast, smooth,\nalways ready.",
        "Set Linea as your default dialer and experience a dialer that gets out of your way.",
        GradientStart, GradientEnd),
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Subtle radial glow behind pager
        Box(modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors  = listOf(GradientStart.copy(alpha = 0.08f), Color.Transparent),
                        center  = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius  = size.width * 0.9f
                    )
                )
            }
        )

        Column(
            modifier = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // App wordmark
            Text(
                text   = "linea.",
                style  = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.ExtraBold,
                color  = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.align(Alignment.Start).padding(horizontal = 28.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Pager
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 24.dp),
                pageSpacing    = 12.dp,
            ) { index ->
                OnboardingCard(page = pages[index], pagerState = pagerState, pageIndex = index)
            }

            Spacer(Modifier.height(28.dp))

            // Page indicators
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                repeat(pages.size) { i ->
                    val selected = pagerState.currentPage == i
                    val width by animateDpAsState(if (selected) 24.dp else 6.dp, tween(250), label = "dot")
                    Box(modifier = Modifier
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(
                            if (selected) Brush.horizontalGradient(listOf(GradientStart, GradientEnd))
                            else Brush.horizontalGradient(listOf(
                                MaterialTheme.colorScheme.outline,
                                MaterialTheme.colorScheme.outline
                            ))
                        )
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // CTA button
            val isLast = pagerState.currentPage == pages.lastIndex
            GradientButton(
                text    = if (isLast) "Get Started" else "Next",
                onClick = {
                    if (isLast) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
            )

            // Skip
            AnimatedVisibility(visible = !isLast) {
                TextButton(onClick = onFinish, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        text  = "Skip",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingCard(page: Page, pagerState: PagerState, pageIndex: Int) {
    val pageOffset = (pagerState.currentPage - pageIndex) + pagerState.currentPageOffsetFraction
    val scale by animateFloatAsState(
        targetValue = if (pageOffset == 0f) 1f else 0.93f,
        animationSpec = tween(300), label = "scale"
    )

    Box(
        modifier = Modifier
            .fillMaxHeight(0.78f)
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                Brush.linearGradient(listOf(page.accentStart.copy(0.35f), page.accentEnd.copy(0.15f))),
                RoundedCornerShape(32.dp)
            )
            .drawBehind {
                // Gradient glow top corner
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(page.accentStart.copy(0.12f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f),
                        radius = size.width * 0.7f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Emoji in gradient circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(page.accentStart.copy(0.15f), page.accentEnd.copy(0.08f))))
                    .border(1.dp, Brush.linearGradient(listOf(page.accentStart.copy(0.4f), page.accentEnd.copy(0.2f))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.emoji, fontSize = 40.sp)
            }

            Spacer(Modifier.height(32.dp))

            // Gradient headline
            Text(
                text       = page.headline,
                style      = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 32.sp
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text      = page.body,
                style     = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp
            )
        }
    }
}

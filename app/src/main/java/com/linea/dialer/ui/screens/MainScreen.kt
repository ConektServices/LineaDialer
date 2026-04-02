package com.linea.dialer.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.linea.dialer.ui.theme.*

private enum class Tab(
    val label: String,
    val icon: ImageVector,
) {
    RECENTS("Recents",  Icons.Rounded.History),
    DIALPAD("Dial",     Icons.Rounded.Dialpad),
    CONTACTS("Contacts", Icons.Rounded.PermContactCalendar),
}

@Composable
fun MainScreen(
    onCallNumber: (String) -> Unit,
    onViewContact: (Long) -> Unit,
) {
    var activeTab by remember { mutableStateOf(Tab.DIALPAD) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            LineaBottomBar(active = activeTab, onSelect = { activeTab = it })
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    slideInHorizontally(tween(250)) { it * dir / 5 } + fadeIn(tween(250)) togetherWith
                    slideOutHorizontally(tween(220)) { -it * dir / 5 } + fadeOut(tween(220))
                },
                label = "tab_content"
            ) { tab ->
                when (tab) {
                    Tab.RECENTS  -> RecentsScreen(
                        onCallNumber  = onCallNumber,
                        onViewContact = onViewContact
                    )
                    Tab.DIALPAD  -> DialpadScreen(onCall = onCallNumber)
                    Tab.CONTACTS -> ContactsScreen(
                        onViewContact = onViewContact,
                        onCallNumber  = onCallNumber
                    )
                }
            }
        }
    }
}

@Composable
private fun LineaBottomBar(active: Tab, onSelect: (Tab) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Divider(
            color     = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            thickness = 0.5.dp,
            modifier  = Modifier.align(Alignment.TopCenter)
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(72.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Tab.entries.forEach { tab ->
                val selected  = tab == active
                val isDialpad = tab == Tab.DIALPAD

                if (isDialpad) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                            .clickable { onSelect(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tab.icon, tab.label, tint = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(tab) }
                            .padding(horizontal = 22.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            tab.icon, tab.label,
                            tint     = if (selected) GradientStart else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) GradientStart else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f)
                        )
                    }
                }
            }
        }
    }
}

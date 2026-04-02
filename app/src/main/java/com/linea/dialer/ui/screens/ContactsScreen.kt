package com.linea.dialer.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linea.dialer.data.model.*
import com.linea.dialer.ui.components.*
import com.linea.dialer.ui.viewmodel.ContactsViewModel
import com.linea.dialer.ui.theme.*

@Composable
fun ContactsScreen(
    onViewContact: (Long) -> Unit,
    onCallNumber: (String) -> Unit,
    vm: ContactsViewModel = viewModel(),
) {
    val uiState   by vm.uiState.collectAsState()
    val search    by vm.search.collectAsState()
    val tagFilter by vm.tagFilter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
    ) {
        Spacer(Modifier.height(16.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contacts", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
            IconButton(onClick = { vm.loadContacts() }) {
                Icon(Icons.Rounded.Refresh, "Refresh", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Search
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.18f), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Search, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            TextField(
                value = search, onValueChange = vm::onSearch,
                placeholder = { Text("Search contacts...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor   = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                    focusedIndicatorColor   = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                ),
                modifier = Modifier.weight(1f), singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Tag filter chips
        LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                TagFilterChip(label = "All", selected = tagFilter == null, onClick = { vm.onTagFilter(null) })
            }
            items(ContactTag.all()) { tag ->
                TagFilterChip(label = tag.label, selected = tagFilter == tag, onClick = { vm.onTagFilter(if (tagFilter == tag) null else tag) })
            }
        }

        Spacer(Modifier.height(4.dp))

        // Count strip
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("${uiState.contacts.size} contacts")
            if (tagFilter != null) StatPill(tagFilter!!.label, highlight = true)
        }

        // Content
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(32.dp), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Couldn't load contacts", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { vm.loadContacts() }) { Text("Retry", color = GradientStart) }
                }
            }
            uiState.contacts.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(if (search.isNotEmpty()) "No contacts match \"$search\"" else "No contacts found",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
            }
            else -> LazyColumn(contentPadding = PaddingValues(bottom = 12.dp), modifier = Modifier.fillMaxSize()) {
                items(uiState.contacts, key = { it.id }) { contact ->
                    RealContactRow(
                        contact = contact,
                        onView  = { onViewContact(contact.id) },
                        onCall  = { onCallNumber(contact.primaryNumber) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TagFilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .then(
                if (selected) Modifier.background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                else Modifier.background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f), RoundedCornerShape(24.dp))
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) androidx.compose.ui.graphics.Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StatPill(text: String, highlight: Boolean = false) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (highlight) GradientStart.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = if (highlight) GradientStart else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RealContactRow(contact: RealContact, onView: () -> Unit, onCall: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ContactAvatar(initials = contact.initials, tag = contact.tag, size = 50.dp)

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(contact.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                TagChip(tag = contact.tag, small = true)
            }
            Text(contact.primaryNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        IconButton(onClick = onCall, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Rounded.Call, "Call", tint = GradientStart, modifier = Modifier.size(18.dp))
        }
    }
}

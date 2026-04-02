package com.linea.dialer.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.linea.dialer.data.db.NoteEntity
import com.linea.dialer.data.model.*
import com.linea.dialer.data.repository.formatCallDate
import com.linea.dialer.ui.components.*
import com.linea.dialer.ui.viewmodel.ContactDetailViewModel
import com.linea.dialer.ui.theme.*
import java.util.*

@Composable
fun ContactDetailScreen(
    onBack: () -> Unit,
    onCall: (String) -> Unit,
    vm: ContactDetailViewModel = viewModel(),
) {
    val state           by vm.uiState.collectAsState()
    val contact          = state.contact
    var newNoteText     by remember { mutableStateOf("") }
    var showNoteInput   by remember { mutableStateOf(false) }
    var showTagMenu     by remember { mutableStateOf(false) }
    var editingNote     by remember { mutableStateOf<NoteEntity?>(null) }
    var showReminder    by remember { mutableStateOf(false) }

    if (state.isLoading || contact == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator(color = GradientStart, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
        }
        return
    }

    if (showReminder) {
        ReminderDialog(contact.name, onSchedule = { msg, ms -> vm.scheduleReminder(msg, ms); showReminder = false }, onDismiss = { showReminder = false })
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Rounded.ArrowBackIosNew, "Back", tint = MaterialTheme.colorScheme.onSurface) }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { showReminder = true }) { Icon(Icons.Rounded.NotificationAdd, "Reminder", tint = GradientStart) }
            Box {
                IconButton(onClick = { showTagMenu = true }) { Icon(Icons.Rounded.Label, "Tag", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                DropdownMenu(expanded = showTagMenu, onDismissRequest = { showTagMenu = false }) {
                    ContactTag.entries.forEach { tag ->
                        DropdownMenuItem(
                            text = { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                TagChip(tag = tag, small = true)
                                if (tag == contact.tag) Icon(Icons.Rounded.Check, null, tint = GradientStart, modifier = Modifier.size(14.dp))
                            }},
                            onClick = { vm.saveTag(tag); showTagMenu = false }
                        )
                    }
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {

            // Profile
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SmartAvatar(contact = contact, size = 72.dp)
                Column {
                    Text(contact.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        TagChip(tag = contact.tag)
                        if (state.notes.isNotEmpty()) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GradientStart.copy(0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                                Text("${state.notes.size} notes", style = MaterialTheme.typography.labelSmall, color = GradientStart)
                            }
                        }
                    }
                    Text(contact.primaryNumber, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(18.dp))

            // Actions
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(Brush.linearGradient(listOf(GradientStart, GradientEnd))).clickable { onCall(contact.primaryNumber) }, contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Call, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Call", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
                Box(modifier = Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f), RoundedCornerShape(14.dp)).clickable {}, contentAlignment = Alignment.Center) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Message, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(16.dp))
                        Text("Message", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }

            if (contact.numbers.size > 1) {
                Spacer(Modifier.height(16.dp))
                GradientDivider()
                Spacer(Modifier.height(12.dp))
                Text("ALL NUMBERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                contact.numbers.forEach { pn ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column { Text(pn.number, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface); Text(pn.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { onCall(pn.number) }, modifier = Modifier.size(36.dp)) { Icon(Icons.Rounded.Call, "Call", tint = GradientStart, modifier = Modifier.size(16.dp)) }
                    }
                }
            }

            // Pending reminders
            val pending = state.reminders.filter { !it.isDone }
            if (pending.isNotEmpty()) {
                Spacer(Modifier.height(16.dp)); GradientDivider(); Spacer(Modifier.height(14.dp))
                Text("REMINDERS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                pending.forEach { r ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GradientStart.copy(0.07f)).border(0.5.dp, GradientStart.copy(0.2f), RoundedCornerShape(12.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) { Text(r.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface); Text(formatCallDate(r.scheduledAt), style = MaterialTheme.typography.labelSmall, color = GradientStart) }
                        IconButton(onClick = { vm.cancelReminder(r) }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.Close, "Cancel", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp)) }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }

            Spacer(Modifier.height(20.dp)); GradientDivider(); Spacer(Modifier.height(14.dp))

            // Notes header
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("NOTES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                TextButton(onClick = { showNoteInput = !showNoteInput; newNoteText = "" }) {
                    Text(if (showNoteInput) "Cancel" else "+ Add note", color = if (showNoteInput) MaterialTheme.colorScheme.outline else GradientStart, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(6.dp))

            // Add note input
            AnimatedVisibility(visible = showNoteInput, enter = expandVertically(tween(220)) + fadeIn(), exit = shrinkVertically(tween(180)) + fadeOut()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = newNoteText, onValueChange = { newNoteText = it }, placeholder = { Text("Write a note…") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientStart, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.2f)), maxLines = 6)
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (newNoteText.isNotBlank()) Brush.linearGradient(listOf(GradientStart, GradientEnd)) else Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.surfaceVariant))).clickable(enabled = newNoteText.isNotBlank()) { vm.addNote(newNoteText.trim()); newNoteText = ""; showNoteInput = false }.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text("Save Note", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (newNoteText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline)
                    }
                }
            }

            if (state.notes.isEmpty() && !showNoteInput) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surface).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.1f), RoundedCornerShape(14.dp)).padding(20.dp), contentAlignment = Alignment.Center) {
                    Text("No notes yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
            } else {
                state.notes.forEach { note ->
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.12f), RoundedCornerShape(14.dp))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (editingNote?.id == note.id) {
                                var editText by remember { mutableStateOf(note.body) }
                                OutlinedTextField(value = editText, onValueChange = { editText = it }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientStart, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.2f)), maxLines = 5)
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TextButton(onClick = { vm.updateNote(note.copy(body = editText)); editingNote = null }) { Text("Save", color = GradientStart, style = MaterialTheme.typography.labelMedium) }
                                    TextButton(onClick = { editingNote = null }) { Text("Cancel", color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.labelMedium) }
                                }
                            } else {
                                Text(note.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, lineHeight = 22.sp)
                                Spacer(Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(formatCallDate(note.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { editingNote = note }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Edit, "Edit", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp)) }
                                    IconButton(onClick = { vm.deleteNote(note.id) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.Delete, "Delete", tint = MaterialTheme.colorScheme.error.copy(0.7f), modifier = Modifier.size(13.dp)) }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp)); GradientDivider(); Spacer(Modifier.height(14.dp))

            // Call history
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Text("CALL HISTORY", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.2.sp)
                Text("${state.callHistory.size} calls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            Spacer(Modifier.height(10.dp))
            if (state.callHistory.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), Alignment.Center) { Text("No call history", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
            } else {
                state.callHistory.take(20).forEach { log ->
                    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surface).padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CallTypeIndicator(type = log.type)
                        Text(formatCallDate(log.dateMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
                        log.durationFormatted?.let { dur -> Text(dur, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ReminderDialog(contactName: String, onSchedule: (String, Long) -> Unit, onDismiss: () -> Unit) {
    val context  = LocalContext.current
    var message by remember { mutableStateOf("Follow up with $contactName") }
    var pickedMs by remember { mutableStateOf(System.currentTimeMillis() + 24 * 60 * 60 * 1000L) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = MaterialTheme.colorScheme.surface,
        shape            = RoundedCornerShape(24.dp),
        title = { Text("Set Reminder", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Message") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GradientStart, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.25f)), maxLines = 3)
                Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(0.2f), RoundedCornerShape(12.dp)).clickable {
                    val cal = Calendar.getInstance().also { it.timeInMillis = pickedMs }
                    DatePickerDialog(context, { _, y, m, d ->
                        cal.set(Calendar.YEAR, y); cal.set(Calendar.MONTH, m); cal.set(Calendar.DAY_OF_MONTH, d)
                        TimePickerDialog(context, { _, h, min -> cal.set(Calendar.HOUR_OF_DAY, h); cal.set(Calendar.MINUTE, min); pickedMs = cal.timeInMillis }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                }.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.Schedule, null, tint = GradientStart, modifier = Modifier.size(18.dp))
                    Text(formatCallDate(pickedMs), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        confirmButton = {
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(GradientStart, GradientEnd))).clickable(enabled = message.isNotBlank()) { onSchedule(message.trim(), pickedMs) }.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text("Schedule", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    )
}

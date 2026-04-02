package com.linea.dialer.ui.components

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.*
import com.linea.dialer.data.db.CallNoteEntity
import com.linea.dialer.ui.theme.GradientEnd
import com.linea.dialer.ui.theme.GradientStart
import com.linea.dialer.ui.theme.TagClient
import com.linea.dialer.ui.theme.TagLead
import com.linea.dialer.ui.theme.TagPartner
import com.linea.dialer.ui.theme.TagPersonal
import com.linea.dialer.ui.theme.TagSupplier
import com.linea.dialer.ui.theme.TagUnknown
import com.linea.dialer.data.model.ActiveCallState
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.data.model.RealContact
import com.linea.dialer.data.repository.NotesRepository
import com.linea.dialer.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


/**
 * Animated bottom sheet that appears for ~8 seconds after a call ends,
 * letting the user quickly jot a note about the conversation.
 *
 * Usage: Render inside [ActiveCallScreen] and pass the ended call state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AfterCallNoteSheet(
    contact: RealContact?,
    callLogId: Long,
    callDurationSecs: Long,
    callType: String,
    callTimestampMs: Long,
    number: String,
    onDismiss: () -> Unit,
) {
    val context         = LocalContext.current
    val scope           = rememberCoroutineScope()
    val keyboard        = LocalSoftwareKeyboardController.current
    val focusRequester  = remember { FocusRequester() }
    var noteText        by remember { mutableStateOf("") }
    var saved           by remember { mutableStateOf(false) }
    var tagPick         by remember { mutableStateOf(contact?.tag ?: ContactTag.UNKNOWN) }
    var showTagRow      by remember { mutableStateOf(false) }

    // Auto-dismiss timer — resets when user starts typing
    var secondsLeft by remember { mutableIntStateOf(10) }
    LaunchedEffect(noteText) {
        secondsLeft = 10
        while (secondsLeft > 0 && !saved) {
            delay(1000)
            secondsLeft--
        }
        if (!saved) onDismiss()
    }

    // Request focus so keyboard pops immediately
    LaunchedEffect(Unit) {
        delay(300)
        focusRequester.requestFocus()
    }

    fun save() {
        scope.launch {
            val body = noteText.trim()
            if (body.isNotEmpty() && contact != null) {
                val repo = NotesRepository.getInstance(context)
                // Save as a call note
                repo.saveCallNote(
                    CallNoteEntity(
                        callLogId         = callLogId,
                        contactLookupKey  = contact.lookupKey,
                        contactName       = contact.name,
                        number            = number,
                        note              = body,
                        callType          = callType,
                        callDurationSecs  = callDurationSecs,
                        callTimestampMs   = callTimestampMs,
                    )
                )
                // Also save to the main notes for easy reading on the contact detail screen
                repo.addNote(lookupKey = contact.lookupKey, body = body, callLogId = callLogId)

                // Update tag if changed
                if (tagPick != contact.tag) {
                    repo.saveTag(contact.lookupKey, tagPick)
                }
            }
            saved = true
            keyboard?.hide()
            delay(400)
            onDismiss()
        }
    }

    val tag      = contact?.tag ?: ContactTag.UNKNOWN
    val tagColor = tag.color()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(
                0.5.dp,
                Brush.linearGradient(listOf(GradientStart.copy(0.25f), GradientEnd.copy(0.15f))),
                RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {

            // Drag handle
            Box(modifier = Modifier.width(40.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline.copy(0.3f)).align(Alignment.CenterHorizontally))

            Spacer(Modifier.height(16.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Add a note",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (contact != null) {
                        Text(
                            contact.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Auto-dismiss countdown ring
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
                        CircularProgressIndicator(
                            progress = { secondsLeft / 10f },
                            modifier = Modifier.size(36.dp),
                            color    = GradientStart,
                            trackColor = MaterialTheme.colorScheme.outline.copy(0.15f),
                            strokeWidth = 2.5.dp,
                        )
                        Text(
                            "$secondsLeft",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, "Dismiss", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Note text field
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                placeholder = { Text("What was this call about?", style = MaterialTheme.typography.bodyMedium) },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = GradientStart,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(0.2f),
                ),
                maxLines = 4,
                minLines = 2,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction      = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { save() }),
            )

            Spacer(Modifier.height(12.dp))

            // Tag picker row (only shown when contact exists)
            if (contact != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Tag:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Current tag — click to expand picker
                    TagChip(tag = tagPick, small = true)
                    if (!showTagRow) {
                        TextButton(
                            onClick = { showTagRow = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelSmall, color = GradientStart)
                        }
                    }
                }
                AnimatedVisibility(visible = showTagRow) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ContactTag.entries.take(5).forEach { t ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (tagPick == t) t.color().copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .border(
                                        if (tagPick == t) 1.dp else 0.5.dp,
                                        if (tagPick == t) t.color().copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { tagPick = t; showTagRow = false }
                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                            ) {
                                Text(t.label, style = MaterialTheme.typography.labelSmall, color = if (tagPick == t) t.color() else MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // Save / Skip row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .let { m ->
                            if (noteText.isNotBlank())
                                m.background(Brush.linearGradient(listOf(GradientStart, GradientEnd)))
                            else
                                m.background(MaterialTheme.colorScheme.surfaceVariant)
                        }
                        .clickable(enabled = noteText.isNotBlank()) { save() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Check, null, tint = if (noteText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                        Text(
                            "Save Note",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (noteText.isNotBlank()) Color.White else MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

package com.linea.dialer.data.model

import android.net.Uri

/**
 * Represents a real Android contact, enriched with Linea metadata
 * (tag, notes) stored in Room (NotesRepository).
 */
data class RealContact(
    val id: Long,                        // ContactsContract row _ID
    val lookupKey: String,               // stable across syncs
    val name: String,
    val numbers: List<PhoneNumber>,      // may have multiple numbers
    val photoUri: Uri? = null,
    val tag: ContactTag = ContactTag.UNKNOWN,
    val notes: String = "",
) {
    val primaryNumber: String
        get() = numbers.firstOrNull()?.number ?: ""

    val initials: String
        get() = name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
            .ifEmpty { "?" }
}

data class PhoneNumber(
    val number: String,
    val normalised: String,              // digits only, for matching
    val type: Int,                       // ContactsContract type int
    val label: String,                   // "Mobile", "Work", custom label
)

/**
 * A real call log entry from CallLog.Calls, enriched with a matched RealContact.
 */
data class RealCallLog(
    val id: Long,
    val number: String,
    val normalised: String,
    val contact: RealContact?,           // null = unknown number
    val type: CallType,
    val dateMs: Long,                    // epoch millis
    val durationSecs: Long,             // 0 for missed
    val simLabel: String = "",           // carrier / SIM label
) {
    val displayName: String
        get() = contact?.name ?: number.ifEmpty { "Unknown" }

    val durationFormatted: String?
        get() = if (durationSecs <= 0L) null else {
            val m = durationSecs / 60
            val s = durationSecs % 60
            if (m > 0) "${m}m ${s}s" else "${s}s"
        }
}

/**
 * The state of the current active call — written by LineaInCallService,
 * read by ActiveCallScreen.
 */
sealed class ActiveCallState {
    object Idle : ActiveCallState()
    data class Calling(val contact: RealContact?, val number: String) : ActiveCallState()
    data class Connected(val contact: RealContact?, val number: String, val startMs: Long) : ActiveCallState()
    data class OnHold(val contact: RealContact?, val number: String) : ActiveCallState()
    data class Ended(val contact: RealContact?, val durationSecs: Long) : ActiveCallState()
}

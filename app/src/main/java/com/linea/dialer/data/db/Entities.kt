package com.linea.dialer.data.db

import androidx.room.*

// ── Contact metadata (tag + last-note summary) ───────────────────────────────
@Entity(tableName = "contact_meta")
data class ContactMetaEntity(
    @PrimaryKey val lookupKey: String,
    val tag: String = "UNKNOWN",            // ContactTag name
    val updatedAt: Long = System.currentTimeMillis(),
)

// ── Notes (one contact can have many notes over time) ────────────────────────
@Entity(
    tableName = "notes",
    foreignKeys = [ForeignKey(
        entity        = ContactMetaEntity::class,
        parentColumns = ["lookupKey"],
        childColumns  = ["contactLookupKey"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("contactLookupKey")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactLookupKey: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val callLogId: Long? = null,            // optional: linked to a specific call
)

// ── Follow-up reminders ───────────────────────────────────────────────────────
@Entity(
    tableName = "reminders",
    foreignKeys = [ForeignKey(
        entity        = ContactMetaEntity::class,
        parentColumns = ["lookupKey"],
        childColumns  = ["contactLookupKey"],
        onDelete      = ForeignKey.CASCADE,
    )],
    indices = [Index("contactLookupKey")]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactLookupKey: String,
    val contactName: String,               // denormalised for notification display
    val contactNumber: String,
    val message: String,
    val scheduledAt: Long,                 // epoch millis
    val isDone: Boolean = false,
    val workRequestId: String = "",        // UUID from WorkManager
    val createdAt: Long = System.currentTimeMillis(),
)

// ── Call notes (quick note attached right after a call ends) ─────────────────
@Entity(tableName = "call_notes")
data class CallNoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val callLogId: Long,                   // maps to CallLog.Calls._ID
    val contactLookupKey: String?,
    val contactName: String,
    val number: String,
    val note: String,
    val callType: String,                  // "INCOMING" | "OUTGOING" | "MISSED"
    val callDurationSecs: Long,
    val callTimestampMs: Long,
    val createdAt: Long = System.currentTimeMillis(),
)

package com.linea.dialer.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.linea.dialer.data.model.CallType
import com.linea.dialer.data.model.RealCallLog
import com.linea.dialer.data.model.RealContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class CallLogRepository(private val context: Context) {

    private val cr           = context.contentResolver
    private val contactsRepo = ContactsRepository.getInstance(context)

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) ==
                PackageManager.PERMISSION_GRANTED

    /**
     * Load the most recent [limit] call log entries.
     *
     * Performance strategy:
     *   Load ALL contacts once into a normalised-number suffix map, then do
     *   O(1) lookups per call log row. This replaces the previous approach
     *   where findByNumber() re-scanned the full contacts list for each row.
     *
     * NOTE: No "LIMIT x" in sortOrder — several OEM ContentProviders reject it.
     *   We stop reading after [limit] cursor rows instead.
     */
    suspend fun loadRecent(limit: Int = 150): List<RealCallLog> = withContext(Dispatchers.IO) {
        if (!hasPermission()) {
            throw SecurityException("READ_CALL_LOG permission not granted")
        }

        // Step 1: build contact lookup map — single scan of all contacts
        val contactMap = buildContactMap()

        // Step 2: query call log
        val logs = mutableListOf<RealCallLog>()

        val cursor: Cursor? = cr.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
            ),
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )

        cursor?.use {
            val idIdx   = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numIdx  = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val typeIdx = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateIdx = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val durIdx  = it.getColumnIndexOrThrow(CallLog.Calls.DURATION)

            var count = 0
            while (it.moveToNext() && count < limit) {
                val id       = it.getLong(idIdx)
                val number   = it.getString(numIdx) ?: ""
                val typeInt  = it.getInt(typeIdx)
                val dateMs   = it.getLong(dateIdx)
                val duration = it.getLong(durIdx)

                val callType = when (typeInt) {
                    CallLog.Calls.INCOMING_TYPE  -> CallType.INCOMING
                    CallLog.Calls.OUTGOING_TYPE  -> CallType.OUTGOING
                    CallLog.Calls.MISSED_TYPE,
                    CallLog.Calls.REJECTED_TYPE  -> CallType.MISSED
                    else                         -> CallType.MISSED
                }

                logs += RealCallLog(
                    id           = id,
                    number       = number,
                    normalised   = number.filter(Char::isDigit),
                    contact      = lookupContact(number, contactMap),
                    type         = callType,
                    dateMs       = dateMs,
                    durationSecs = duration,
                    simLabel     = "",
                )
                count++
            }
        }
        logs
    }

    suspend fun loadForContact(numbers: List<String>): List<RealCallLog> = withContext(Dispatchers.IO) {
        if (numbers.isEmpty() || !hasPermission()) return@withContext emptyList()
        loadRecent(500).filter { log ->
            numbers.any { n ->
                val nNorm = n.filter(Char::isDigit)
                log.normalised.endsWith(nNorm.takeLast(9)) ||
                        nNorm.endsWith(log.normalised.takeLast(9))
            }
        }
    }

    // ── Contact map helpers ───────────────────────────────────────────────────

    /**
     * Returns a map of last-9-digit suffix → RealContact.
     * This handles country-code variations like +254 vs 0 prefix.
     */
    private suspend fun buildContactMap(): Map<String, RealContact> {
        val contacts = runCatching { contactsRepo.loadAll() }.getOrDefault(emptyList())
        val map = HashMap<String, RealContact>(contacts.size * 2)
        for (contact in contacts) {
            for (phone in contact.numbers) {
                val norm = phone.normalised.takeLast(9)
                if (norm.isNotEmpty()) map[norm] = contact
            }
        }
        return map
    }

    private fun lookupContact(rawNumber: String, map: Map<String, RealContact>): RealContact? {
        val norm = rawNumber.filter(Char::isDigit)
        if (norm.length < 4) return null
        for (len in 9 downTo 7) {
            val found = map[norm.takeLast(len)]
            if (found != null) return found
        }
        return null
    }

    companion object {
        @Volatile private var instance: CallLogRepository? = null
        fun getInstance(context: Context): CallLogRepository =
            instance ?: synchronized(this) {
                instance ?: CallLogRepository(context.applicationContext).also { instance = it }
            }
    }
}

// ── Date formatting ──────────────────────────────────────────────────────────

fun formatCallDate(epochMs: Long): String {
    val now   = System.currentTimeMillis()
    val diff  = now - epochMs
    val cal   = Calendar.getInstance().also { it.timeInMillis = epochMs }
    val today = Calendar.getInstance()
    val fmt12 = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return when {
        diff < TimeUnit.HOURS.toMillis(24) &&
                cal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR) ->
            "Today, ${fmt12.format(Date(epochMs))}"
        diff < TimeUnit.HOURS.toMillis(48) ->
            "Yesterday, ${fmt12.format(Date(epochMs))}"
        diff < TimeUnit.DAYS.toMillis(7) ->
            SimpleDateFormat("EEE, hh:mm a", Locale.getDefault()).format(Date(epochMs))
        else ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMs))
    }
}
package com.linea.dialer.data.repository

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.provider.ContactsContract.Contacts
import com.linea.dialer.data.model.ContactTag
import com.linea.dialer.data.model.PhoneNumber
import com.linea.dialer.data.model.RealContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads real contacts from Android's ContactsContract.
 * Tags and notes are NOT read here — they come from NotesRepository (Room).
 * The ViewModel layer merges the two.
 */
class ContactsRepository(private val context: Context) {

    private val cr = context.contentResolver

    suspend fun loadAll(): List<RealContact> = withContext(Dispatchers.IO) {
        val numbersById = loadPhoneNumbers()
        val contacts    = mutableListOf<RealContact>()

        val cursor: Cursor? = cr.query(
            Contacts.CONTENT_URI,
            arrayOf(
                Contacts._ID,
                Contacts.LOOKUP_KEY,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
                Contacts.HAS_PHONE_NUMBER,
            ),
            "${Contacts.HAS_PHONE_NUMBER} = 1",
            null,
            "${Contacts.DISPLAY_NAME_PRIMARY} ASC"
        )

        cursor?.use {
            val idIdx    = it.getColumnIndexOrThrow(Contacts._ID)
            val lookIdx  = it.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)
            val nameIdx  = it.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY)
            val photoIdx = it.getColumnIndexOrThrow(Contacts.PHOTO_THUMBNAIL_URI)

            while (it.moveToNext()) {
                val id          = it.getLong(idIdx)
                val lookupKey   = it.getString(lookIdx) ?: continue
                val name        = it.getString(nameIdx)?.trim()?.ifEmpty { null } ?: continue
                val photoUri    = it.getString(photoIdx)?.let { s -> Uri.parse(s) }
                val numbers     = numbersById[id] ?: continue

                contacts += RealContact(
                    id        = id,
                    lookupKey = lookupKey,
                    name      = name,
                    numbers   = numbers,
                    photoUri  = photoUri,
                    // Tag defaults to UNKNOWN here; ViewModel enriches from Room
                    tag       = ContactTag.UNKNOWN,
                    notes     = "",
                )
            }
        }
        contacts
    }

    suspend fun loadById(contactId: Long): RealContact? = withContext(Dispatchers.IO) {
        val numbers = loadPhoneNumbersForContact(contactId)

        val cursor: Cursor? = cr.query(
            Contacts.CONTENT_URI,
            arrayOf(
                Contacts._ID,
                Contacts.LOOKUP_KEY,
                Contacts.DISPLAY_NAME_PRIMARY,
                Contacts.PHOTO_THUMBNAIL_URI,
            ),
            "${Contacts._ID} = ?",
            arrayOf(contactId.toString()),
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val lookupKey = it.getString(it.getColumnIndexOrThrow(Contacts.LOOKUP_KEY)) ?: return@withContext null
                val name      = it.getString(it.getColumnIndexOrThrow(Contacts.DISPLAY_NAME_PRIMARY))?.trim() ?: return@withContext null
                val photoUri  = it.getString(it.getColumnIndexOrThrow(Contacts.PHOTO_THUMBNAIL_URI))?.let { s -> Uri.parse(s) }
                return@withContext RealContact(
                    id        = contactId,
                    lookupKey = lookupKey,
                    name      = name,
                    numbers   = numbers,
                    photoUri  = photoUri,
                    tag       = ContactTag.UNKNOWN,
                    notes     = "",
                )
            }
        }
        null
    }

    suspend fun findByNumber(rawNumber: String): RealContact? = withContext(Dispatchers.IO) {
        val normalised = rawNumber.filter(Char::isDigit)
        if (normalised.length < 4) return@withContext null
        loadAll().firstOrNull { c ->
            c.numbers.any { pn ->
                pn.normalised.endsWith(normalised.takeLast(9)) ||
                normalised.endsWith(pn.normalised.takeLast(9))
            }
        }
    }

    suspend fun search(query: String): List<RealContact> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext loadAll()
        val q = query.trim().lowercase()
        loadAll().filter { c ->
            c.name.lowercase().contains(q) ||
            c.numbers.any { n -> n.number.contains(q) || n.normalised.contains(q) }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun loadPhoneNumbers(): Map<Long, List<PhoneNumber>> {
        val map = mutableMapOf<Long, MutableList<PhoneNumber>>()
        val cursor: Cursor? = cr.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.CONTACT_ID, Phone.NUMBER, Phone.NORMALIZED_NUMBER, Phone.TYPE, Phone.LABEL),
            null, null, null
        )
        cursor?.use {
            val cidIdx   = it.getColumnIndexOrThrow(Phone.CONTACT_ID)
            val numIdx   = it.getColumnIndexOrThrow(Phone.NUMBER)
            val normIdx  = it.getColumnIndex(Phone.NORMALIZED_NUMBER)
            val typeIdx  = it.getColumnIndexOrThrow(Phone.TYPE)
            val labelIdx = it.getColumnIndexOrThrow(Phone.LABEL)

            while (it.moveToNext()) {
                val cid    = it.getLong(cidIdx)
                val number = it.getString(numIdx) ?: continue
                val norm   = (if (normIdx >= 0) it.getString(normIdx) else null)
                    ?: number.filter(Char::isDigit)
                val type   = it.getInt(typeIdx)
                val label  = Phone.getTypeLabel(context.resources, type, it.getString(labelIdx)).toString()
                map.getOrPut(cid) { mutableListOf() }.add(PhoneNumber(number, norm, type, label))
            }
        }
        return map
    }

    private fun loadPhoneNumbersForContact(contactId: Long): List<PhoneNumber> {
        val list = mutableListOf<PhoneNumber>()
        val cursor: Cursor? = cr.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.NUMBER, Phone.NORMALIZED_NUMBER, Phone.TYPE, Phone.LABEL),
            "${Phone.CONTACT_ID} = ?",
            arrayOf(contactId.toString()),
            null
        )
        cursor?.use {
            val numIdx   = it.getColumnIndexOrThrow(Phone.NUMBER)
            val normIdx  = it.getColumnIndex(Phone.NORMALIZED_NUMBER)
            val typeIdx  = it.getColumnIndexOrThrow(Phone.TYPE)
            val labelIdx = it.getColumnIndexOrThrow(Phone.LABEL)
            while (it.moveToNext()) {
                val number = it.getString(numIdx) ?: continue
                val norm   = (if (normIdx >= 0) it.getString(normIdx) else null) ?: number.filter(Char::isDigit)
                val type   = it.getInt(typeIdx)
                val label  = Phone.getTypeLabel(context.resources, type, it.getString(labelIdx)).toString()
                list.add(PhoneNumber(number, norm, type, label))
            }
        }
        return list
    }

    companion object {
        @Volatile private var instance: ContactsRepository? = null
        fun getInstance(context: Context): ContactsRepository =
            instance ?: synchronized(this) {
                instance ?: ContactsRepository(context.applicationContext).also { instance = it }
            }
    }
}

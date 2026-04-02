package com.linea.dialer.data.local

import android.content.Context
import android.content.SharedPreferences
import com.linea.dialer.data.model.ContactTag
import org.json.JSONObject

/**
 * Lightweight persistence for Linea-specific metadata that Android's
 * ContactsContract doesn't store: contact tags and call notes.
 *
 * Storage format (SharedPreferences):
 *   Key   : "meta_<lookupKey>"
 *   Value : JSON  { "tag": "CLIENT", "notes": "..." }
 *
 * This is intentionally simple. Migrate to Room when the feature set grows.
 */
class MetadataStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Tag ──────────────────────────────────────────────────────────────────

    fun getTag(lookupKey: String): ContactTag {
        val json = prefs.getString(prefKey(lookupKey), null) ?: return ContactTag.UNKNOWN
        return runCatching {
            ContactTag.valueOf(JSONObject(json).optString("tag", ContactTag.UNKNOWN.name))
        }.getOrDefault(ContactTag.UNKNOWN)
    }

    fun setTag(lookupKey: String, tag: ContactTag) {
        val json = getOrCreate(lookupKey).put("tag", tag.name)
        prefs.edit().putString(prefKey(lookupKey), json.toString()).apply()
    }

    // ── Notes ────────────────────────────────────────────────────────────────

    fun getNotes(lookupKey: String): String {
        val json = prefs.getString(prefKey(lookupKey), null) ?: return ""
        return runCatching { JSONObject(json).optString("notes", "") }.getOrDefault("")
    }

    fun setNotes(lookupKey: String, notes: String) {
        val json = getOrCreate(lookupKey).put("notes", notes)
        prefs.edit().putString(prefKey(lookupKey), json.toString()).apply()
    }

    // ── Bulk read (used when loading all contacts) ───────────────────────────

    data class Metadata(val tag: ContactTag, val notes: String)

    fun getAll(lookupKeys: List<String>): Map<String, Metadata> {
        return lookupKeys.associateWith { key ->
            Metadata(tag = getTag(key), notes = getNotes(key))
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun getOrCreate(lookupKey: String): JSONObject {
        val existing = prefs.getString(prefKey(lookupKey), null)
        return if (existing != null) runCatching { JSONObject(existing) }.getOrElse { JSONObject() }
        else JSONObject()
    }

    private fun prefKey(lookupKey: String) = "meta_$lookupKey"

    companion object {
        private const val PREF_NAME = "linea_metadata"

        // Singleton
        @Volatile private var instance: MetadataStore? = null
        fun getInstance(context: Context): MetadataStore =
            instance ?: synchronized(this) {
                instance ?: MetadataStore(context).also { instance = it }
            }
    }
}

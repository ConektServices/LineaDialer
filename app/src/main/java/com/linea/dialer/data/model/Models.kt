package com.linea.dialer.data.model

import androidx.compose.runtime.Stable

// ── Enums ────────────────────────────────────────────────────────────────────

enum class CallType { INCOMING, OUTGOING, MISSED }

enum class ContactTag(val label: String) {
    CLIENT("Client"),
    LEAD("Lead"),
    PARTNER("Partner"),
    SUPPLIER("Supplier"),
    PERSONAL("Personal"),
    UNKNOWN("Unknown");

    companion object {
        fun all() = listOf(CLIENT, LEAD, PARTNER, SUPPLIER, PERSONAL)
    }
}

// ── Models ───────────────────────────────────────────────────────────────────

@Stable
data class Contact(
    val id: Int,
    val name: String,
    val number: String,
    val tag: ContactTag = ContactTag.UNKNOWN,
    val initials: String = name.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString(""),
    val totalCalls: Int = 0,
    val notes: String = "",
)

@Stable
data class CallLog(
    val id: Int,
    val contact: Contact?,
    val number: String,
    val type: CallType,
    val timestamp: String,
    val duration: String?,
)

data class OnboardingPage(
    val illustrationRes: Int,
    val headline: String,
    val body: String,
)

// ── Sample data ──────────────────────────────────────────────────────────────

val sampleContacts = listOf(
    Contact(1, "Amara Osei",     "+254 701 234 567", ContactTag.CLIENT,   "AO", 14, "Discussed Q1 contract renewal. Follow up on pricing."),
    Contact(2, "Kemi Adeyemi",   "+234 812 345 678", ContactTag.LEAD,     "KA", 3,  "Interested in enterprise plan. Send demo link."),
    Contact(3, "Fatou Diallo",   "+221 77 456 7890", ContactTag.PARTNER,  "FD", 8,  "Partnership agreement signed. Kick-off call next week."),
    Contact(4, "Seun Bankole",   "+234 703 567 890", ContactTag.SUPPLIER, "SB", 6,  "Confirmed March shipment. Check invoice #2031."),
    Contact(5, "Lena Müller",    "+49 151 234 5678", ContactTag.CLIENT,   "LM", 11, "Very satisfied with onboarding. Potential upsell in May."),
    Contact(6, "Tariq Hassan",   "+971 50 678 9012", ContactTag.LEAD,     "TH", 2,  ""),
    Contact(7, "Grace Mutua",    "+254 722 888 001", ContactTag.PERSONAL, "GM", 5,  ""),
    Contact(8, "James Kariuki",  "+254 733 444 002", ContactTag.CLIENT,   "JK", 9,  "Awaiting final sign-off on SLA document."),
)

val sampleCallLogs = listOf(
    CallLog(1, sampleContacts[0], sampleContacts[0].number, CallType.INCOMING, "Today, 09:14", "4m 23s"),
    CallLog(2, sampleContacts[1], sampleContacts[1].number, CallType.OUTGOING, "Today, 11:05", "1m 52s"),
    CallLog(3, null,              "+254 709 000 123",        CallType.MISSED,   "Today, 08:50", null),
    CallLog(4, sampleContacts[2], sampleContacts[2].number, CallType.OUTGOING, "Yesterday",    "6m 10s"),
    CallLog(5, sampleContacts[4], sampleContacts[4].number, CallType.INCOMING, "Yesterday",    "2m 47s"),
    CallLog(6, sampleContacts[3], sampleContacts[3].number, CallType.OUTGOING, "Mar 27",       "3m 05s"),
    CallLog(7, sampleContacts[6], sampleContacts[6].number, CallType.INCOMING, "Mar 26",       "0m 42s"),
    CallLog(8, sampleContacts[7], sampleContacts[7].number, CallType.OUTGOING, "Mar 25",       "5m 18s"),
)
